package com.docassistant.auth.service;

import com.docassistant.auth.entity.PasswordResetToken;
import com.docassistant.auth.entity.User;
import com.docassistant.auth.entity.UserRole;
import com.docassistant.auth.exception.ValidationException;
import com.docassistant.auth.repository.PasswordResetTokenRepository;
import com.docassistant.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;

/**
 * 密码重置功能测试
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PasswordResetTest {
    
    @Autowired
    private AuthService authService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;
    
    @Autowired
    private PasswordService passwordService;
    
    @MockBean
    private EmailService emailService;
    
    private User testUser;
    
    @BeforeEach
    void setUp() {
        // Mock email service
        doNothing().when(emailService).sendPasswordChangedNotification(anyString());
        
        // 创建测试用户
        testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .passwordHash(passwordService.encodePassword("OldPassword123"))
                .role(UserRole.REGULAR_USER)
                .isEmailVerified(true)
                .build();
        testUser = userRepository.save(testUser);
    }
    
    @Test
    void testResetPassword_Success() {
        // 创建重置令牌
        String token = "test-reset-token";
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .userId(testUser.getId())
                .token(token)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .used(false)
                .build();
        passwordResetTokenRepository.save(resetToken);
        
        // 执行密码重置
        String newPassword = "NewPassword456";
        authService.resetPassword(token, newPassword);
        
        // 验证密码已更新
        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(passwordService.verifyPassword(newPassword, updatedUser.getPasswordHash())).isTrue();
        assertThat(passwordService.verifyPassword("OldPassword123", updatedUser.getPasswordHash())).isFalse();
        
        // 验证令牌已标记为已使用
        PasswordResetToken usedToken = passwordResetTokenRepository.findByToken(token).orElseThrow();
        assertThat(usedToken.getUsed()).isTrue();
        
        // 验证密码更改时间已设置
        assertThat(updatedUser.getPasswordChangedAt()).isNotNull();
    }
    
    @Test
    void testResetPassword_InvalidToken() {
        // 尝试使用不存在的令牌
        assertThatThrownBy(() -> authService.resetPassword("invalid-token", "NewPassword456"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("无效的重置令牌");
    }
    
    @Test
    void testResetPassword_ExpiredToken() {
        // 创建已过期的重置令牌
        String token = "expired-token";
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .userId(testUser.getId())
                .token(token)
                .expiresAt(LocalDateTime.now().minusHours(2)) // 2小时前过期
                .used(false)
                .build();
        passwordResetTokenRepository.save(resetToken);
        
        // 尝试使用过期令牌
        assertThatThrownBy(() -> authService.resetPassword(token, "NewPassword456"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("重置令牌已过期");
    }
    
    @Test
    void testResetPassword_UsedToken() {
        // 创建已使用的重置令牌
        String token = "used-token";
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .userId(testUser.getId())
                .token(token)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .used(true) // 已使用
                .build();
        passwordResetTokenRepository.save(resetToken);
        
        // 尝试使用已使用的令牌
        assertThatThrownBy(() -> authService.resetPassword(token, "NewPassword456"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("重置令牌已被使用");
    }
    
    @Test
    void testResetPassword_WeakPassword() {
        // 创建重置令牌
        String token = "test-token";
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .userId(testUser.getId())
                .token(token)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .used(false)
                .build();
        passwordResetTokenRepository.save(resetToken);
        
        // 尝试使用弱密码
        assertThatThrownBy(() -> authService.resetPassword(token, "weak"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("密码必须至少8个字符");
    }
    
    @Test
    void testResetPassword_SetsPasswordChangedAt() {
        // 创建重置令牌
        String token = "test-token";
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .userId(testUser.getId())
                .token(token)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .used(false)
                .build();
        passwordResetTokenRepository.save(resetToken);
        
        // 验证密码更改时间初始为null
        assertThat(testUser.getPasswordChangedAt()).isNull();
        
        // 执行密码重置
        authService.resetPassword(token, "NewPassword456");
        
        // 验证密码更改时间已设置
        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(updatedUser.getPasswordChangedAt()).isNotNull();
        assertThat(updatedUser.getPasswordChangedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }
}
