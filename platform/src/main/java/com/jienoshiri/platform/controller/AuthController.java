package com.jienoshiri.platform.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jienoshiri.platform.dto.LoginDto;
import com.jienoshiri.platform.dto.RegisterDto;
import com.jienoshiri.platform.entity.SysUser;
import com.jienoshiri.platform.mapper.UserMapper;
import com.jienoshiri.platform.service.AuthService;
import com.jienoshiri.platform.utils.TokenResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private TokenResolver tokenResolver;

    @Autowired
    private UserMapper userMapper;

    // 注册接口: POST /auth/register
    @PostMapping("/register")
    public String register(@RequestBody RegisterDto dto) {
        return authService.register(dto);
    }

    // 登录接口: POST /auth/login
    @PostMapping("/login")
    public String login(@RequestBody LoginDto dto) {
        return authService.login(dto);
    }

    // ⭐ 核心修复：新增获取当前用户信息接口
    // GET /auth/me
    @GetMapping("/me")
    public SysUser me(@RequestHeader("Authorization") String token) {
        // 1. 从 Token 中解析用户名
        // (前端传来的 Token 如果没有 "Bearer " 前缀，直接解析即可)
        String username = tokenResolver.getUsername(token);

        // 2. 根据用户名查询用户
        QueryWrapper<SysUser> query = new QueryWrapper<>();
        query.eq("username", username);
        SysUser user = userMapper.selectOne(query);

        // 3. 安全起见，把密码抹除再返回
        if (user != null) {
            user.setPassword(null);
        }
        return user;
    }
}