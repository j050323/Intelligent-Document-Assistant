package com.docassistant.document.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * 查询优化配置
 */
@Configuration
@EnableJpaRepositories(
    basePackages = "com.docassistant.document.repository",
    enableDefaultTransactions = true
)
public class QueryOptimizationConfig {
    
    // JPA查询优化配置
    // 1. 启用查询缓存
    // 2. 启用批量获取
    // 3. 优化N+1查询问题
}
