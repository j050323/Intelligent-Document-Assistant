package com.docassistant.auth.security;

import com.docassistant.auth.config.JwtProperties;
import com.docassistant.auth.entity.User;
import com.docassistant.auth.entity.UserRole;
import com.docassistant.auth.service.TokenService;
import com.docassistant.auth.service.impl.TokenServiceImpl;
import io.jsonwebtoken.Claims;
import net.jqwik.api.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDateTime;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 权限控制属性测试
 * 使用jqwik进行基于属性的测试
 */
class PermissionControlPropertyTest {

    private final JwtProperties jwtProperties;
    private final TokenService tokenService;

    @SuppressWarnings("unchecked")
    public PermissionControlPropertyTest() {
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
     * Feature: user-authentication-system, Property 14: 权限控制正确性
     * 验证需求：4.3, 4.4
     * 
     * 属性：对于任何受保护的管理员功能，普通用户访问应该被拒绝，管理员访问应该被允许
     * 
     * 测试验证：
     * 1. 普通用户的JWT令牌不应该包含ADMINISTRATOR角色
     * 2. 管理员的JWT令牌应该包含ADMINISTRATOR角色
     * 3. 从令牌中提取的角色信息应该正确映射到Spring Security权限
     * 4. 普通用户的权限不应该包含ROLE_ADMINISTRATOR
     * 5. 管理员的权限应该包含ROLE_ADMINISTRATOR
     */
    @Property(tries = 100)
    void regularUsersCannotAccessAdminFunctions(@ForAll("regularUsers") User regularUser) {
        
        // 生成普通用户的JWT令牌
        String token = tokenService.generateAccessToken(regularUser, false);
        
        // 验证令牌不为空
        assertThat(token)
                .as("生成的令牌不应该为空")
                .isNotNull()
                .isNotEmpty();
        
        // 解析令牌
        Claims claims = tokenService.validateToken(token);
        
        // 验证1: 普通用户的令牌应该包含REGULAR_USER角色
        String role = claims.get("role", String.class);
        assertThat(role)
                .as("普通用户的令牌应该包含REGULAR_USER角色")
                .isEqualTo("REGULAR_USER");
        
        // 验证2: 普通用户的令牌不应该包含ADMINISTRATOR角色
        assertThat(role)
                .as("普通用户的令牌不应该包含ADMINISTRATOR角色")
                .isNotEqualTo("ADMINISTRATOR");
        
        // 验证3: 模拟Spring Security权限检查
        // 根据JWT过滤器的逻辑，角色会被转换为"ROLE_"前缀的权限
        SimpleGrantedAuthority userAuthority = new SimpleGrantedAuthority("ROLE_" + role);
        
        assertThat(userAuthority.getAuthority())
                .as("普通用户的权限应该是ROLE_REGULAR_USER")
                .isEqualTo("ROLE_REGULAR_USER");
        
        // 验证4: 普通用户不应该有管理员权限
        assertThat(userAuthority.getAuthority())
                .as("普通用户不应该有ROLE_ADMINISTRATOR权限")
                .isNotEqualTo("ROLE_ADMINISTRATOR");
        
        // 验证5: 检查权限字符串是否匹配管理员角色
        boolean hasAdminRole = userAuthority.getAuthority().equals("ROLE_ADMINISTRATOR");
        assertThat(hasAdminRole)
                .as("普通用户不应该被识别为管理员")
                .isFalse();
    }

    /**
     * Feature: user-authentication-system, Property 14: 权限控制正确性
     * 验证需求：4.3, 4.4
     * 
     * 属性：对于任何受保护的管理员功能，管理员访问应该被允许
     * 
     * 测试验证：
     * 1. 管理员的JWT令牌应该包含ADMINISTRATOR角色
     * 2. 管理员的JWT令牌不应该包含REGULAR_USER角色
     * 3. 从令牌中提取的角色信息应该正确映射到Spring Security权限
     * 4. 管理员的权限应该包含ROLE_ADMINISTRATOR
     * 5. 管理员应该被正确识别为具有管理员权限
     */
    @Property(tries = 100)
    void administratorsCanAccessAdminFunctions(@ForAll("administrators") User admin) {
        
        // 生成管理员的JWT令牌
        String token = tokenService.generateAccessToken(admin, false);
        
        // 验证令牌不为空
        assertThat(token)
                .as("生成的令牌不应该为空")
                .isNotNull()
                .isNotEmpty();
        
        // 解析令牌
        Claims claims = tokenService.validateToken(token);
        
        // 验证1: 管理员的令牌应该包含ADMINISTRATOR角色
        String role = claims.get("role", String.class);
        assertThat(role)
                .as("管理员的令牌应该包含ADMINISTRATOR角色")
                .isEqualTo("ADMINISTRATOR");
        
        // 验证2: 管理员的令牌不应该包含REGULAR_USER角色
        assertThat(role)
                .as("管理员的令牌不应该包含REGULAR_USER角色")
                .isNotEqualTo("REGULAR_USER");
        
        // 验证3: 模拟Spring Security权限检查
        // 根据JWT过滤器的逻辑，角色会被转换为"ROLE_"前缀的权限
        SimpleGrantedAuthority adminAuthority = new SimpleGrantedAuthority("ROLE_" + role);
        
        assertThat(adminAuthority.getAuthority())
                .as("管理员的权限应该是ROLE_ADMINISTRATOR")
                .isEqualTo("ROLE_ADMINISTRATOR");
        
        // 验证4: 管理员不应该有普通用户权限（应该是管理员权限）
        assertThat(adminAuthority.getAuthority())
                .as("管理员不应该有ROLE_REGULAR_USER权限")
                .isNotEqualTo("ROLE_REGULAR_USER");
        
        // 验证5: 检查权限字符串是否匹配管理员角色
        boolean hasAdminRole = adminAuthority.getAuthority().equals("ROLE_ADMINISTRATOR");
        assertThat(hasAdminRole)
                .as("管理员应该被识别为具有管理员权限")
                .isTrue();
    }

    /**
     * Feature: user-authentication-system, Property 14: 权限控制正确性
     * 验证需求：4.3, 4.4
     * 
     * 属性：对于任何用户，其JWT令牌中的角色应该与用户实体中的角色一致
     * 
     * 测试验证：
     * 1. 令牌中的角色应该与用户对象的角色匹配
     * 2. 角色信息在令牌生成和解析过程中应该保持一致
     * 3. 不同角色的用户应该生成不同的权限
     */
    @Property(tries = 100)
    void tokenRoleMatchesUserRole(@ForAll("allUsers") User user) {
        
        // 生成JWT令牌
        String token = tokenService.generateAccessToken(user, false);
        
        // 解析令牌
        Claims claims = tokenService.validateToken(token);
        String tokenRole = claims.get("role", String.class);
        
        // 验证1: 令牌中的角色应该与用户对象的角色匹配
        assertThat(tokenRole)
                .as("令牌中的角色应该与用户实体中的角色一致")
                .isEqualTo(user.getRole().name());
        
        // 验证2: 根据用户角色，验证生成的Spring Security权限是否正确
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + tokenRole);
        String expectedAuthority = "ROLE_" + user.getRole().name();
        
        assertThat(authority.getAuthority())
                .as("Spring Security权限应该与用户角色对应")
                .isEqualTo(expectedAuthority);
        
        // 验证3: 验证角色类型的正确性
        if (user.getRole() == UserRole.REGULAR_USER) {
            assertThat(authority.getAuthority())
                    .as("普通用户应该有ROLE_REGULAR_USER权限")
                    .isEqualTo("ROLE_REGULAR_USER");
        } else if (user.getRole() == UserRole.ADMINISTRATOR) {
            assertThat(authority.getAuthority())
                    .as("管理员应该有ROLE_ADMINISTRATOR权限")
                    .isEqualTo("ROLE_ADMINISTRATOR");
        }
    }

