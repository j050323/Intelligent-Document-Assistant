package com.docassistant.document.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 缓存配置类
 */
@Configuration
@EnableCaching
public class CacheConfig {
    
    public static final String DOCUMENT_LIST_CACHE = "documentList";
    public static final String FOLDER_TREE_CACHE = "folderTree";
    public static final String DOCUMENT_DETAIL_CACHE = "documentDetail";
    public static final String STORAGE_INFO_CACHE = "storageInfo";
    
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // 配置ObjectMapper以支持Java 8日期时间类型
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.activateDefaultTyping(
            LaissezFaireSubTypeValidator.instance,
            ObjectMapper.DefaultTyping.NON_FINAL,
            JsonTypeInfo.As.PROPERTY
        );
        
        // 创建支持Java 8日期时间的JSON序列化器
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);
        
        // 默认缓存配置
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))  // 默认10分钟过期
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .disableCachingNullValues();
        
        // 为不同的缓存设置不同的过期时间
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // 文档列表缓存：5分钟
        cacheConfigurations.put(DOCUMENT_LIST_CACHE, 
                defaultConfig.entryTtl(Duration.ofMinutes(5)));
        
        // 文件夹树缓存：10分钟
        cacheConfigurations.put(FOLDER_TREE_CACHE, 
                defaultConfig.entryTtl(Duration.ofMinutes(10)));
        
        // 文档详情缓存：15分钟
        cacheConfigurations.put(DOCUMENT_DETAIL_CACHE, 
                defaultConfig.entryTtl(Duration.ofMinutes(15)));
        
        // 存储空间信息缓存：3分钟
        cacheConfigurations.put(STORAGE_INFO_CACHE, 
                defaultConfig.entryTtl(Duration.ofMinutes(3)));
        
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}
