package com.docassistant.auth.service;

/**
 * Service interface for verification code operations.
 * Handles generation, storage, and validation of email verification codes.
 */
public interface VerificationCodeService {
    
    /**
     * Generate a 6-digit verification code and store it in Redis.
     * The code will expire after 10 minutes.
     * 
     * @param email the email address to associate with the verification code
     * @return the generated 6-digit verification code
     */
    String generateAndStoreCode(String email);
    
    /**
     * Validate a verification code for a given email.
     * 
     * @param email the email address
     * @param code the verification code to validate
     * @return true if the code is valid and matches, false otherwise
     */
    boolean validateCode(String email, String code);
    
    /**
     * Delete a verification code from Redis.
     * 
     * @param email the email address whose verification code should be deleted
     */
    void deleteCode(String email);
}
