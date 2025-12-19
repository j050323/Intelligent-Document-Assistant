package com.docassistant.auth.security;

import com.docassistant.auth.entity.User;
import com.docassistant.auth.repository.UserRepository;
import com.docassistant.auth.service.TokenService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * JWT认证过滤器
 * 拦截所有请求，验证JWT令牌并设置Spring Security上下文
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final TokenService tokenService;
    private final UserRepository userRepository;
    
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        
        try {
            String jwt = extractJwtFromRequest(request);
            
            if (jwt != null) {
                // 验证令牌
                Claims claims = tokenService.validateToken(jwt);
                
                // 检查令牌是否在黑名单中
                if (tokenService.isTokenBlacklisted(jwt)) {
                    log.debug("Token is blacklisted");
                    filterChain.doFilter(request, response);
                    return;
                }
                
                // 提取用户信息
                Long userId = getUserIdFromClaims(claims);
                String username = claims.get("username", String.class);
                String role = claims.get("role", String.class);
                
                // 检查令牌是否在密码更改之前签发
                if (userId != null) {
                    Optional<User> userOpt = userRepository.findById(userId);
                    if (userOpt.isPresent()) {
                        User user = userOpt.get();
                        if (tokenService.isTokenIssuedBeforePasswordChange(jwt, user.getPasswordChangedAt())) {
                            log.debug("Token was issued before password change, rejecting");
                            filterChain.doFilter(request, response);
                            return;
                        }
                    }
                }
                
                if (userId != null && username != null && role != null) {
                    // 创建权限列表
                    List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                            new SimpleGrantedAuthority("ROLE_" + role)
                    );
                    
                    // 创建认证对象
                    UsernamePasswordAuthenticationToken authentication = 
                            new UsernamePasswordAuthenticationToken(userId, null, authorities);
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    // 设置到Security上下文
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                    log.debug("Set authentication for user: {} with role: {}", username, role);
                }
            }
        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage());
        }
        
        filterChain.doFilter(request, response);
    }
    
    /**
     * 从请求头中提取JWT令牌
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        
        return null;
    }
    
    /**
     * 从Claims中提取用户ID
     */
    private Long getUserIdFromClaims(Claims claims) {
        Object userIdObj = claims.get("userId");
        
        if (userIdObj instanceof Integer) {
            return ((Integer) userIdObj).longValue();
        } else if (userIdObj instanceof Long) {
            return (Long) userIdObj;
        }
        
        return null;
    }
}