    /**
     * Feature: user-authentication-system, Property 14: 权限控制正确性
     * 验证需求：4.3, 4.4
     * 
     * 属性：对于任何两个不同角色的用户，他们的权限应该不同
     * 
     * 测试验证：
     * 1. 普通用户和管理员的令牌应该包含不同的角色信息
     * 2. 普通用户和管理员的Spring Security权限应该不同
     * 3. 权限检查应该能够区分不同角色的用户
     */
    @Property(tries = 100)
    void differentRolesHaveDifferentPermissions(
            @ForAll("regularUsers") User regularUser,
            @ForAll("administrators") User admin) {
        
        // 生成两个用户的JWT令牌
        String regularToken = tokenService.generateAccessToken(regularUser, false);
        String adminToken = tokenService.generateAccessToken(admin, false);
        
        // 解析令牌
        Claims regularClaims = tokenService.validateToken(regularToken);
        Claims adminClaims = tokenService.validateToken(adminToken);
        
        String regularRole = regularClaims.get("role", String.class);
        String adminRole = adminClaims.get("role", String.class);
        
        // 验证1: 两个用户的角色应该不同
        assertThat(regularRole)
                .as("普通用户和管理员的角色应该不同")
                .isNotEqualTo(adminRole);
        
        // 验证2: 生成的Spring Security权限应该不同
        SimpleGrantedAuthority regularAuthority = new SimpleGrantedAuthority("ROLE_" + regularRole);
        SimpleGrantedAuthority adminAuthority = new SimpleGrantedAuthority("ROLE_" + adminRole);
        
        assertThat(regularAuthority.getAuthority())
                .as("普通用户和管理员的权限应该不同")
                .isNotEqualTo(adminAuthority.getAuthority());
        
        // 验证3: 验证具体的权限值
        assertThat(regularAuthority.getAuthority())
                .as("普通用户应该有ROLE_REGULAR_USER权限")
                .isEqualTo("ROLE_REGULAR_USER");
        
        assertThat(adminAuthority.getAuthority())
                .as("管理员应该有ROLE_ADMINISTRATOR权限")
                .isEqualTo("ROLE_ADMINISTRATOR");
        
        // 验证4: 模拟权限检查
        boolean regularHasAdminRole = regularAuthority.getAuthority().equals("ROLE_ADMINISTRATOR");
        boolean adminHasAdminRole = adminAuthority.getAuthority().equals("ROLE_ADMINISTRATOR");
        
        assertThat(regularHasAdminRole)
                .as("普通用户不应该被识别为管理员")
                .isFalse();
        
        assertThat(adminHasAdminRole)
                .as("管理员应该被识别为管理员")
                .isTrue();
    }

    /**
     * 生成普通用户对象
     */
    @Provide
    Arbitrary<User> regularUsers() {
        return generateUsers(UserRole.REGULAR_USER);
    }

    /**
     * 生成管理员用户对象
     */
    @Provide
    Arbitrary<User> administrators() {
        return generateUsers(UserRole.ADMINISTRATOR);
    }

    /**
     * 生成所有类型的用户对象
     */
    @Provide
    Arbitrary<User> allUsers() {
        Arbitrary<UserRole> roles = Arbitraries.of(UserRole.REGULAR_USER, UserRole.ADMINISTRATOR);
        return roles.flatMap(this::generateUsers);
    }

    /**
     * 生成指定角色的用户对象
     */
    private Arbitrary<User> generateUsers(UserRole role) {
        Arbitrary<Long> ids = Arbitraries.longs().between(1L, 1000000L);
        Arbitrary<String> usernames = Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(3)
                .ofMaxLength(20);
        Arbitrary<String> emails = usernames.map(name -> name + "@example.com");
        
        return Combinators.combine(ids, usernames, emails)
                .as((id, username, email) -> User.builder()
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
