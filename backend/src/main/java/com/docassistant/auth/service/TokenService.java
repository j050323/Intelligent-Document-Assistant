package com.docassistant.auth.service;

import com.docassistant.auth.entity.User;
import io.jsonwebtoken.Claims;

/**
 * JWT令牌管理服务接口
 * 负责JWT令牌的生成、验证和黑名单管理
 */
public interface TokenService {
    
    /**
     * 生成访问令牌
     * 
     * @param user 用户实体
     * @param rememberMe 是否记住登录状态
     * @return JWT访问令牌
     */
    String generateAccessToken(User user, boolean rememberMe);
    
    /**
     * 生成刷新令牌
     * 
     * @param user 用户实体
     * @return JWT刷新令牌
     */
    String generateRefreshToken(User user);
    
    /**
     * 验证并解析令牌
     * 
     * @param token JWT令牌
     * @return 令牌中的声明信息
     * @throws io.jsonwebtoken.JwtException 如果令牌无效或已过期
     */
    Claims validateToken(String token);
    
    /**
     * 将令牌加入黑名单
     * 
     * @param token JWT令牌
     */
    void invalidateToken(String token);
    
    /**
     * 检查令牌是否在黑名单中
     * 
     * @param token JWT令牌
     * @return true如果令牌已被列入黑名单，否则返回false
     */
    boolean isTokenBlacklisted(String token);
    
    /**
     * 从令牌中提取用户ID
     * 
     * @param token JWT令牌
     * @return 用户ID
     */
    Long getUserIdFromToken(String token);
    
    /**
     * 从令牌中提取用户名
     * 
     * @param token JWT令牌
     * @return 用户名
     */
    String getUsernameFromToken(String token);
    
    /**
     * 检查令牌是否在密码更改之前签发
     * 
     * @param token JWT令牌
     * @param passwordChangedAt 密码更改时间
     * @return true如果令牌在密码更改之前签发，否则返回false
     */
    boolean isTokenIssuedBeforePasswordChange(String token, java.time.LocalDateTime passwordChangedAt);
}
