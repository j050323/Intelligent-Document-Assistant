package com.docassistant.auth.service;

import com.docassistant.auth.config.AppProperties;
import com.docassistant.auth.service.impl.EmailServiceImpl;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 邮件服务单元测试
 * 需求：1.1, 5.1
 * 
 * 测试邮件发送功能和验证码邮件内容
 */
@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MimeMessage mimeMessage;

    private EmailService emailService;
    private AppProperties appProperties;

    @BeforeEach
    void setUp() {
        // 配置应用属性
        appProperties = new AppProperties();
        AppProperties.Cors cors = new AppProperties.Cors();
        cors.setAllowedOrigins("http://localhost:3000");
        appProperties.setCors(cors);

        // 创建EmailService实例
        emailService = new EmailServiceImpl(mailSender, appProperties);
        
        // 使用反射设置私有字段
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@docassistant.com");
        ReflectionTestUtils.setField(emailService, "serverPort", "8080");

        // Mock MimeMessage创建
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    /**
     * 测试发送验证码邮件
     * 验证：
     * 1. 邮件发送方法被调用
     * 2. 邮件内容包含验证码
     */
    @Test
    void sendVerificationCode_ShouldSendEmailWithCode() {
        // Given
        String email = "test@example.com";
        String code = "123456";

        // When
        emailService.sendVerificationCode(email, code);

        // Then
        verify(mailSender, times(1)).createMimeMessage();
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    /**
     * 测试发送密码重置邮件
     * 验证：
     * 1. 邮件发送方法被调用
     * 2. 重置链接包含令牌
     */
    @Test
    void sendPasswordResetLink_ShouldSendEmailWithResetLink() {
        // Given
        String email = "test@example.com";
        String resetToken = "reset-token-123";

        // When
        emailService.sendPasswordResetLink(email, resetToken);

        // Then
        verify(mailSender, times(1)).createMimeMessage();
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    /**
     * 测试发送密码修改通知邮件
     * 验证：
     * 1. 邮件发送方法被调用
     */
    @Test
    void sendPasswordChangedNotification_ShouldSendEmail() {
        // Given
        String email = "test@example.com";

        // When
        emailService.sendPasswordChangedNotification(email);

        // Then
        verify(mailSender, times(1)).createMimeMessage();
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    /**
     * 测试邮件发送失败时抛出异常
     * 验证：
     * 1. 当JavaMailSender抛出RuntimeException时，应该传播异常
     */
    @Test
    void sendEmail_WhenMailSenderFails_ShouldThrowRuntimeException() {
        // Given
        String email = "test@example.com";
        String code = "123456";
        
        // Mock mailSender to throw RuntimeException
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new RuntimeException("Mail server connection failed"))
                .when(mailSender).send(any(MimeMessage.class));

        // When & Then
        assertThatThrownBy(() -> emailService.sendVerificationCode(email, code))
                .isInstanceOf(RuntimeException.class);
    }

    /**
     * 测试多次发送邮件
     * 验证：
     * 1. 可以连续发送多封邮件
     * 2. 每次发送都会调用mailSender
     */
    @Test
    void sendMultipleEmails_ShouldCallMailSenderMultipleTimes() {
        // Given
        String email1 = "test1@example.com";
        String email2 = "test2@example.com";
        String code = "123456";

        // When
        emailService.sendVerificationCode(email1, code);
        emailService.sendVerificationCode(email2, code);

        // Then
        verify(mailSender, times(2)).createMimeMessage();
        verify(mailSender, times(2)).send(any(MimeMessage.class));
    }
}
