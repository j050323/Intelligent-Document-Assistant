package com.docassistant.auth.service.impl;

import com.docassistant.auth.config.AppProperties;
import com.docassistant.auth.service.VerificationCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

/**
 * Verification code service implementation.
 * Generates 6-digit verification codes and stores them in Redis with TTL.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationCodeServiceImpl implements VerificationCodeService {
    
    private static final String VERIFICATION_CODE_PREFIX = "verification_code:";
    private static final int CODE_LENGTH = 6;
    private static final SecureRandom RANDOM = new SecureRandom();
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final AppProperties appProperties;
    
    @Override
    public String generateAndStoreCode(String email) {
        log.info("Generating verification code for email: {}", email);
        
        // Generate 6-digit code
        String code = generateSixDigitCode();
        
        // Store in Redis with TTL
        String key = buildRedisKey(email);
        Integer expiration = appProperties.getVerificationCode().getExpiration();
        
        redisTemplate.opsForValue().set(key, code, expiration, TimeUnit.SECONDS);
        
        log.info("Verification code stored in Redis for email: {} with TTL: {} seconds", email, expiration);
        
        return code;
    }
    
    @Override
    public boolean validateCode(String email, String code) {
        log.info("Validating verification code for email: {}", email);
        
        if (email == null || code == null) {
            log.warn("Email or code is null");
            return false;
        }
        
        String key = buildRedisKey(email);
        Object storedCode = redisTemplate.opsForValue().get(key);
        
        if (storedCode == null) {
            log.warn("No verification code found for email: {}", email);
            return false;
        }
        
        boolean isValid = code.equals(storedCode.toString());
        
        if (isValid) {
            log.info("Verification code validated successfully for email: {}", email);
        } else {
            log.warn("Invalid verification code for email: {}", email);
        }
        
        return isValid;
    }
    
    @Override
    public void deleteCode(String email) {
        log.info("Deleting verification code for email: {}", email);
        
        String key = buildRedisKey(email);
        redisTemplate.delete(key);
        
        log.info("Verification code deleted for email: {}", email);
    }
    
    /**
     * Generate a random 6-digit verification code.
     * 
     * @return a 6-digit string code
     */
    private String generateSixDigitCode() {
        int code = RANDOM.nextInt(900000) + 100000; // Range: 100000 to 999999
        return String.valueOf(code);
    }
    
    /**
     * Build Redis key for verification code storage.
     * 
     * @param email the email address
     * @return the Redis key
     */
    private String buildRedisKey(String email) {
        return VERIFICATION_CODE_PREFIX + email;
    }
}
