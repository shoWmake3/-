package com.jienoshiri.platform.utils;

import org.springframework.stereotype.Component;

@Component
public class TokenResolver {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;

    public TokenResolver(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    public String normalizeToken(String tokenOrHeader) {
        if (tokenOrHeader == null) {
            return null;
        }

        String trimmed = tokenOrHeader.trim();
        if (trimmed.startsWith(BEARER_PREFIX)) {
            return trimmed.substring(BEARER_PREFIX.length()).trim();
        }
        return trimmed;
    }

    public boolean validate(String tokenOrHeader) {
        String token = normalizeToken(tokenOrHeader);
        return token != null && !token.isEmpty() && jwtUtil.validateToken(token);
    }

    public String getUsername(String tokenOrHeader) {
        String token = normalizeToken(tokenOrHeader);
        if (token == null || token.isEmpty()) {
            throw new IllegalArgumentException("Token 不能为空");
        }
        return jwtUtil.getUsername(token);
    }
}
