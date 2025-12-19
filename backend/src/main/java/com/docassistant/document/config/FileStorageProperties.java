package com.docassistant.document.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 文件存储配置属性
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.file-storage")
public class FileStorageProperties {
    
    /**
     * 头像存储路径
     */
    private String avatarPath;
    
    /**
     * 文档存储路径
     */
    private String documentPath;
}
