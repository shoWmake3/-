package com.jienoshiri.platform.controller;

import com.jienoshiri.platform.dto.CommentVo;
import com.jienoshiri.platform.dto.PostVo;
import com.jienoshiri.platform.entity.Comment;
import com.jienoshiri.platform.entity.Post;
import com.jienoshiri.platform.entity.SysUser;
import com.jienoshiri.platform.mapper.PostMapper;
import com.jienoshiri.platform.mapper.UserMapper;
import com.jienoshiri.platform.service.PostService;
import com.jienoshiri.platform.utils.JwtUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/post")
public class PostController {

    @Autowired
    private PostService postService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PostMapper postMapper;


    // 获取首页帖子列表
    @GetMapping("/list")
    public List<PostVo> list(
            @RequestParam(required = false) BigDecimal lat,
            @RequestParam(required = false) BigDecimal lng,
            @RequestParam(required = false) String keyword,
            @RequestHeader(value = "Authorization", required = false) String token // 获取 Token
    ) {
        Long userId = null;
        String identityType = null; // ⭐ 新增变量
        if (token != null && !token.isEmpty()) {
            try {
                // 解析 Token 拿用户ID (先拿用户名再查ID，或者JWT里直接存ID)
                String username = jwtUtil.getUsername(token);
                SysUser user = userMapper.selectOne(new QueryWrapper<SysUser>().eq("username", username));
                if (user != null){
                    userId = user.getId();
                    identityType = user.getIdentityType(); // ⭐ 获取身份
                }
            } catch (Exception e) {
                // Token 无效或过期，当游客处理
            }
        }
        return postService.getPostList(lat, lng, keyword, userId, identityType);
    }

    // 发布帖子
    @PostMapping("/publish")
    public String publish(@RequestBody Post post, @RequestHeader("Authorization") String token) {
        // 1. 从 Token 中获取当前用户名
        // Token 格式通常是 "Bearer xxxxx"，这里简单处理直接传 token 字符串
        // 如果你的前端带了 "Bearer " 前缀，需要 substring(7)
        String username = jwtUtil.getUsername(token);

        // 2. 找到用户ID
        QueryWrapper<SysUser> query = new QueryWrapper<>();
        query.eq("username", username);
        SysUser user = userMapper.selectOne(query);

        // 3. 设置作者ID
        post.setUserId(user.getId());

        // 4. 保存
        postService.publishPost(post);
        return "发布成功";
    }

    /**
     * 点赞接口
     */
    @PostMapping("/like")
    public String like(@RequestParam Long postId, @RequestHeader("Authorization") String token) {
        String username = jwtUtil.getUsername(token);
        SysUser user = userMapper.selectOne(new QueryWrapper<SysUser>().eq("username", username));

        boolean isLike = postService.toggleLike(postId, user.getId());
        return isLike ? "点赞成功" : "取消点赞";
    }

    /**
     * 发评论接口
     */
    @PostMapping("/comment")
    public String addComment(@RequestBody Comment comment, @RequestHeader("Authorization") String token) {
        String username = jwtUtil.getUsername(token);
        SysUser user = userMapper.selectOne(new QueryWrapper<SysUser>().eq("username", username));

        comment.setUserId(user.getId());
        postService.addComment(comment);
        return "评论成功";
    }

    /**
     * 获取评论列表接口 (公开接口，无需登录)
     */
    @GetMapping("/comments")
    public List<CommentVo> getComments(@RequestParam Long postId) {
        return postService.getComments(postId);
    }

    // 新增接口：增加阅读量
    @PostMapping("/view/{id}")
    public String increaseView(@PathVariable Long id) {
        postService.increaseViewCount(id);
        return "success";
    }

    /**
     * ⭐ 新增：获取我发布的帖子
     */
    @GetMapping("/my")
    public List<PostVo> getMyPosts(@RequestHeader("Authorization") String token) {
        String username = jwtUtil.getUsername(token);
        SysUser user = userMapper.selectOne(new QueryWrapper<SysUser>().eq("username", username));

        // 复用 Service 里的逻辑，或者直接查
        // 为了简单，我们这里直接写查询逻辑
        QueryWrapper<Post> query = new QueryWrapper<>();
        query.eq("user_id", user.getId());
        query.orderByDesc("create_time");

        List<Post> posts = postMapper.selectList(query);

        // 转 VO
        List<PostVo> result = new ArrayList<>();
        for (Post p : posts) {
            PostVo vo = new PostVo();
            BeanUtils.copyProperties(p, vo);
            vo.setAuthorName(user.getNickname());
            vo.setAuthorAvatar(user.getAvatar());
            result.add(vo);
        }
        return result;
    }
}