package com.jienoshiri.platform.dto;

import lombok.Data;

@Data
public class LoginDto {
    private String username;
    private String password;
    private String role;

    // 后面如果要加“身份类型”或“验证码”，都加在这里
}