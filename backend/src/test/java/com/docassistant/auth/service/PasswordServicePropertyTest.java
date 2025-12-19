package com.docassistant.auth.service;

import com.docassistant.auth.service.impl.PasswordServiceImpl;
import net.jqwik.api.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 密码服务属性测试
 * 使用jqwik进行基于属性的测试
 */
class PasswordServicePropertyTest {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final PasswordService passwordService = new PasswordServiceImpl(passwordEncoder);

    /**
     * Feature: user-authentication-system, Property 19: BCrypt密码加密
     * 验证需求：7.1
     * 
     * 属性：对于任何存储的密码，数据库中应该存储BCrypt加密后的哈希值，而不是明文密码
     * 
     * 测试验证：
     * 1. 存储的不是明文密码
     * 2. 是BCrypt格式（以$2a$或$2b$开头）
     * 3. 可以使用BCrypt验证
     */
    @Property(tries = 50)
    void passwordsAreStoredAsBCryptHash(@ForAll("validPasswords") String password) {
        
        // 使用密码服务加密密码
        String encodedPassword = passwordService.encodePassword(password);
        
        // 验证1: 存储的不是明文密码
        assertThat(encodedPassword)
                .as("密码哈希不应该等于明文密码")
                .isNotEqualTo(password);
        
        // 验证2: 是BCrypt格式（以$2a$或$2b$开头，总长度60字符）
        assertThat(encodedPassword)
                .as("密码应该是BCrypt格式")
                .matches("^\\$2[ab]\\$\\d{2}\\$.{53}$");
        
        // 验证3: 可以使用BCrypt验证
        assertThat(passwordEncoder.matches(password, encodedPassword))
                .as("BCrypt应该能够验证原始密码")
                .isTrue();
        
        // 额外验证: 错误的密码不应该匹配
        assertThat(passwordEncoder.matches(password + "wrong", encodedPassword))
                .as("错误的密码不应该通过验证")
                .isFalse();
    }

    /**
     * Feature: user-authentication-system, Property 20: BCrypt密码验证
     * 验证需求：7.2
     * 
     * 属性：对于任何密码验证请求，系统应该使用BCrypt算法比对输入密码和存储的哈希值
     * 
     * 测试验证：
     * 1. 正确的密码应该验证成功
     * 2. 错误的密码应该验证失败
     * 3. 验证过程使用BCrypt算法
     */
    @Property(tries = 100)
    void bcryptPasswordVerificationWorks(@ForAll("validPasswords") String password) {
        
        // 首先加密密码
        String encodedPassword = passwordService.encodePassword(password);
        
        // 验证1: 正确的密码应该验证成功
        assertThat(passwordService.verifyPassword(password, encodedPassword))
                .as("正确的密码应该通过验证")
                .isTrue();
        
        // 验证2: 错误的密码应该验证失败
        String wrongPassword = password + "wrong";
        assertThat(passwordService.verifyPassword(wrongPassword, encodedPassword))
                .as("错误的密码不应该通过验证")
                .isFalse();
        
        // 验证3: 空密码应该验证失败
        assertThat(passwordService.verifyPassword("", encodedPassword))
                .as("空密码不应该通过验证")
                .isFalse();
        
        // 验证4: null密码应该验证失败
        assertThat(passwordService.verifyPassword(null, encodedPassword))
                .as("null密码不应该通过验证")
                .isFalse();
        
        // 验证5: 完全不同的密码应该验证失败
        assertThat(passwordService.verifyPassword("CompletelyDifferent123", encodedPassword))
                .as("完全不同的密码不应该通过验证")
                .isFalse();
    }

    /**
     * 生成有效的密码
     * 密码要求：至少8个字符，包含字母和数字
     */
    @Provide
    Arbitrary<String> validPasswords() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(4)
                .ofMaxLength(50)
                .map(letters -> {
                    // 确保包含字母和数字
                    int digits = Arbitraries.integers()
                            .between(1000, 9999)
                            .sample();
                    return letters + digits;
                });
    }
}
