package com.docassistant.document.config;

import jakarta.servlet.MultipartConfigElement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;

import java.io.File;

/**
 * 文件上传配置
 */
@Slf4j
@Configuration
public class FileUploadConfig {
    
    @Value("${spring.servlet.multipart.max-file-size:100MB}")
    private String maxFileSize;
    
    @Value("${spring.servlet.multipart.max-request-size:100MB}")
    private String maxRequestSize;
    
    @Value("${app.file-storage.document-path:./uploads/documents}")
    private String documentPath;
    
    /**
     * 配置MultipartResolver
     * Spring Boot默认使用StandardServletMultipartResolver
     * 这里显式配置以确保正确处理文件上传
     */
    @Bean
    public StandardServletMultipartResolver multipartResolver() {
        log.info("Configuring multipart resolver with max file size: {}, max request size: {}", 
                maxFileSize, maxRequestSize);
        return new StandardServletMultipartResolver();
    }
    
    /**
     * 配置MultipartConfigElement
     * 设置文件大小限制和临时文件目录
     */
    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        
        // 设置单个文件最大大小
        factory.setMaxFileSize(DataSize.parse(maxFileSize));
        
        // 设置整个请求最大大小
        factory.setMaxRequestSize(DataSize.parse(maxRequestSize));
        
        // 设置临时文件目录
        String tempDir = System.getProperty("java.io.tmpdir");
        File uploadTempDir = new File(tempDir, "document-uploads");
        if (!uploadTempDir.exists()) {
            boolean created = uploadTempDir.mkdirs();
            if (created) {
                log.info("Created temporary upload directory: {}", uploadTempDir.getAbsolutePath());
            }
        }
        factory.setLocation(uploadTempDir.getAbsolutePath());
        
        log.info("Configured multipart with max file size: {}, max request size: {}, temp dir: {}", 
                maxFileSize, maxRequestSize, uploadTempDir.getAbsolutePath());
        
        return factory.createMultipartConfig();
    }
    
    /**
     * 确保文档存储目录存在
     */
    @Bean
    public Boolean ensureDocumentStorageDirectory() {
        File documentDir = new File(documentPath);
        if (!documentDir.exists()) {
            boolean created = documentDir.mkdirs();
            if (created) {
                log.info("Created document storage directory: {}", documentDir.getAbsolutePath());
            } else {
                log.warn("Failed to create document storage directory: {}", documentDir.getAbsolutePath());
            }
            return created;
        } else {
            log.info("Document storage directory exists: {}", documentDir.getAbsolutePath());
            return true;
        }
    }
}
