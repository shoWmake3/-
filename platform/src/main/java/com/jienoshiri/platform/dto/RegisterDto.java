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
    // ⭐ 新增字段：身份认证材料 (接收前端传来的图片URL)
    // 解决 "getIdentityProof 找不到方法" 的报错
    private String identityProof;
}