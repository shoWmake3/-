package com.jienoshiri.platform.dto;

import lombok.Data;

@Data
public class RegisterDto {
    private String username;
    private String password;
    private String nickname;
    private String email;
    // 身份: student, tourist, worker, agent
    private String identityType;
}