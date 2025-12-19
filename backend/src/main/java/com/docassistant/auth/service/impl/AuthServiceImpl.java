package com.docassistant.auth.service.impl;

import com.docassistant.auth.dto.*;
import com.docassistant.auth.entity.PasswordResetToken;
import com.docassistant.auth.entity.User;
import com.docassistant.auth.entity.UserRole;
import com.docassistant.auth.exception.AuthenticationException;
import com.docassistant.auth.exception.ValidationException;
import com.docassistant.auth.repository.PasswordResetTokenRepository;
import com.docassistant.auth.repository.UserRepository;
import com.docassistant.auth.service.*;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 认证业务逻辑服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    
    private final UserRepository userRepository;
    private final SystemLogService systemLogService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordService passwordService;
    private final TokenService tokenService;
    private final EmailService emailService;
    private final VerificationCodeService verificationCodeService;
    private final com.docassistant.auth.config.AppProperties appProperties;
    
    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        log.info("Processing registration for username: {}, email: {}", request.getUsername(), request.getEmail());
        
        // 验证密码复杂度
        if (!passwordService.isPasswordValid(request.getPassword())) {
            throw new ValidationException(
                "密码必须至少8个字符，并包含字母和数字",
                "PASSWORD_TOO_WEAK"
            );
        }
        
        // 检查邮箱是否已存在
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ValidationException(
                "邮箱已被注册",
                "EMAIL_ALREADY_EXISTS"
            );
        }
        
        // 检查用户名是否已存在
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ValidationException(
                "用户名已被使用",
                "USERNAME_ALREADY_EXISTS"
            );
        }
        
        // 加密密码
        String encodedPassword = passwordService.encodePassword(request.getPassword());
        
        // 创建用户
        // 在开发环境下自动验证邮箱
        boolean isDevMode = appProperties.getEnvironment() != null && 
                           appProperties.getEnvironment().equals("dev");
        
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(encodedPassword)
                .role(UserRole.REGULAR_USER)
                .isEmailVerified(isDevMode)  // 开发环境自动验证
                .build();
        
        user = userRepository.save(user);
        log.info("User created successfully with ID: {}", user.getId());
        
        // 只在非开发环境发送验证邮件
        String message;
        if (!isDevMode) {
            // 生成并发送验证码
            String verificationCode = verificationCodeService.generateAndStoreCode(request.getEmail());
            emailService.sendVerificationCode(request.getEmail(), verificationCode);
            log.info("Verification code sent to email: {}", request.getEmail());
            message = "注册成功，验证码已发送到您的邮箱";
        } else {
            log.info("Development mode: Email verification skipped for user: {}", user.getEmail());
            message = "注册成功，您可以直接登录";
        }
        
        return RegisterResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .message(message)
                .build();
    }
    
    @Override
    @Transactional
    public void verifyEmail(String email, String code) {
        log.info("Verifying email: {}", email);
        
        // 验证验证码
        if (!verificationCodeService.validateCode(email, code)) {
            throw new ValidationException(
                "验证码错误或已过期",
                "INVALID_VERIFICATION_CODE"
            );
        }
        
        // 查找用户
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ValidationException(
                    "用户不存在",
                    "USER_NOT_FOUND"
                ));
        
        // 激活用户账户
        user.setIsEmailVerified(true);
        userRepository.save(user);
        
        // 删除验证码
        verificationCodeService.deleteCode(email);
        
        log.info("Email verified successfully for user: {}", user.getUsername());
    }
    
    @Override
    @Transactional
    public LoginResponse login(LoginRequest request, String ipAddress, String userAgent) {
        log.info("Processing login for: {}", request.getUsernameOrEmail());
        
        // 查找用户（支持用户名或邮箱登录）
        User user = userRepository.findByUsernameOrEmail(
                request.getUsernameOrEmail(),
                request.getUsernameOrEmail()
        ).orElse(null);
        
        // 验证用户存在性和密码
        if (user == null || !passwordService.verifyPassword(request.getPassword(), user.getPasswordHash())) {
            // 记录登录失败日志
            systemLogService.logLoginFailure(request.getUsernameOrEmail(), ipAddress, userAgent, "用户名或密码错误");
            throw new AuthenticationException(
                "用户名或密码错误",
                "INVALID_CREDENTIALS"
            );
        }
        
        // 检查邮箱是否已验证
        if (!user.getIsEmailVerified()) {
            systemLogService.logLoginFailure(request.getUsernameOrEmail(), ipAddress, userAgent, "邮箱未验证");
            throw new AuthenticationException(
                "邮箱未验证，请先验证邮箱",
                "EMAIL_NOT_VERIFIED"
            );
        }
        
        // 生成JWT令牌
        String accessToken = tokenService.generateAccessToken(user, request.getRememberMe());
        String refreshToken = tokenService.generateRefreshToken(user);
        
        // 计算过期时间（秒）
        Long expiresIn = request.getRememberMe() ? 30L * 24 * 60 * 60 : 24L * 60 * 60;
        
        // 更新最后登录时间
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
        
        // 记录登录成功日志
        systemLogService.logLoginSuccess(user.getId(), ipAddress, userAgent);
        
        log.info("User logged in successfully: {}", user.getUsername());
        
        // 构建用户DTO
        UserDTO userDTO = UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .avatarUrl(user.getAvatarUrl())
                .isEmailVerified(user.getIsEmailVerified())
                .createdAt(user.getCreatedAt())
                .build();
        
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(userDTO)
                .expiresIn(expiresIn)
                .build();
    }
    
    @Override
    @Transactional
    public void logout(String token) {
        log.info("Processing logout");
        
        // 将令牌加入黑名单
        tokenService.invalidateToken(token);
        
        log.info("User logged out successfully");
    }
    
    @Override
    @Transactional
    public TokenResponse refreshToken(String refreshToken) {
        log.info("Processing token refresh");
        
        try {
            // 验证刷新令牌
            Claims claims = tokenService.validateToken(refreshToken);
            Long userId = claims.get("userId", Long.class);
            
            // 检查令牌是否在黑名单中
            if (tokenService.isTokenBlacklisted(refreshToken)) {
                throw new AuthenticationException(
                    "令牌已失效",
                    "TOKEN_BLACKLISTED"
                );
            }
            
            // 查找用户
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new AuthenticationException(
                        "用户不存在",
                        "USER_NOT_FOUND"
                    ));
            
            // 生成新的访问令牌
            String newAccessToken = tokenService.generateAccessToken(user, false);
            String newRefreshToken = tokenService.generateRefreshToken(user);
            
            // 将旧的刷新令牌加入黑名单
            tokenService.invalidateToken(refreshToken);
            
            log.info("Token refreshed successfully for user: {}", user.getUsername());
            
            return TokenResponse.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .expiresIn(24L * 60 * 60) // 24小时
                    .build();
        } catch (Exception e) {
            log.error("Token refresh failed", e);
            throw new AuthenticationException(
                "令牌刷新失败",
                "TOKEN_REFRESH_FAILED",
                e
            );
        }
    }
    
    @Override
    @Transactional
    public void requestPasswordReset(String email) {
        log.info("Processing password reset request for email: {}", email);
        
        // 查找用户
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ValidationException(
                    "该邮箱未注册",
                    "USER_NOT_FOUND"
                ));
        
        // 生成重置令牌（使用UUID确保唯一性和安全性）
        String resetToken = UUID.randomUUID().toString();
        
        // 设置过期时间为1小时后
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);
        
        // 创建密码重置令牌记录
        PasswordResetToken passwordResetToken = PasswordResetToken.builder()
                .userId(user.getId())
                .token(resetToken)
                .expiresAt(expiresAt)
                .used(false)
                .build();
        
        passwordResetTokenRepository.save(passwordResetToken);
        log.info("Password reset token created for user: {}", user.getUsername());
        
        // 发送密码重置邮件
        emailService.sendPasswordResetLink(email, resetToken);
        log.info("Password reset email sent to: {}", email);
    }
    
    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {
        log.info("Processing password reset with token");
        
        // 验证新密码复杂度
        if (!passwordService.isPasswordValid(newPassword)) {
            throw new ValidationException(
                "密码必须至少8个字符，并包含字母和数字",
                "PASSWORD_TOO_WEAK"
            );
        }
        
        // 查找重置令牌
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new ValidationException(
                    "无效的重置令牌",
                    "INVALID_RESET_TOKEN"
                ));
        
        // 检查令牌是否已使用
        if (resetToken.getUsed()) {
            throw new ValidationException(
                "重置令牌已被使用",
                "TOKEN_ALREADY_USED"
            );
        }
        
        // 检查令牌是否过期（超过1小时）
        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ValidationException(
                "重置令牌已过期，请重新请求密码重置",
                "TOKEN_EXPIRED"
            );
        }
        
        // 查找用户
        User user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new ValidationException(
                    "用户不存在",
                    "USER_NOT_FOUND"
                ));
        
        // 加密新密码
        String encodedPassword = passwordService.encodePassword(newPassword);
        
        // 更新用户密码
        user.setPasswordHash(encodedPassword);
        userRepository.save(user);
        log.info("Password updated successfully for user: {}", user.getUsername());
        
        // 标记令牌为已使用
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
        
        // 使所有该用户的现有JWT令牌失效
        // 通过更新密码更改时间，JWT验证时会检查令牌签发时间是否早于此时间
        user.setPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);
        
        // 标记所有未使用的重置令牌为已使用
        passwordResetTokenRepository.markAllTokensAsUsedByUserId(user.getId());
        
        // 发送密码更改通知邮件
        emailService.sendPasswordChangedNotification(user.getEmail());
        log.info("Password reset completed for user: {}", user.getUsername());
    }
}
