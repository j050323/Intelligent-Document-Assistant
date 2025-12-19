package com.docassistant.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密码加密配置类
 * 配置BCryptPasswordEncoder用于密码加密和验证
 */
@Configuration
public class PasswordEncoderConfig {

    /**
     * 创建BCryptPasswordEncoder Bean
     * BCrypt是一种单向哈希算法,适合密码存储
     * 
     * @return PasswordEncoder实例
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
