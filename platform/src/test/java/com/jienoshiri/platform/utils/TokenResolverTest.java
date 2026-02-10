package com.jienoshiri.platform.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenResolverTest {

    private final JwtUtil jwtUtil = new JwtUtil();
    private final TokenResolver tokenResolver = new TokenResolver(jwtUtil);

    @Test
    void shouldParseBareToken() {
        String token = jwtUtil.generateToken("alice");

        assertTrue(tokenResolver.validate(token));
        assertEquals("alice", tokenResolver.getUsername(token));
    }

    @Test
    void shouldParseBearerToken() {
        String token = jwtUtil.generateToken("bob");
        String bearerToken = "Bearer " + token;

        assertTrue(tokenResolver.validate(bearerToken));
        assertEquals("bob", tokenResolver.getUsername(bearerToken));
    }
}
