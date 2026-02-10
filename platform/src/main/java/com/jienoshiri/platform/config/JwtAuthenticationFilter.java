package com.jienoshiri.platform.config;

import com.jienoshiri.platform.utils.TokenResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private TokenResolver tokenResolver;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. 获取请求头中的 Token
        String token = request.getHeader("Authorization");

        // 2. 如果有 Token 且有效
        if (tokenResolver.validate(token)) {
            // 3. 解析出用户名
            String username = tokenResolver.getUsername(token);

            // 4. 生成 Spring Security 认可的认证对象 (这里暂时给空权限 list)
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(username, null, new ArrayList<>());

            // 5. ⭐ 关键一步：把这个人“放进”安全上下文，告诉 Spring Security 他已登录
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        // 6. 继续后续流程
        filterChain.doFilter(request, response);
    }
}
