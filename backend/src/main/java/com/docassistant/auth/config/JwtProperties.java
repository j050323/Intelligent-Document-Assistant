package com.docassistant.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * JWT配置属性类
 * 从application.yml读取JWT相关配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    
    /**
     * JWT签名密钥
     */
    private String secret;
    
    /**
     * 访问令牌过期时间（毫秒）
     */
    private Long accessTokenExpiration;
    
    /**
     * 刷新令牌过期时间（毫秒）
     */
    private Long refreshTokenExpiration;
    
    /**
     * 记住我令牌过期时间（毫秒）
     */
    private Long rememberMeExpiration;
}
