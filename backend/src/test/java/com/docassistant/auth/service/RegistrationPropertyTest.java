package com.docassistant.auth.service;

import com.docassistant.auth.dto.RegisterRequest;
import com.docassistant.auth.dto.RegisterResponse;
import com.docassistant.auth.entity.User;
import com.docassistant.auth.entity.UserRole;
import com.docassistant.auth.repository.UserRepository;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 用户注册属性测试
 * 使用jqwik的生成器进行基于属性的测试
 * 
 * Feature: user-authentication-system, Property 1: 注册成功创建用户
 * 验证需求：1.1, 1.2
 */
@SpringBootTest(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration"
})
@ActiveProfiles("test")
class RegistrationPropertyTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;
    
    @org.springframework.boot.test.mock.mockito.MockBean
    private org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplateObject;
    
    @org.springframework.boot.test.mock.mockito.MockBean
    private org.springframework.data.redis.core.RedisTemplate<String, String> redisTemplateString;
    
    @org.springframework.boot.test.mock.mockito.MockBean
    private org.springframework.data.redis.connection.RedisConnectionFactory redisConnectionFactory;
    
    @org.springframework.boot.test.mock.mockito.MockBean
    private org.springframework.mail.javamail.JavaMailSender mailSender;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        // Configure Redis mock to return a mocked ValueOperations
        @SuppressWarnings("unchecked")
        org.springframework.data.redis.core.ValueOperations<String, Object> valueOps = 
                org.mockito.Mockito.mock(org.springframework.data.redis.core.ValueOperations.class);
        org.mockito.Mockito.when(redisTemplateObject.opsForValue()).thenReturn(valueOps);
        
        // Configure JavaMailSender mock to return a mocked MimeMessage
        jakarta.mail.internet.MimeMessage mimeMessage = 
                org.mockito.Mockito.mock(jakarta.mail.internet.MimeMessage.class);
        org.mockito.Mockito.when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    /**
     * 属性：对于任何有效的注册请求（包含有效邮箱/用户名和符合要求的密码），
     *       系统应该成功创建用户账户并发送验证码
     * 
     * 测试验证：
     * 1. 注册请求应该成功返回RegisterResponse
     * 2. 返回的响应应该包含用户ID
     * 3. 用户应该被保存到数据库
     * 4. 保存的用户信息应该与请求匹配
     * 5. 用户角色应该默认为REGULAR_USER
     * 6. 邮箱验证状态应该为false（待验证）
     * 7. 密码应该被加密存储（不是明文）
     */
    @Test
    void registrationCreatesUserForValidInput() {
        // 生成器定义
        Arbitrary<String> usernameGen = Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(3)
                .ofMaxLength(20)
                .map(letters -> letters + Arbitraries.integers().between(1, 999).sample());
        
        Arbitrary<String> emailGen = Combinators.combine(
                Arbitraries.strings().withCharRange('a', 'z').ofMinLength(3).ofMaxLength(15),
                Arbitraries.of("example.com", "test.com", "mail.com", "email.com")
        ).as((local, domain) -> local + "@" + domain);
        
        Arbitrary<String> passwordGen = Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(4)
                .ofMaxLength(50)
                .map(letters -> letters + Arbitraries.integers().between(10000, 99999).sample());
        
        // 运行100次测试
        for (int i = 0; i < 100; i++) {
            // 清理数据库
            userRepository.deleteAll();
            
            // 生成随机测试数据
            String username = usernameGen.sample();
            String email = emailGen.sample();
            String password = passwordGen.sample();
            
            // 构建注册请求
            RegisterRequest request = RegisterRequest.builder()
                    .username(username)
                    .email(email)
                    .password(password)
                    .build();
            
            // 执行注册
            RegisterResponse response = authService.register(request);
            
            // 验证1: 注册请求应该成功返回RegisterResponse
            assertThat(response)
                    .as("注册应该返回非空的RegisterResponse (iteration %d)", i)
                    .isNotNull();
            
            // 验证2: 返回的响应应该包含用户ID
            assertThat(response.getUserId())
                    .as("注册响应应该包含用户ID (iteration %d)", i)
                    .isNotNull()
                    .isPositive();
            
            // 验证3: 返回的响应应该包含用户名和邮箱
            assertThat(response.getUsername())
                    .as("注册响应应该包含用户名 (iteration %d)", i)
                    .isEqualTo(username);
            
            assertThat(response.getEmail())
                    .as("注册响应应该包含邮箱 (iteration %d)", i)
                    .isEqualTo(email);
            
            // 验证4: 用户应该被保存到数据库
            Optional<User> savedUserOpt = userRepository.findById(response.getUserId());
            assertThat(savedUserOpt)
                    .as("用户应该被保存到数据库 (iteration %d)", i)
                    .isPresent();
            
            User savedUser = savedUserOpt.get();
            
            // 验证5: 保存的用户信息应该与请求匹配
            assertThat(savedUser.getUsername())
                    .as("保存的用户名应该与请求匹配 (iteration %d)", i)
                    .isEqualTo(username);
            
            assertThat(savedUser.getEmail())
                    .as("保存的邮箱应该与请求匹配 (iteration %d)", i)
                    .isEqualTo(email);
            
            // 验证6: 用户角色应该默认为REGULAR_USER
            assertThat(savedUser.getRole())
                    .as("新用户的角色应该默认为REGULAR_USER (iteration %d)", i)
                    .isEqualTo(UserRole.REGULAR_USER);
            
            // 验证7: 邮箱验证状态应该为false（待验证）
            assertThat(savedUser.getIsEmailVerified())
                    .as("新用户的邮箱验证状态应该为false (iteration %d)", i)
                    .isFalse();
            
            // 验证8: 密码应该被加密存储（不是明文）
            assertThat(savedUser.getPasswordHash())
                    .as("密码应该被加密存储 (iteration %d)", i)
                    .isNotNull()
                    .isNotEmpty()
                    .isNotEqualTo(password);
            
            // 验证9: 密码哈希应该是BCrypt格式
            assertThat(savedUser.getPasswordHash())
                    .as("密码应该使用BCrypt格式存储 (iteration %d)", i)
                    .matches("^\\$2[ab]\\$\\d{2}\\$.{53}$");
            
            // 验证10: 用户应该有创建时间
            assertThat(savedUser.getCreatedAt())
                    .as("用户应该有创建时间 (iteration %d)", i)
                    .isNotNull();
            
            // 验证11: 可以通过邮箱查询到用户
            Optional<User> userByEmail = userRepository.findByEmail(email);
            assertThat(userByEmail)
                    .as("应该能够通过邮箱查询到用户 (iteration %d)", i)
                    .isPresent()
                    .get()
                    .extracting(User::getId)
                    .isEqualTo(savedUser.getId());
            
            // 验证12: 可以通过用户名查询到用户
            Optional<User> userByUsername = userRepository.findByUsername(username);
            assertThat(userByUsername)
                    .as("应该能够通过用户名查询到用户 (iteration %d)", i)
                    .isPresent()
                    .get()
                    .extracting(User::getId)
                    .isEqualTo(savedUser.getId());
        }
    }
}
