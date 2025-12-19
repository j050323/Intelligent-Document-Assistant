package com.docassistant.auth.config;

import com.docassistant.auth.security.JwtAccessDeniedHandler;
import com.docassistant.auth.security.JwtAuthenticationEntryPoint;
import com.docassistant.auth.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Spring Security配置类
 * 配置JWT认证、权限控制、CORS和CSRF保护
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
    private final CorsConfigurationSource corsConfigurationSource;
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 配置CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            
            // 禁用CSRF（因为使用JWT，不需要CSRF保护）
            .csrf(AbstractHttpConfigurer::disable)
            
            // 配置异常处理
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                .accessDeniedHandler(jwtAccessDeniedHandler)
            )
            
            // 配置会话管理为无状态
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // 配置授权规则
            .authorizeHttpRequests(auth -> auth
                // 公开的认证接口
                .requestMatchers(
                    "/api/auth/register",
                    "/api/auth/verify-email",
                    "/api/auth/login",
                    "/api/auth/forgot-password",
                    "/api/auth/reset-password"
                ).permitAll()
                
                // 需要认证的接口
                .requestMatchers(
                    "/api/auth/logout",
                    "/api/auth/refresh-token"
                ).authenticated()
                
                // 用户个人信息接口（需要认证）
                .requestMatchers(
                    HttpMethod.GET, "/api/users/me"
                ).authenticated()
                .requestMatchers(
                    HttpMethod.PUT, "/api/users/me"
                ).authenticated()
                .requestMatchers(
                    HttpMethod.POST, "/api/users/me/avatar"
                ).authenticated()
                .requestMatchers(
                    HttpMethod.PUT, "/api/users/me/email"
                ).authenticated()
                .requestMatchers(
                    HttpMethod.PUT, "/api/users/me/password"
                ).authenticated()
                
                // 管理员专用接口
                .requestMatchers(
                    "/api/users/{id}",
                    "/api/users/{id}/role"
                ).hasRole("ADMINISTRATOR")
                .requestMatchers(
                    "/api/logs/**"
                ).hasRole("ADMINISTRATOR")
                
                // 其他所有请求都需要认证
                .anyRequest().authenticated()
            )
            
            // 添加JWT过滤器
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}
