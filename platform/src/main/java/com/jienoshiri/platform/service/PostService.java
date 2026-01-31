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
            // 只有当没有 UserCF 推荐时，才凸显这个
            if (contentBasedIds.contains(post.getId())) {
                score += 500;

                // ⭐ 修复 BUG：
                // 之前的代码：if (!vo.getTitle().startsWith("【"))
                // 现在的代码：只要没有被"猜你喜欢"标记过，就加上"精选"。
                // 这样即使标题原本是 "【隐藏】xxx"，也能变成 "【精选】【隐藏】xxx"
                if (!vo.getTitle().startsWith("【猜你喜欢】")) {
                    vo.setTitle("【精选】" + vo.getTitle());
                }
            }

            // 权重 3: LBS (离得近) -> +300分
            if (vo.getDistance() != null && vo.getDistance() < 10) {
                score += 300;
            }

            // 权重 4: Wiki 百科 -> +200分
            if (post.getStatus() == 3) {
                score += 200;
            }

            // 权重 5: 时间分
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

            // (可选) 取消点赞是否要扣分？通常不扣，或者扣 -1
            // changeReputation(post.getUserId(), -1, "取消点赞");

            return false;
        } else {
            // --- 点赞逻辑 ---
            PostLike like = new PostLike();
            like.setPostId(postId);
            like.setUserId(userId);
            like.setCreateTime(LocalDateTime.now());
            postLikeMapper.insert(like);

            // ⭐ 核心修改：给帖子作者加分 (+1分)
            // 注意：是给 post.getUserId() (作者) 加分，不是给 userId (点赞人) 加分
            changeReputation(post.getUserId(), 1, "帖子被点赞");

            // 更新帖子点赞数
            post.setLikeCount(post.getLikeCount() + 1);
            postMapper.updateById(post);
            return true;
        }
    }

    /**
     * 发表评论 / 评分
     */
    public void addComment(Comment comment) {
        // 1. 获取评价人 (用于计算权重)
        SysUser user = userMapper.selectById(comment.getUserId());

        // 计算评价人的权重快照 (1.0 基准 + 声望加成)
        double currentReputation = (user.getReputation() == null) ? 0 : user.getReputation();
        double weight = 1.0 + (currentReputation / 100.0) * 0.5;
        comment.setWeightSnapshot(weight);

        // 2. 处理评分 (空值默认为0)
        if (comment.getScore() == null) {
            comment.setScore(0.0);
        }

        // 3. 保存评论
        comment.setCreateTime(LocalDateTime.now());
        commentMapper.insert(comment);

        // 4. 更新帖子的评论数
        Post post = postMapper.selectById(comment.getPostId());
        if (post != null) {
            post.setCommentCount(post.getCommentCount() + 1);
            postMapper.updateById(post);

            // ⭐⭐ 核心修改：如果被打高分，奖励帖子作者 ⭐⭐
            // 逻辑：如果评分 >= 4.0，作者声望 +3
            if (comment.getScore() >= 4.0) {
                // 注意：这里是给 post.getUserId() (作者) 加分
                changeReputation(post.getUserId(), 3, "获得高分评价");
            }
        }

        // (可选) 给评价人自己也加点辛苦分 (+1)
        // changeReputation(user.getId(), 1, "发布评价");
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