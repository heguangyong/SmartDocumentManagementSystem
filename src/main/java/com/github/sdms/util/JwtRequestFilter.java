package com.github.sdms.util;

import com.github.sdms.model.User;
import com.github.sdms.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class JwtRequestFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository; // ✅ 替换 UserDetailsService

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        String jwt = null;
        Long userId = null;

        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                jwt = authHeader.substring(7);
                userId = jwtUtil.extractUserId(jwt); // 使用 userId 提取 JWT 中的 userId
            }
            if (userId == null) {
                throw new IllegalArgumentException("User ID cannot be null");
            }
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                // 根据 userId 从数据库中加载用户
                User user = userRepository.findById(userId).orElse(null);

                if (user != null && jwtUtil.validateToken(jwt, new org.springframework.security.core.userdetails.User(user.getEmail(), "", Collections.emptyList()))) {
                    CustomerUserDetails userDetails = new CustomerUserDetails(user);

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            logger.warn("JWT filter exception: {}", e);
        }

        filterChain.doFilter(request, response);
    }

}
