package com.docassistant.auth.service.impl;

import com.docassistant.auth.config.AppProperties;
import com.docassistant.auth.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Email service implementation.
 * Handles sending verification codes, password reset links, and notifications.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {
    
    private final JavaMailSender mailSender;
    private final AppProperties appProperties;
    
    @Value("${spring.mail.username}")
    private String fromEmail;
    
    @Value("${server.port:8080}")
    private String serverPort;
    
    @Override
    public void sendVerificationCode(String email, String code) {
        log.info("Sending verification code to email: {}", email);
        
        String subject = "验证您的邮箱 - 智能文档助手";
        String htmlContent = buildVerificationCodeEmail(code);
        
        sendHtmlEmail(email, subject, htmlContent);
        
        log.info("Verification code email sent successfully to: {}", email);
    }
    
    @Override
    public void sendPasswordResetLink(String email, String resetToken) {
        log.info("Sending password reset link to email: {}", email);
        
        String subject = "重置您的密码 - 智能文档助手";
        String resetLink = buildPasswordResetLink(resetToken);
        String htmlContent = buildPasswordResetEmail(resetLink);
        
        sendHtmlEmail(email, subject, htmlContent);
        
        log.info("Password reset email sent successfully to: {}", email);
    }
    
    @Override
    public void sendPasswordChangedNotification(String email) {
        log.info("Sending password changed notification to email: {}", email);
        
        String subject = "密码已更改 - 智能文档助手";
        String htmlContent = buildPasswordChangedEmail();
        
        sendHtmlEmail(email, subject, htmlContent);
        
        log.info("Password changed notification sent successfully to: {}", email);
    }
    
    /**
     * Send HTML email using JavaMailSender.
     */
    private void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
        } catch (MessagingException e) {
            log.error("Failed to send email to: {}", to, e);
            throw new RuntimeException("邮件发送失败", e);
        }
    }
    
    /**
     * Build password reset link URL.
     */
    private String buildPasswordResetLink(String resetToken) {
        // Get the first allowed origin from CORS configuration
        String frontendUrl = appProperties.getCors().getAllowedOrigins().split(",")[0];
        return frontendUrl + "/reset-password?token=" + resetToken;
    }
    
    /**
     * Build HTML content for verification code email.
     */
    private String buildVerificationCodeEmail(String code) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #4CAF50; color: white; padding: 20px; text-align: center; }
                    .content { background-color: #f9f9f9; padding: 30px; border-radius: 5px; margin-top: 20px; }
                    .code { font-size: 32px; font-weight: bold; color: #4CAF50; text-align: center; 
                            letter-spacing: 5px; padding: 20px; background-color: #fff; 
                            border: 2px dashed #4CAF50; border-radius: 5px; margin: 20px 0; }
                    .footer { text-align: center; margin-top: 20px; color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>智能文档助手</h1>
                    </div>
                    <div class="content">
                        <h2>验证您的邮箱地址</h2>
                        <p>感谢您注册智能文档助手！</p>
                        <p>请使用以下验证码完成邮箱验证：</p>
                        <div class="code">%s</div>
                        <p>此验证码将在 <strong>10分钟</strong> 后过期。</p>
                        <p>如果您没有注册智能文档助手账户，请忽略此邮件。</p>
                    </div>
                    <div class="footer">
                        <p>此邮件由系统自动发送，请勿回复。</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(code);
    }
    
    /**
     * Build HTML content for password reset email.
     */
    private String buildPasswordResetEmail(String resetLink) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #FF9800; color: white; padding: 20px; text-align: center; }
                    .content { background-color: #f9f9f9; padding: 30px; border-radius: 5px; margin-top: 20px; }
                    .button { display: inline-block; padding: 12px 30px; background-color: #FF9800; 
                             color: white; text-decoration: none; border-radius: 5px; margin: 20px 0; }
                    .button:hover { background-color: #F57C00; }
                    .warning { background-color: #fff3cd; border-left: 4px solid #ffc107; 
                              padding: 10px; margin: 20px 0; }
                    .footer { text-align: center; margin-top: 20px; color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>智能文档助手</h1>
                    </div>
                    <div class="content">
                        <h2>重置您的密码</h2>
                        <p>我们收到了重置您账户密码的请求。</p>
                        <p>请点击下面的按钮重置密码：</p>
                        <div style="text-align: center;">
                            <a href="%s" class="button">重置密码</a>
                        </div>
                        <p>或者复制以下链接到浏览器：</p>
                        <p style="word-break: break-all; color: #666;">%s</p>
                        <div class="warning">
                            <strong>⚠️ 安全提示：</strong>
                            <ul>
                                <li>此链接将在 <strong>1小时</strong> 后过期</li>
                                <li>如果您没有请求重置密码，请忽略此邮件</li>
                                <li>请勿将此链接分享给他人</li>
                            </ul>
                        </div>
                    </div>
                    <div class="footer">
                        <p>此邮件由系统自动发送，请勿回复。</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(resetLink, resetLink);
    }
    
    /**
     * Build HTML content for password changed notification email.
     */
    private String buildPasswordChangedEmail() {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #2196F3; color: white; padding: 20px; text-align: center; }
                    .content { background-color: #f9f9f9; padding: 30px; border-radius: 5px; margin-top: 20px; }
                    .success { background-color: #d4edda; border-left: 4px solid #28a745; 
                              padding: 15px; margin: 20px 0; }
                    .warning { background-color: #fff3cd; border-left: 4px solid #ffc107; 
                              padding: 10px; margin: 20px 0; }
                    .footer { text-align: center; margin-top: 20px; color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>智能文档助手</h1>
                    </div>
                    <div class="content">
                        <h2>密码已成功更改</h2>
                        <div class="success">
                            <strong>✓ 您的密码已成功更改</strong>
                        </div>
                        <p>您的账户密码已经成功更改。如果这是您本人的操作，可以忽略此邮件。</p>
                        <div class="warning">
                            <strong>⚠️ 安全提示：</strong>
                            <p>如果您没有进行此操作，您的账户可能已被他人访问。请立即：</p>
                            <ul>
                                <li>联系我们的客服团队</li>
                                <li>检查您的账户安全设置</li>
                                <li>更改其他使用相同密码的账户</li>
                            </ul>
                        </div>
                    </div>
                    <div class="footer">
                        <p>此邮件由系统自动发送，请勿回复。</p>
                    </div>
                </div>
            </body>
            </html>
            """;
    }
}
