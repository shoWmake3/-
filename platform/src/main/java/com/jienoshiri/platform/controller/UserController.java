package com.jienoshiri.platform.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jienoshiri.platform.entity.SysUser;
import com.jienoshiri.platform.mapper.UserMapper;
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

    // ⭐ 更新用户信息接口 (比如头像、昵称)
    @PostMapping("/update")
    public String updateUserInfo(@RequestBody SysUser user, @RequestHeader("Authorization") String token) {
        // 1. 鉴权：确保是改自己的信息
        String username = jwtUtil.getUsername(token);
        SysUser currentUser = userMapper.selectOne(new QueryWrapper<SysUser>().eq("username", username));

        if (currentUser == null) {
            return "用户不存在";
        }

        // 2. 只更新允许修改的字段
        if (user.getAvatar() != null) {
            currentUser.setAvatar(user.getAvatar());
        }
        if (user.getNickname() != null) {
            currentUser.setNickname(user.getNickname());
        }
        // ... 其他字段按需添加

        // 3. 存入数据库
        userMapper.updateById(currentUser);
        return "更新成功";
    }
}