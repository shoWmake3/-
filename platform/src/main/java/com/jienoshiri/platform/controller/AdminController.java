package com.jienoshiri.platform.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.jienoshiri.platform.entity.Post;
import com.jienoshiri.platform.entity.SysUser;
import com.jienoshiri.platform.entity.Wiki;
import com.jienoshiri.platform.mapper.PostMapper;
import com.jienoshiri.platform.mapper.UserMapper;
import com.jienoshiri.platform.mapper.WikiMapper;
import com.jienoshiri.platform.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private PostMapper postMapper;
    @Autowired
    private WikiMapper wikiMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private JwtUtil jwtUtil;

    // --- 权限校验辅助方法 ---
    private void checkAdmin(String token) {
        String username = jwtUtil.getUsername(token);
        SysUser user = userMapper.selectOne(new QueryWrapper<SysUser>().eq("username", username));
        if (user == null || !"admin".equals(user.getRole())) {
            throw new RuntimeException("无权访问：需要管理员权限");
        }
    }

    // 1. 获取待审核帖子列表
    @GetMapping("/audit/posts")
    public List<Post> getPendingPosts(@RequestHeader("Authorization") String token) {
        checkAdmin(token);
        return postMapper.selectList(new QueryWrapper<Post>()
                .eq("status", 0) // 0=待审核
                .orderByDesc("create_time"));
    }

    // 2. 审核帖子 (pass/reject)
    @PostMapping("/audit/post")
    public String auditPost(@RequestHeader("Authorization") String token,
                            @RequestParam Long id,
                            @RequestParam Boolean pass,
                            @RequestParam(required = false) String reason) {
        checkAdmin(token);
        Post post = postMapper.selectById(id);
        if (post == null) return "帖子不存在";

        if (pass) {
            post.setStatus(1); // 通过
        } else {
            post.setStatus(2); // 拒绝
            post.setRejectReason(reason);
        }
        postMapper.updateById(post);
        return "操作成功";
    }

    // 3. 获取待审核 Wiki 列表
    @GetMapping("/audit/wikis")
    public List<Wiki> getPendingWikis(@RequestHeader("Authorization") String token) {
        checkAdmin(token);
        return wikiMapper.selectList(new QueryWrapper<Wiki>()
                .eq("status", 0)
                .orderByDesc("create_time"));
    }

    // 4. 审核 Wiki
    @PostMapping("/audit/wiki")
    public String auditWiki(@RequestHeader("Authorization") String token,
                            @RequestParam Long id,
                            @RequestParam Boolean pass,
                            @RequestParam(required = false) String reason) {
        checkAdmin(token);
        Wiki wiki = wikiMapper.selectById(id);
        if (wiki == null) return "词条不存在";

        if (pass) {
            wiki.setStatus(1);
            // TODO: 这里应该同步更新 ES，为演示简便暂略
        } else {
            wiki.setStatus(2);
            wiki.setRejectReason(reason);
        }
        wikiMapper.updateById(wiki);
        return "操作成功";
    }
}