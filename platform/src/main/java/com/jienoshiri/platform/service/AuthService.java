package com.jienoshiri.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jienoshiri.platform.dto.LoginDto;
import com.jienoshiri.platform.dto.RegisterDto;
import com.jienoshiri.platform.entity.SysUser;
import com.jienoshiri.platform.mapper.UserMapper;
import com.jienoshiri.platform.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder; // 注入我们在 SecurityConfig 里配的加密器

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 用户注册 (修正版)
     */
    public String register(RegisterDto dto) {
        // 1. 查重逻辑 (用户名 & 邮箱)
        QueryWrapper<SysUser> query = new QueryWrapper<>();
        query.eq("username", dto.getUsername());
        if (userMapper.selectCount(query) > 0) {
            throw new RuntimeException("用户名已存在！");
        }

        if (dto.getEmail() != null && !dto.getEmail().isEmpty()) {
            QueryWrapper<SysUser> queryEmail = new QueryWrapper<>();
            queryEmail.eq("email", dto.getEmail());
            if (userMapper.selectCount(queryEmail) > 0) {
                throw new RuntimeException("该邮箱已被注册，请直接登录！");
            }
        }

        // 2. 创建用户
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setNickname(dto.getNickname());
        if (dto.getEmail() != null) user.setEmail(dto.getEmail());
        user.setIdentityType(dto.getIdentityType());
        user.setRole("user");
        user.setCreateTime(LocalDateTime.now());
        user.setStatus(1); // status 是 Integer，可以用 1

        // 3. 声望初始化
        String type = dto.getIdentityType();
        int initReputation = 0;
        if ("student".equals(type)) {
            initReputation = 100;
        } else if ("agent".equals(type)) {
            initReputation = -20;
        } else if ("worker".equals(type)) {
            initReputation = 10;
        }
        user.setReputation(initReputation);

        // 4. ⭐ 核心修正：身份审核逻辑 (int -> boolean)
        if ("student".equals(type) || "agent".equals(type)) {
            if (dto.getIdentityProof() != null && !dto.getIdentityProof().isEmpty()) {
                user.setIdentityProof(dto.getIdentityProof());
                user.setAuditStatus(1);     // Integer
                user.setIsVerified(false);  // ⭐ Fix: 0 -> false
            } else {
                user.setAuditStatus(0);
                user.setIsVerified(false);  // ⭐ Fix: 0 -> false
            }
        } else {
            user.setAuditStatus(0);
            user.setIsVerified(true);   // ⭐ Fix: 1 -> true
        }

        // 5. 密码加密并保存
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        userMapper.insert(user);

        return "注册成功";
    }

    /**
     * 用户登录
     * @return JWT Token
     */
    public String login(LoginDto dto) {
        // 1. 查用户
        QueryWrapper<SysUser> query = new QueryWrapper<>();
        query.eq("username", dto.getUsername());
        SysUser user = userMapper.selectOne(query);

        // 2. 校验用户是否存在
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 3. ⭐ 核心：校验密码 (用 match 方法比对明文和密文)
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new RuntimeException("密码错误");
        }

        // 4. 检查账号状态 (风控)
        if (user.getStatus() == 0) {
            throw new RuntimeException("该账号已被封禁");
        }

        // 5. 生成 Token
        return jwtUtil.generateToken(user.getUsername());
    }


}