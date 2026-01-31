package com.jienoshiri.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jienoshiri.platform.dto.CommentVo;
import com.jienoshiri.platform.dto.PostVo;
import com.jienoshiri.platform.entity.Comment;
import com.jienoshiri.platform.entity.Post;
import com.jienoshiri.platform.entity.PostLike;
import com.jienoshiri.platform.entity.SysUser;
import com.jienoshiri.platform.mapper.CommentMapper;
import com.jienoshiri.platform.mapper.PostLikeMapper;
import com.jienoshiri.platform.mapper.PostMapper;
import com.jienoshiri.platform.mapper.UserMapper;
import com.jienoshiri.platform.utils.LocationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class PostService {

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PostLikeMapper postLikeMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private NotificationService notificationService;

    /**
     * 发布帖子
     */
    public void publishPost(Post post) {
        post.setCreateTime(LocalDateTime.now());
        post.setViewCount(0);
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setStatus(1); // 默认正常
        postMapper.insert(post);
        // ⭐ 新增：发帖奖励声望
        changeReputation(post.getUserId(), 5, "发布帖子");
    }

    /**
     * 获取帖子列表 (首页信息流)
     */
    public List<PostVo> getPostList(BigDecimal userLat, BigDecimal userLng, String keyword, Long currentUserId, String identityType) {

        // 1. 查所有帖子
        QueryWrapper<Post> query = new QueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            query.and(w -> w.like("title", keyword).or().like("content", keyword));
        }
        query.orderByDesc("create_time");
        List<Post> posts = postMapper.selectList(query);

        // 2. 准备推荐数据
        List<Long> userCfIds = new ArrayList<>(); // 协同过滤ID
        List<Long> contentBasedIds = new ArrayList<>(); // 基于内容ID

        if (currentUserId != null) {
            // A. 获取协同过滤推荐 (UserCF)
            try {
                userCfIds = recommendationService.recommendPostIds(currentUserId, 10);
            } catch (Exception e) {}

            // B. 获取基于内容推荐 (Content-Based)
            try {
                contentBasedIds = recommendationService.recommendByContent(identityType);
                System.out.println(">>> Content-Based 为身份 " + identityType + " 推荐了: " + contentBasedIds);
            } catch (Exception e) {}
        }

        List<PostVo> result = new ArrayList<>();

        // 3. 混合打分
        for (Post post : posts) {
            PostVo vo = new PostVo();
            BeanUtils.copyProperties(post, vo);

            // 填充作者信息
            SysUser author = userMapper.selectById(post.getUserId());
            if (author != null) {
                vo.setAuthorName(author.getNickname());
                vo.setAuthorAvatar(author.getAvatar());
                vo.setAuthorIdentity(author.getIdentityType());
                // ⭐ 新增：将作者的声望值传给前端 (需要在 PostVo 中添加字段)
                vo.setAuthorReputation(author.getReputation());
            }
            // 填充距离
            if (userLat != null && post.getLatitude() != null) {
                double km = LocationUtils.getDistance(userLat, userLng, post.getLatitude(), post.getLongitude());
                vo.setDistance(km);
            }
            // 填充点赞状态
            if (currentUserId != null) {
                Long count = postLikeMapper.selectCount(new QueryWrapper<PostLike>()
                        .eq("user_id", currentUserId).eq("post_id", post.getId()));
                vo.setIsLiked(count > 0);
            }

            // ⭐⭐ 混合加权核心逻辑 ⭐⭐
            double score = 0;

            // 权重 1: UserCF (最强推荐) -> +1000分
            if (userCfIds.contains(post.getId())) {
                score += 1000;
                vo.setTitle("【猜你喜欢】" + vo.getTitle());
            }

            // 权重 2: Content-Based (身份匹配) -> +500分
            if (contentBasedIds.contains(post.getId())) {
                score += 500;
                if (!vo.getTitle().startsWith("【猜你喜欢】")) {
                    vo.setTitle("【精选】" + vo.getTitle());
                }
            }

            // ⭐ 权重 3: 作者声望加权 (High Reputation Bonus)
            // 逻辑：每 1 点声望增加 0.5 分，最高加 200 分
            // 让“认证学长”或“高信誉中介”的帖子更容易被看到
            if (author != null && author.getReputation() != null) {
                double repBonus = author.getReputation() * 0.5;
                // 限制最大加分，防止刷分霸屏
                score += Math.min(Math.max(repBonus, -500), 200);
            }

            // 权重 4: LBS (离得近) -> +300分
            if (vo.getDistance() != null && vo.getDistance() < 10) {
                score += 300;
            }

            // 权重 5: Wiki 百科 -> +200分
            if (post.getStatus() == 3) {
                score += 200;
            }

            // 权重 6: 时间分
            score += (double) post.getId() / 1000000.0;

            vo.setScore(score);
            result.add(vo);
        }

        // 4. 排序
        result.sort((o1, o2) -> Double.compare(o2.getScore(), o1.getScore()));
        System.out.println(">>> 正在计算推荐分数...");
        return result;
    }

    public boolean toggleLike(Long postId, Long userId) {
        // 1. 检查是否点过赞
        QueryWrapper<PostLike> query = new QueryWrapper<>();
        query.eq("post_id", postId);
        query.eq("user_id", userId);
        PostLike exists = postLikeMapper.selectOne(query);

        // 2. 查帖子信息 (为了获取作者ID)
        Post post = postMapper.selectById(postId);
        if (post == null) return false;

        if (exists != null) {
            // --- 取消点赞逻辑 ---
            postLikeMapper.deleteById(exists.getId());
            post.setLikeCount(Math.max(0, post.getLikeCount() - 1));
            postMapper.updateById(post);

            return false;
        } else {
            // --- 点赞逻辑 ---
            PostLike like = new PostLike();
            like.setPostId(postId);
            like.setUserId(userId);
            like.setCreateTime(LocalDateTime.now());
            postLikeMapper.insert(like);

            // 更新: いいねした人の情報を取得
            SysUser liker = userMapper.selectById(userId);
            int bonus = 1; // デフォルトは +1

            // ⭐ 追加ロジック: 高声望ユーザーからのいいねは価値が高い
            if (liker != null && liker.getReputation() >= 100) {
                bonus = 3; // 認証先輩なら +3
            }

            // 作者に声望ボーナスを与える
            changeReputation(post.getUserId(), bonus, "被高信誉用户点赞");

            // 更新帖子点赞数
            post.setLikeCount(post.getLikeCount() + 1);
            postMapper.updateById(post);
            // ⭐ 触发通知：有人点赞了
            // 只有当点赞人不是作者自己时才通知
            if (!post.getUserId().equals(userId)) {
                String likerName = (liker != null) ? liker.getNickname() : "有人";
                notificationService.send(
                        post.getUserId(),
                        "收到新点赞",
                        likerName + " 赞了你的帖子",
                        1,
                        postId
                );
            }
            return true;
        }
    }

    /**
     * 发表评论 / 评分 (完整版：含声望权重 + 系统通知)
     */
    public void addComment(Comment comment) {
        // 1. 获取评价人 (用于计算权重)
        SysUser user = userMapper.selectById(comment.getUserId());

        // 计算评价人的权重快照 (1.0 基准 + 声望加成)
        // 逻辑：声望 100 的人，权重是 1.5；声望 0 的人，权重是 1.0
        double currentReputation = (user.getReputation() == null) ? 0 : user.getReputation();
        double weight = 1.0 + (currentReputation / 100.0) * 0.5;

        // 限制权重上限为 3.0，防止失衡
        weight = Math.min(weight, 3.0);
        comment.setWeightSnapshot(weight);

        // 2. 处理评分 (空值默认为0)
        if (comment.getScore() == null) {
            comment.setScore(0.0);
        }

        // 3. 保存评论
        comment.setCreateTime(LocalDateTime.now());
        commentMapper.insert(comment);

        // 4. 更新帖子数据 & 触发通知
        Post post = postMapper.selectById(comment.getPostId());
        if (post != null) {
            // 更新评论数
            post.setCommentCount(post.getCommentCount() + 1);
            postMapper.updateById(post);

            // 奖励作者声望 (如果被打高分 >= 4.0)
            if (comment.getScore() >= 4.0) {
                changeReputation(post.getUserId(), 3, "获得高分评价");
            }

            // ⭐⭐ 新增：触发系统通知 (如果评论人不是作者自己) ⭐⭐
            if (!post.getUserId().equals(comment.getUserId())) {
                String commenterName = (user != null) ? user.getNickname() : "有人";

                // 调用 NotificationService 发送通知
                // 参数：接收人ID, 标题, 内容, 类型(2=评论), 关联ID(帖子ID)
                notificationService.send(
                        post.getUserId(),
                        "收到新评论",
                        commenterName + " 评论了你：" + comment.getContent(),
                        2,
                        post.getId()
                );
            }
        }

        // 5. 给发评论的人自己 +2 分
        changeReputation(comment.getUserId(), 2, "发布评论奖励");
    }

    public List<CommentVo> getComments(Long postId) {
        QueryWrapper<Comment> query = new QueryWrapper<>();
        query.eq("post_id", postId);
        query.orderByDesc("create_time");
        List<Comment> comments = commentMapper.selectList(query);
        List<CommentVo> result = new ArrayList<>();
        for (Comment c : comments) {
            CommentVo vo = new CommentVo();
            BeanUtils.copyProperties(c, vo);

            SysUser user = userMapper.selectById(c.getUserId());
            if (user != null) {
                vo.setNickname(user.getNickname());
                vo.setAvatar(user.getAvatar());
                vo.setIdentityType(user.getIdentityType());
                // ⭐ 新增：将评论人的声望值传给前端 (需要在 CommentVo 中添加字段)
                vo.setReputation(user.getReputation());
            }
            result.add(vo);
        }
        return result;
    }

    public void increaseViewCount(Long postId) {
        Post post = postMapper.selectById(postId);
        if (post != null) {
            post.setViewCount(post.getViewCount() + 1);
            postMapper.updateById(post);
        }
    }

    // 辅助方法：修改声望
    private void changeReputation(Long userId, int change, String reason) {
        SysUser user = userMapper.selectById(userId);
        if (user != null) {
            int current = (user.getReputation() == null) ? 0 : user.getReputation();
            user.setReputation(current + change);
            userMapper.updateById(user);
            System.out.println("【声望变动】用户 " + user.getNickname() + " " + reason + " -> " + user.getReputation());
        }
    }
}