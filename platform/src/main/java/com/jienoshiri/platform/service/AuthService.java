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


        // ⭐ 新增：检查邮箱是否已存在 (修复这个 BUG)
        QueryWrapper<SysUser> queryEmail = new QueryWrapper<>();
        queryEmail.eq("email", dto.getEmail());
        if (userMapper.selectCount(queryEmail) > 0) {
            throw new RuntimeException("该邮箱已被注册，请直接登录！");
        }

        // 2. 创建用户对象
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setNickname(dto.getNickname());
        user.setEmail(dto.getEmail());

        // 设置身份 (直接存字符串，没问题)
        user.setIdentityType(dto.getIdentityType());

        user.setIsVerified(false); // 默认未认证
        user.setStatus(1);         // 状态正常

        // 3. ⭐ 核心修改：根据身份初始化声望 (Reputation)
        // 逻辑依据：留学生是社区信任基石(给分)，中介需要积累信誉(扣分/低分)
        String type = dto.getIdentityType();
        int initReputation = 0;

        if ("student".equals(type)) {
            initReputation = 100;  // 留学生：高信任起步
        } else if ("agent".equals(type)) {
            initReputation = -50;  // 中介：负分起步，防止发广告
        } else if ("worker".equals(type)) {
            initReputation = 10;   // 工作党：少量信任
        } else {
            initReputation = 0;    // 游客：0
        }

        user.setReputation(initReputation);

        // 4. 密码加密
        user.setPassword(passwordEncoder.encode(dto.getPassword()));

        // 5. 保存到数据库
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