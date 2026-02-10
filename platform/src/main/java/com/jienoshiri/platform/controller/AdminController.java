package com.jienoshiri.platform.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jienoshiri.platform.entity.Post;
import com.jienoshiri.platform.entity.SysUser;
import com.jienoshiri.platform.entity.Wiki;
import com.jienoshiri.platform.mapper.PostMapper;
import com.jienoshiri.platform.mapper.UserMapper;
import com.jienoshiri.platform.mapper.WikiMapper;
import com.jienoshiri.platform.mapper.SysConfigMapper; // 👈 追加
import com.jienoshiri.platform.service.PostService;   // 👈 追加
import com.jienoshiri.platform.utils.TokenResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.jienoshiri.platform.utils.JwtUtil;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;

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
    private TokenResolver tokenResolver;

    @Autowired
    private SysConfigMapper sysConfigMapper; // 👈 設定用Mapper

    @Autowired
    private PostService postService; // ⭐ 修正: 直接 Service を注入 (SpringContextUtilを使わない)

    @Autowired
    private JwtUtil jwtUtil;

    // --- 权限校验辅助方法 ---
    private void checkAdmin(String token) {
        SysUser user = getOperator(token);
        if (!"admin".equals(user.getRole()) && !"super_admin".equals(user.getRole())) {
            throw new RuntimeException("无权访问：需要管理员权限");
        }
    }

    private SysUser getOperator(String token) {
        String username = jwtUtil.getUsername(token);
        SysUser user = userMapper.selectOne(new QueryWrapper<SysUser>().eq("username", username));
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        return user;
    }

    private void checkSuperAdmin(String token) {
        SysUser user = getOperator(token);
        if (!"super_admin".equals(user.getRole())) {
            throw new RuntimeException("无权访问：需要超级管理员权限");
        }
    }

    // 1. 获取待审核帖子列表
    @GetMapping("/audit/posts")
    public List<Post> getPendingPosts(@RequestHeader("Authorization") String token) {
        checkAdmin(token);
        return postMapper.selectList(new QueryWrapper<Post>()
                .in("status", 0, 2, 4) // 待审核 + 驳回 + 违规下架
                .orderByDesc("update_time")
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
        } else {
            wiki.setStatus(2);
            wiki.setRejectReason(reason);
        }
        wikiMapper.updateById(wiki);
        return "操作成功";
    }

    // 5. 获取待审核用户列表
    @GetMapping("/audit/users")
    public List<SysUser> getPendingUsers(@RequestHeader("Authorization") String token) {
        checkAdmin(token);
        return userMapper.selectList(new QueryWrapper<SysUser>()
                .eq("audit_status", 1)
                .orderByDesc("create_time"));
    }

    // 6. 审核用户
    @PostMapping("/audit/user")
    public String auditUser(@RequestHeader("Authorization") String token,
                            @RequestParam Long id,
                            @RequestParam Boolean pass,
                            @RequestParam(required = false) String reason) {
        checkAdmin(token);
        SysUser user = userMapper.selectById(id);
        if (user == null) return "用户不存在";

        if (pass) {
            user.setAuditStatus(2);
            user.setIsVerified(true);
            user.setRejectReason(null);
        } else {
            user.setAuditStatus(3);
            user.setIsVerified(false);
            user.setRejectReason(reason);
        }

        userMapper.updateById(user);
        return "操作成功";
    }

    // 7. 封号并下架违规帖
    @PostMapping("/ban")
    public String banUserAndDelPost(@RequestHeader("Authorization") String token,
                                    @RequestParam Long postId,
                                    @RequestParam String reason) {
        checkAdmin(token);

        Post post = postMapper.selectById(postId);
        if (post == null) return "帖子不存在";

        SysUser user = userMapper.selectById(post.getUserId());
        if (user != null) {
            user.setStatus(0);
            user.setRejectReason("因发布违规内容被封禁: " + reason);
            userMapper.updateById(user);
        }

        post.setStatus(4); // 违规下架
        post.setRejectReason(reason);
        postMapper.updateById(post);
        return "操作成功：已下架帖子并封号";
    }

    // 8. 硬删除帖子（仅超级管理员）
    @PostMapping("/post/hard-delete")
    public String hardDeletePost(@RequestHeader("Authorization") String token,
                                 @RequestParam Long postId) {
        checkSuperAdmin(token);

        Post post = postMapper.selectById(postId);
        if (post == null) return "帖子不存在";

        postMapper.deleteById(postId);
        return "操作成功：已硬删除帖子";
    }

    // ----------- Dashboard 数据接口 -----------

    @GetMapping("/stats")
    public Map<String, Object> getDashboardStats(@RequestHeader("Authorization") String token) {
        checkAdmin(token);
        Map<String, Object> data = new java.util.HashMap<>();

        data.put("totalUsers", userMapper.selectCount(null));
        data.put("totalPosts", postMapper.selectCount(null));

        String today = java.time.LocalDate.now().toString();
        data.put("newUsersToday", userMapper.selectCount(new QueryWrapper<SysUser>().apply("DATE(create_time) = {0}", today)));

        // 分类统计 (简易版)
        List<Post> allPosts = postMapper.selectList(null);
        Map<String, Integer> catCount = new java.util.HashMap<>();
        for (Post p : allPosts) {
            String c = p.getCategory() == null ? "其他" : p.getCategory();
            catCount.put(c, catCount.getOrDefault(c, 0) + 1);
        }
        data.put("categoryStats", catCount);

        return data;
    }

    // ----------- 权重配置接口 -----------

    @GetMapping("/config/list")
    public List<com.jienoshiri.platform.entity.SysConfig> getConfigList(@RequestHeader("Authorization") String token) {
        checkAdmin(token);
        List<com.jienoshiri.platform.entity.SysConfig> configList = sysConfigMapper.selectList(null);
        for (com.jienoshiri.platform.entity.SysConfig config : configList) {
            if ("weight_wiki".equals(config.getParamKey())) {
                config.setDescription("已转Wiki帖子(status=3)加权");
            }
        }
        return configList;
    }

    @PostMapping("/config/update")
    public String updateConfig(@RequestHeader("Authorization") String token,
                               @RequestBody Map<String, String> params) {
        checkAdmin(token);

        String key = params.get("key");
        String value = params.get("value");

        com.jienoshiri.platform.entity.SysConfig config = sysConfigMapper.selectOne(new QueryWrapper<com.jienoshiri.platform.entity.SysConfig>().eq("param_key", key));
        if (config != null) {
            config.setParamValue(value);
            sysConfigMapper.updateById(config);

            // ⭐ 修正: 直接调用注入的 postService，不再报错
            postService.refreshWeights();
        }
        return "更新成功";
    }
}
