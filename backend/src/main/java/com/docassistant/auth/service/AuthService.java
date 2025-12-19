package com.docassistant.auth.service;

import com.docassistant.auth.dto.LoginRequest;
import com.docassistant.auth.dto.LoginResponse;
import com.docassistant.auth.dto.RegisterRequest;
import com.docassistant.auth.dto.RegisterResponse;
import com.docassistant.auth.dto.TokenResponse;

/**
 * 认证业务逻辑服务接口
 */
public interface AuthService {
    
    /**
     * 用户注册
     * 
     * @param request 注册请求
     * @return 注册响应
     */
    RegisterResponse register(RegisterRequest request);
    
    /**
     * 验证邮箱
     * 
     * @param email 邮箱地址
     * @param code 验证码
     */
    void verifyEmail(String email, String code);
    
    /**
     * 用户登录
     * 
     * @param request 登录请求
     * @param ipAddress IP地址
     * @param userAgent 用户代理
     * @return 登录响应
     */
    LoginResponse login(LoginRequest request, String ipAddress, String userAgent);
    
    /**
     * 用户登出
     * 
     * @param token JWT令牌
     */
    void logout(String token);
    
    /**
     * 刷新令牌
     * 
     * @param refreshToken 刷新令牌
     * @return 新的令牌响应
     */
    TokenResponse refreshToken(String refreshToken);
    
    /**
     * 请求密码重置
     * 
     * @param email 邮箱地址
     */
    void requestPasswordReset(String email);
    
    /**
     * 重置密码
     * 
     * @param token 重置令牌
     * @param newPassword 新密码
     */
    void resetPassword(String token, String newPassword);
}
