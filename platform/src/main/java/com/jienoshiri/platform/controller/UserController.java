package com.jienoshiri.platform.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jienoshiri.platform.dto.RegisterDto; // 👈 记得导入这个
import com.jienoshiri.platform.entity.SysUser;
import com.jienoshiri.platform.mapper.UserMapper;
import com.jienoshiri.platform.service.AuthService; // 👈 记得导入这个
import com.jienoshiri.platform.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtil jwtUtil;

    // ⭐ 注入 AuthService，否则无法调用注册逻辑
    @Autowired
    private AuthService authService;

    // ⭐⭐ 之前缺失的注册接口，必须补上！ ⭐⭐
    // 对应前端请求: POST http://localhost:8080/user/register
    @PostMapping("/register")
    public String register(@RequestBody RegisterDto dto) {
        return authService.register(dto);
    }

    // 更新用户信息接口
    @PostMapping("/update")
    public String updateUserInfo(@RequestBody SysUser user, @RequestHeader("Authorization") String token) {
        String username = jwtUtil.getUsername(token);
        SysUser currentUser = userMapper.selectOne(new QueryWrapper<SysUser>().eq("username", username));

        if (currentUser == null) {
            return "用户不存在";
        }

        // 只更新允许修改的字段
        if (user.getAvatar() != null) currentUser.setAvatar(user.getAvatar());
        if (user.getNickname() != null) currentUser.setNickname(user.getNickname());

        userMapper.updateById(currentUser);
        return "更新成功";
    }

    // 获取当前用户详细信息
    @GetMapping("/info")
    public SysUser getUserInfo(@RequestHeader("Authorization") String token) {
        String username = jwtUtil.getUsername(token);
        SysUser user = userMapper.selectOne(new QueryWrapper<SysUser>().eq("username", username));
        if (user != null) {
            user.setPassword(null); // 密码脱敏
        }
        return user;
    }
}