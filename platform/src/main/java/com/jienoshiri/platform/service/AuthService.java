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

@Service
public class AuthService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder; // 注入我们在 SecurityConfig 里配的加密器

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 用户注册
     */
    public String register(RegisterDto dto) {
        // 1. 检查用户名是否已存在
        QueryWrapper<SysUser> query = new QueryWrapper<>();
        query.eq("username", dto.getUsername());
        if (userMapper.selectCount(query) > 0) {
            throw new RuntimeException("用户名已存在！");
        }

        // 2. 创建用户对象
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setNickname(dto.getNickname());
        user.setEmail(dto.getEmail());
        user.setIdentityType(dto.getIdentityType());
        user.setIsVerified(false); // 默认未认证
        user.setReputation(0);     // 默认声望为0
        user.setStatus(1);         // 状态正常

        // 3. ⭐ 核心：密码必须加密存储！
        user.setPassword(passwordEncoder.encode(dto.getPassword()));

        // 4. 保存到数据库
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