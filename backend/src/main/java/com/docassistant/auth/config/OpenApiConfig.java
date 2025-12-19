package com.docassistant.auth.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI (Swagger) 配置类
 * 配置API文档的基本信息和安全认证方案
 */
@Configuration
public class OpenApiConfig {
    
    @Value("${app.api.version:1.0.0}")
    private String apiVersion;
    
    @Value("${app.api.server-url:http://localhost:8080}")
    private String serverUrl;
    
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(List.of(
                        new Server()
                                .url(serverUrl)
                                .description("开发环境服务器")
                ))
                .components(new Components()
                        .addSecuritySchemes("bearer-jwt", securityScheme()))
                .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
    }
    
    /**
     * API基本信息
     */
    private Info apiInfo() {
        return new Info()
                .title("智能文档助手 API")
                .description("""
                        智能文档助手的完整API文档，包含用户认证、文档管理和文件夹管理功能
                        
                        ## 功能特性
                        
                        ### 用户认证与授权
                        - 用户注册与邮箱验证
                        - 用户登录与JWT令牌认证
                        - 密码重置与修改
                        - 个人信息管理
                        - 头像上传
                        - 基于角色的访问控制（RBAC）
                        - 系统日志查询（管理员）
                        
                        ### 文档管理
                        - 文档上传（支持PDF、Word、TXT格式，最大100MB）
                        - 批量上传文档
                        - 文档列表查询（支持分页、搜索、筛选、排序）
                        - 文档在线预览
                        - 文档下载（支持断点续传）
                        - 文档重命名和移动
                        - 文档删除和批量删除
                        - 存储空间管理和配额限制
                        
                        ### 文件夹管理
                        - 创建文件夹（支持多层级结构）
                        - 文件夹列表查询
                        - 文件夹重命名
                        - 删除空文件夹
                        - 查询文件夹中的文档
                        
                        ## 认证方式
                        大部分API需要JWT令牌认证。请先调用登录接口获取令牌，然后在请求头中添加：
                        ```
                        Authorization: Bearer <your-jwt-token>
                        ```
                        
                        ## 用户角色
                        - **REGULAR_USER**: 普通用户，可以管理自己的账户信息和文档
                        - **ADMINISTRATOR**: 管理员，拥有所有权限，可以管理其他用户、查看系统日志和访问所有文档
                        
                        ## 支持的文档格式
                        - **PDF**: .pdf
                        - **Word**: .doc, .docx
                        - **文本**: .txt
                        
                        ## 文件大小限制
                        - 单个文件最大: 100MB
                        - 默认存储配额: 根据用户配置
                        
                        ## 错误代码
                        - **400**: 请求参数错误
                        - **401**: 未认证
                        - **403**: 无权限访问
                        - **404**: 资源不存在
                        - **507**: 存储空间不足
                        """)
                .version(apiVersion)
                .contact(new Contact()
                        .name("智能文档助手团队")
                        .email("support@docassistant.com"))
                .license(new License()
                        .name("MIT License")
                        .url("https://opensource.org/licenses/MIT"));
    }
    
    /**
     * JWT安全认证方案
     */
    private SecurityScheme securityScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("请输入JWT令牌（无需添加'Bearer '前缀）");
    }
}
