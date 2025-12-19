package com.docassistant.auth.service;

import com.docassistant.auth.config.JwtProperties;
import com.docassistant.auth.entity.User;
import com.docassistant.auth.entity.UserRole;
import com.docassistant.auth.service.impl.TokenServiceImpl;
import io.jsonwebtoken.Claims;
import net.jqwik.api.*;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.LocalDateTime;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.mock;

/**
 * JWT令牌服务属性测试
 * 使用jqwik进行基于属性的测试
 */
class TokenServicePropertyTest {

    private final JwtProperties jwtProperties;
    private final TokenService tokenService;

    @SuppressWarnings("unchecked")
    public TokenServicePropertyTest() {
        // 配置JWT属性
        this.jwtProperties = new JwtProperties();
        this.jwtProperties.setSecret("test-secret-key-for-jwt-token-signing-must-be-at-least-256-bits-long");
        this.jwtProperties.setAccessTokenExpiration(24 * 60 * 60 * 1000L); // 24小时
        this.jwtProperties.setRefreshTokenExpiration(7 * 24 * 60 * 60 * 1000L); // 7天
        this.jwtProperties.setRememberMeExpiration(30 * 24 * 60 * 60 * 1000L); // 30天
        
        // Mock RedisTemplate
        RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);
        
        this.tokenService = new TokenServiceImpl(jwtProperties, redisTemplate);
    }

    /**
     * Feature: user-authentication-system, Property 22: JWT令牌结构完整性
     * 验证需求：8.1
     * 
     * 属性：对于任何生成的JWT令牌，应该包含用户ID、用户名、角色和过期时间信息
     * 
     * 测试验证：
     * 1. 令牌包含用户ID
     * 2. 令牌包含用户名
     * 3. 令牌包含角色信息
     * 4. 令牌包含过期时间
     * 5. 令牌包含签发时间
     * 6. 令牌包含唯一标识符(jti)
     */
    @Property(tries = 100)
    void jwtTokenContainsRequiredClaims(
            @ForAll("validUsers") User user,
            @ForAll boolean rememberMe) {
        
        // 生成访问令牌
        String token = tokenService.generateAccessToken(user, rememberMe);
        
        // 验证令牌不为空
        assertThat(token)
                .as("生成的令牌不应该为空")
                .isNotNull()
                .isNotEmpty();
        
        // 解析令牌
        Claims claims = tokenService.validateToken(token);
        
        // 验证1: 令牌包含用户ID
        assertThat(claims.get("userId"))
                .as("令牌应该包含用户ID")
                .isNotNull();
        
        Long tokenUserId = getUserIdFromClaims(claims);
        assertThat(tokenUserId)
                .as("令牌中的用户ID应该与原始用户ID匹配")
                .isEqualTo(user.getId());
        
        // 验证2: 令牌包含用户名
        assertThat(claims.get("username"))
                .as("令牌应该包含用户名")
                .isNotNull();
        
        String tokenUsername = claims.get("username", String.class);
        assertThat(tokenUsername)
                .as("令牌中的用户名应该与原始用户名匹配")
                .isEqualTo(user.getUsername());
        
        // 验证3: 令牌包含角色信息
        assertThat(claims.get("role"))
                .as("令牌应该包含角色信息")
                .isNotNull();
        
        String tokenRole = claims.get("role", String.class);
        assertThat(tokenRole)
                .as("令牌中的角色应该与原始用户角色匹配")
                .isEqualTo(user.getRole().name());
        
        // 验证4: 令牌包含过期时间
        assertThat(claims.getExpiration())
                .as("令牌应该包含过期时间")
                .isNotNull()
                .isAfter(new Date());
        
        // 验证5: 令牌包含签发时间
        assertThat(claims.getIssuedAt())
                .as("令牌应该包含签发时间")
                .isNotNull()
                .isBeforeOrEqualTo(new Date());
        
        // 验证6: 令牌包含唯一标识符(jti)
        assertThat(claims.getId())
                .as("令牌应该包含唯一标识符(jti)")
                .isNotNull()
                .isNotEmpty();
        
        // 验证7: 令牌包含subject（通常是用户名）
        assertThat(claims.getSubject())
                .as("令牌应该包含subject")
                .isNotNull()
                .isEqualTo(user.getUsername());
        
        // 验证8: 过期时间应该根据rememberMe设置正确
        long expectedExpiration = rememberMe ? 
                jwtProperties.getRememberMeExpiration() : 
                jwtProperties.getAccessTokenExpiration();
        
        long actualExpiration = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();
        
        // 允许1秒的误差
        assertThat(actualExpiration)
                .as("令牌过期时间应该根据rememberMe设置正确")
                .isCloseTo(expectedExpiration, within(1000L));
    }

    /**
     * 从Claims中提取用户ID
     * 处理Integer和Long两种类型
     */
    private Long getUserIdFromClaims(Claims claims) {
        Object userIdObj = claims.get("userId");
        if (userIdObj instanceof Integer) {
            return ((Integer) userIdObj).longValue();
        } else if (userIdObj instanceof Long) {
            return (Long) userIdObj;
        }
        throw new IllegalArgumentException("Invalid userId type in claims");
    }

    /**
     * Feature: user-authentication-system, Property 23: JWT令牌验证正确性
     * 验证需求：8.2, 8.3, 8.4
     * 
     * 属性：对于任何JWT令牌验证请求，系统应该检查签名有效性和过期时间，拒绝无效或过期的令牌
     * 
     * 测试验证：
     * 1. 有效的令牌（签名正确且未过期）应该被成功验证
     * 2. 验证后应该能够提取正确的用户信息
     * 3. 令牌的所有声明应该保持完整
     */
    @Property(tries = 100)
    void validTokensAreAccepted(
            @ForAll("validUsers") User user,
            @ForAll boolean rememberMe) {
        
        // 生成有效的访问令牌
        String token = tokenService.generateAccessToken(user, rememberMe);
        
        // 验证1: 有效的令牌应该被成功验证（不抛出异常）
        Claims claims = tokenService.validateToken(token);
        
        assertThat(claims)
                .as("有效令牌的验证应该返回Claims对象")
                .isNotNull();
        
        // 验证2: 应该能够提取正确的用户ID
        Long extractedUserId = tokenService.getUserIdFromToken(token);
        assertThat(extractedUserId)
                .as("从令牌中提取的用户ID应该与原始用户ID匹配")
                .isEqualTo(user.getId());
        
        // 验证3: 应该能够提取正确的用户名
        String extractedUsername = tokenService.getUsernameFromToken(token);
        assertThat(extractedUsername)
                .as("从令牌中提取的用户名应该与原始用户名匹配")
                .isEqualTo(user.getUsername());
        
        // 验证4: 令牌的签名应该有效（validateToken不抛出异常即表示签名有效）
        assertThat(claims.get("userId"))
                .as("验证后的令牌应该包含完整的用户ID声明")
                .isNotNull();
        
        assertThat(claims.get("username"))
                .as("验证后的令牌应该包含完整的用户名声明")
                .isNotNull();
        
        assertThat(claims.get("role"))
                .as("验证后的令牌应该包含完整的角色声明")
                .isNotNull();
        
        // 验证5: 令牌的过期时间应该在未来
        assertThat(claims.getExpiration())
                .as("有效令牌的过期时间应该在未来")
                .isAfter(new Date());
    }

    /**
     * Feature: user-authentication-system, Property 23: JWT令牌验证正确性
     * 验证需求：8.3
     * 
     * 属性：对于任何已过期的JWT令牌，系统应该拒绝该令牌并抛出异常
     * 
     * 测试验证：
     * 1. 过期的令牌应该被拒绝
     * 2. 应该抛出JWT相关的异常
     */
    @Property(tries = 100)
    void expiredTokensAreRejected(@ForAll("validUsers") User user) {
        // 创建一个已过期的JWT属性配置
        JwtProperties expiredJwtProperties = new JwtProperties();
        expiredJwtProperties.setSecret("test-secret-key-for-jwt-token-signing-must-be-at-least-256-bits-long");
        expiredJwtProperties.setAccessTokenExpiration(-1000L); // 负数表示已过期
        expiredJwtProperties.setRefreshTokenExpiration(7 * 24 * 60 * 60 * 1000L);
        expiredJwtProperties.setRememberMeExpiration(30 * 24 * 60 * 60 * 1000L);
        
        @SuppressWarnings("unchecked")
        RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);
        TokenService expiredTokenService = new TokenServiceImpl(expiredJwtProperties, redisTemplate);
        
        // 生成一个已过期的令牌
        String expiredToken = expiredTokenService.generateAccessToken(user, false);
        
        // 等待一小段时间确保令牌过期
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 验证: 过期的令牌应该被拒绝（抛出异常）
        try {
            tokenService.validateToken(expiredToken);
            throw new AssertionError("过期的令牌应该被拒绝，但验证成功了");
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            // 预期的异常，测试通过
            assertThat(e)
                    .as("过期令牌应该抛出ExpiredJwtException")
                    .isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
        } catch (Exception e) {
            throw new AssertionError("过期令牌应该抛出ExpiredJwtException，但抛出了: " + e.getClass().getName(), e);
        }
    }

    /**
     * Feature: user-authentication-system, Property 23: JWT令牌验证正确性
     * 验证需求：8.4
     * 
     * 属性：对于任何签名无效的JWT令牌，系统应该拒绝该令牌并抛出异常
     * 
     * 测试验证：
     * 1. 签名无效的令牌应该被拒绝
     * 2. 应该抛出JWT签名相关的异常
     */
    @Property(tries = 100)
    void tokensWithInvalidSignatureAreRejected(@ForAll("validUsers") User user) {
        // 使用当前服务生成有效令牌
        String validToken = tokenService.generateAccessToken(user, false);
        
        // 创建一个使用不同密钥的TokenService（这样签名就会不匹配）
        JwtProperties differentSecretProperties = new JwtProperties();
        differentSecretProperties.setSecret("different-secret-key-for-jwt-token-signing-must-be-at-least-256-bits");
        differentSecretProperties.setAccessTokenExpiration(24 * 60 * 60 * 1000L);
        differentSecretProperties.setRefreshTokenExpiration(7 * 24 * 60 * 60 * 1000L);
        differentSecretProperties.setRememberMeExpiration(30 * 24 * 60 * 60 * 1000L);
        
        @SuppressWarnings("unchecked")
        RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);
        TokenService differentSecretService = new TokenServiceImpl(differentSecretProperties, redisTemplate);
        
        // 验证: 使用不同密钥验证令牌应该失败
        try {
            differentSecretService.validateToken(validToken);
            throw new AssertionError("签名无效的令牌应该被拒绝，但验证成功了");
        } catch (io.jsonwebtoken.security.SignatureException e) {
            // 预期的异常，测试通过
            assertThat(e)
                    .as("签名无效的令牌应该抛出SignatureException")
                    .isInstanceOf(io.jsonwebtoken.security.SignatureException.class);
        } catch (io.jsonwebtoken.JwtException e) {
            // 也接受其他JWT异常（某些版本可能抛出不同的异常）
            assertThat(e)
                    .as("签名无效的令牌应该抛出JwtException")
                    .isInstanceOf(io.jsonwebtoken.JwtException.class);
        } catch (Exception e) {
            throw new AssertionError("签名无效的令牌应该抛出JwtException，但抛出了: " + e.getClass().getName(), e);
        }
    }

    /**
     * 生成有效的用户对象
     */
    @Provide
    Arbitrary<User> validUsers() {
        Arbitrary<Long> ids = Arbitraries.longs().between(1L, 1000000L);
        Arbitrary<String> usernames = Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(3)
                .ofMaxLength(20);
        Arbitrary<String> emails = usernames.map(name -> name + "@example.com");
        Arbitrary<UserRole> roles = Arbitraries.of(UserRole.REGULAR_USER, UserRole.ADMINISTRATOR);
        
        return Combinators.combine(ids, usernames, emails, roles)
                .as((id, username, email, role) -> User.builder()
                        .id(id)
                        .username(username)
                        .email(email)
                        .passwordHash("$2a$10$dummyHashForTesting")
                        .role(role)
                        .isEmailVerified(true)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build());
    }
}
