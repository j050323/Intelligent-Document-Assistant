package com.docassistant.auth.service;

/**
 * Service interface for email operations.
 * Handles sending verification codes and password reset emails.
 */
public interface EmailService {
    
    /**
     * Send verification code to user's email address.
     * 
     * @param email the recipient email address
     * @param code the verification code to send
     */
    void sendVerificationCode(String email, String code);
    
    /**
     * Send password reset link to user's email address.
     * 
     * @param email the recipient email address
     * @param resetToken the password reset token
     */
    void sendPasswordResetLink(String email, String resetToken);
    
    /**
     * Send notification that password has been changed.
     * 
     * @param email the recipient email address
     */
    void sendPasswordChangedNotification(String email);
}
