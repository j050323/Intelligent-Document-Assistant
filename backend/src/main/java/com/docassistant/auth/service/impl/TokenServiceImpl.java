package com.docassistant.auth.service.impl;

import com.docassistant.auth.config.JwtProperties;
import com.docassistant.auth.entity.User;
import com.docassistant.auth.service.TokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * JWT令牌管理服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {
    
    private final JwtProperties jwtProperties;
    private final RedisTemplate<String, String> redisTemplate;
    
    private static final String TOKEN_BLACKLIST_PREFIX = "token_blacklist:";
    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_USERNAME = "username";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TOKEN_TYPE = "tokenType";
    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";
    
    @Override
    public String generateAccessToken(User user, boolean rememberMe) {
        long expiration = rememberMe ? 
            jwtProperties.getRememberMeExpiration() : 
            jwtProperties.getAccessTokenExpiration();
        
        return generateToken(user, expiration, TOKEN_TYPE_ACCESS);
    }
    
    @Override
    public String generateRefreshToken(User user) {
        return generateToken(user, jwtProperties.getRefreshTokenExpiration(), TOKEN_TYPE_REFRESH);
    }
    
    /**
     * 生成JWT令牌的通用方法
     */
    private String generateToken(User user, long expiration, String tokenType) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);
        
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_USER_ID, user.getId());
        claims.put(CLAIM_USERNAME, user.getUsername());
        claims.put(CLAIM_ROLE, user.getRole().name());
        claims.put(CLAIM_TOKEN_TYPE, tokenType);
        
        String jti = UUID.randomUUID().toString();
        
        return Jwts.builder()
                .claims(claims)
                .subject(user.getUsername())
                .id(jti)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }
    
    @Override
    public Claims validateToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
    
    @Override
    public void invalidateToken(String token) {
        try {
            Claims claims = validateToken(token);
            String jti = claims.getId();
            Date expiration = claims.getExpiration();
            
            if (jti != null && expiration != null) {
                long ttl = expiration.getTime() - System.currentTimeMillis();
                if (ttl > 0) {
                    String key = TOKEN_BLACKLIST_PREFIX + jti;
                    redisTemplate.opsForValue().set(key, "1", ttl, TimeUnit.MILLISECONDS);
                    log.debug("Token {} added to blacklist with TTL {} ms", jti, ttl);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to invalidate token: {}", e.getMessage());
        }
    }
    
    @Override
    public boolean isTokenBlacklisted(String token) {
        try {
            Claims claims = validateToken(token);
            String jti = claims.getId();
            
            if (jti == null) {
                return false;
            }
            
            String key = TOKEN_BLACKLIST_PREFIX + jti;
            Boolean exists = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.warn("Error checking token blacklist: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public Long getUserIdFromToken(String token) {
        Claims claims = validateToken(token);
        Object userIdObj = claims.get(CLAIM_USER_ID);
        
        if (userIdObj instanceof Integer) {
            return ((Integer) userIdObj).longValue();
        } else if (userIdObj instanceof Long) {
            return (Long) userIdObj;
        }
        
        throw new IllegalArgumentException("Invalid userId in token");
    }
    
    @Override
    public String getUsernameFromToken(String token) {
        Claims claims = validateToken(token);
        return claims.get(CLAIM_USERNAME, String.class);
    }
    
    @Override
    public boolean isTokenIssuedBeforePasswordChange(String token, java.time.LocalDateTime passwordChangedAt) {
        if (passwordChangedAt == null) {
            return false;
        }
        
        try {
            Claims claims = validateToken(token);
            Date issuedAt = claims.getIssuedAt();
            
            if (issuedAt == null) {
                return false;
            }
            
            // Convert LocalDateTime to milliseconds for comparison
            long passwordChangedMillis = passwordChangedAt
                    .atZone(java.time.ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();
            
            // Token is invalid if it was issued before password change
            return issuedAt.getTime() < passwordChangedMillis;
        } catch (Exception e) {
            log.warn("Error checking token issue time: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取JWT签名密钥
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
