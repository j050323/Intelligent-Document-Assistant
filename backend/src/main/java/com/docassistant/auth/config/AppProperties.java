package com.docassistant.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 应用配置属性类
 * 从application.yml读取应用相关配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    
    private String environment;  // 环境标识：dev, prod 等
    private Cors cors = new Cors();
    private VerificationCode verificationCode = new VerificationCode();
    private PasswordReset passwordReset = new PasswordReset();
    private FileStorage fileStorage = new FileStorage();
    
    @Data
    public static class Cors {
        private String allowedOrigins;
    }
    
    @Data
    public static class VerificationCode {
        /**
         * 验证码过期时间（秒）
         */
        private Integer expiration;
    }
    
    @Data
    public static class PasswordReset {
        /**
         * 密码重置令牌过期时间（秒）
         */
        private Integer expiration;
    }
    
    @Data
    public static class FileStorage {
        /**
         * 头像存储路径
         */
        private String avatarPath;
    }
}
