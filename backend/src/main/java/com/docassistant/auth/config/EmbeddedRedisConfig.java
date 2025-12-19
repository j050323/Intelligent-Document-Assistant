package com.docassistant.auth.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import redis.embedded.RedisServer;

import java.io.IOException;

/**
 * 嵌入式 Redis 配置（仅用于开发环境）
 * 自动启动一个内存中的 Redis 服务器
 */
@Slf4j
@Configuration
@Profile("dev")
public class EmbeddedRedisConfig {
    
    private final RedisProperties redisProperties;
    private RedisServer redisServer;
    
    public EmbeddedRedisConfig(RedisProperties redisProperties) {
        this.redisProperties = redisProperties;
    }
    
    @PostConstruct
    public void startRedis() throws IOException {
        int port = redisProperties.getPort();
        
        try {
            redisServer = new RedisServer(port);
            redisServer.start();
            
            log.info("=".repeat(80));
            log.info("嵌入式 Redis 已启动");
            log.info("端口: {}", port);
            log.info("这是开发环境配置，数据将在应用重启后丢失");
            log.info("=".repeat(80));
        } catch (Exception e) {
            log.warn("无法启动嵌入式 Redis: {}", e.getMessage());
            log.warn("如果端口 {} 已被占用，请修改 application-dev.yml 中的 spring.data.redis.port", port);
        }
    }
    
    @PreDestroy
    public void stopRedis() {
        if (redisServer != null && redisServer.isActive()) {
            redisServer.stop();
            log.info("嵌入式 Redis 已停止");
        }
    }
}
