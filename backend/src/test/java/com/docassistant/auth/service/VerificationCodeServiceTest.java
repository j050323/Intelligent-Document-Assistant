package com.docassistant.auth.service;

import com.docassistant.auth.config.AppProperties;
import com.docassistant.auth.service.impl.VerificationCodeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * 验证码服务单元测试
 * 需求：1.1, 5.1
 * 
 * 测试验证码生成和存储功能
 */
@ExtendWith(MockitoExtension.class)
class VerificationCodeServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private VerificationCodeService verificationCodeService;
    private AppProperties appProperties;

    @BeforeEach
    void setUp() {
        // 配置应用属性
        appProperties = new AppProperties();
        AppProperties.VerificationCode verificationCodeConfig = new AppProperties.VerificationCode();
        verificationCodeConfig.setExpiration(600); // 10分钟
        appProperties.setVerificationCode(verificationCodeConfig);

        // Mock RedisTemplate操作 (使用lenient避免不必要的stubbing警告)
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // 创建VerificationCodeService实例
        verificationCodeService = new VerificationCodeServiceImpl(redisTemplate, appProperties);
    }

    /**
     * 测试生成验证码
     * 验证：
     * 1. 生成的验证码是6位数字
     * 2. 验证码被存储到Redis
     * 3. 设置了正确的过期时间
     */
    @Test
    void generateAndStoreCode_ShouldGenerateSixDigitCodeAndStoreInRedis() {
        // Given
        String email = "test@example.com";

        // When
        String code = verificationCodeService.generateAndStoreCode(email);

        // Then
        // 验证1: 验证码是6位数字
        assertThat(code)
                .as("验证码应该是6位数字")
                .hasSize(6)
                .matches("\\d{6}");

        // 验证2: 验证码被存储到Redis
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> valueCaptor = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<Long> timeoutCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<TimeUnit> timeUnitCaptor = ArgumentCaptor.forClass(TimeUnit.class);

        verify(valueOperations, times(1)).set(
                keyCaptor.capture(),
                valueCaptor.capture(),
                timeoutCaptor.capture(),
                timeUnitCaptor.capture()
        );

        // 验证Redis key格式
        assertThat(keyCaptor.getValue())
                .as("Redis key应该包含邮箱地址")
                .isEqualTo("verification_code:" + email);

        // 验证存储的值是生成的验证码
        assertThat(valueCaptor.getValue())
                .as("存储的值应该是生成的验证码")
                .isEqualTo(code);

        // 验证3: 设置了正确的过期时间（600秒）
        assertThat(timeoutCaptor.getValue())
                .as("过期时间应该是600秒")
                .isEqualTo(600L);

        assertThat(timeUnitCaptor.getValue())
                .as("时间单位应该是秒")
                .isEqualTo(TimeUnit.SECONDS);
    }

    /**
     * 测试验证码范围
     * 验证：
     * 1. 生成的验证码在100000到999999之间
     */
    @Test
    void generateAndStoreCode_ShouldGenerateCodeInValidRange() {
        // Given
        String email = "test@example.com";

        // When
        String code = verificationCodeService.generateAndStoreCode(email);
        int codeValue = Integer.parseInt(code);

        // Then
        assertThat(codeValue)
                .as("验证码应该在100000到999999之间")
                .isBetween(100000, 999999);
    }

    /**
     * 测试验证正确的验证码
     * 验证：
     * 1. 正确的验证码应该返回true
     */
    @Test
    void validateCode_WithCorrectCode_ShouldReturnTrue() {
        // Given
        String email = "test@example.com";
        String code = "123456";
        String redisKey = "verification_code:" + email;

        when(valueOperations.get(redisKey)).thenReturn(code);

        // When
        boolean result = verificationCodeService.validateCode(email, code);

        // Then
        assertThat(result)
                .as("正确的验证码应该返回true")
                .isTrue();

        verify(valueOperations, times(1)).get(redisKey);
    }

    /**
     * 测试验证错误的验证码
     * 验证：
     * 1. 错误的验证码应该返回false
     */
    @Test
    void validateCode_WithIncorrectCode_ShouldReturnFalse() {
        // Given
        String email = "test@example.com";
        String storedCode = "123456";
        String inputCode = "654321";
        String redisKey = "verification_code:" + email;

        when(valueOperations.get(redisKey)).thenReturn(storedCode);

        // When
        boolean result = verificationCodeService.validateCode(email, inputCode);

        // Then
        assertThat(result)
                .as("错误的验证码应该返回false")
                .isFalse();

        verify(valueOperations, times(1)).get(redisKey);
    }

    /**
     * 测试验证不存在的验证码
     * 验证：
     * 1. 当Redis中没有验证码时应该返回false
     */
    @Test
    void validateCode_WithNonExistentCode_ShouldReturnFalse() {
        // Given
        String email = "test@example.com";
        String code = "123456";
        String redisKey = "verification_code:" + email;

        when(valueOperations.get(redisKey)).thenReturn(null);

        // When
        boolean result = verificationCodeService.validateCode(email, code);

        // Then
        assertThat(result)
                .as("不存在的验证码应该返回false")
                .isFalse();

        verify(valueOperations, times(1)).get(redisKey);
    }

    /**
     * 测试验证空值
     * 验证：
     * 1. 邮箱为null时应该返回false
     * 2. 验证码为null时应该返回false
     */
    @Test
    void validateCode_WithNullValues_ShouldReturnFalse() {
        // When & Then
        assertThat(verificationCodeService.validateCode(null, "123456"))
                .as("邮箱为null时应该返回false")
                .isFalse();

        assertThat(verificationCodeService.validateCode("test@example.com", null))
                .as("验证码为null时应该返回false")
                .isFalse();

        // 验证没有调用Redis操作
        verify(valueOperations, never()).get(anyString());
    }

    /**
     * 测试删除验证码
     * 验证：
     * 1. 删除操作应该调用Redis的delete方法
     * 2. 使用正确的key
     */
    @Test
    void deleteCode_ShouldDeleteFromRedis() {
        // Given
        String email = "test@example.com";
        String expectedKey = "verification_code:" + email;

        // When
        verificationCodeService.deleteCode(email);

        // Then
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(redisTemplate, times(1)).delete(keyCaptor.capture());

        assertThat(keyCaptor.getValue())
                .as("应该使用正确的Redis key删除验证码")
                .isEqualTo(expectedKey);
    }

    /**
     * 测试生成多个验证码的唯一性
     * 验证：
     * 1. 连续生成的验证码应该不同（大概率）
     */
    @Test
    void generateAndStoreCode_ShouldGenerateDifferentCodes() {
        // Given
        String email = "test@example.com";

        // When
        String code1 = verificationCodeService.generateAndStoreCode(email);
        String code2 = verificationCodeService.generateAndStoreCode(email);
        String code3 = verificationCodeService.generateAndStoreCode(email);

        // Then
        // 注意：理论上可能生成相同的验证码，但概率极低（1/900000）
        // 这里我们测试至少有一个不同
        boolean allDifferent = !code1.equals(code2) || !code2.equals(code3) || !code1.equals(code3);
        
        assertThat(allDifferent)
                .as("连续生成的验证码应该不同（大概率）")
                .isTrue();
    }

    /**
     * 测试完整的验证码流程
     * 验证：
     * 1. 生成验证码
     * 2. 验证正确的验证码
     * 3. 删除验证码
     * 4. 验证删除后的验证码失败
     */
    @Test
    void completeVerificationCodeFlow_ShouldWorkCorrectly() {
        // Given
        String email = "test@example.com";
        String redisKey = "verification_code:" + email;

        // Step 1: 生成验证码
        String code = verificationCodeService.generateAndStoreCode(email);

        // 模拟Redis存储
        when(valueOperations.get(redisKey)).thenReturn(code);

        // Step 2: 验证正确的验证码
        boolean validationResult = verificationCodeService.validateCode(email, code);
        assertThat(validationResult)
                .as("正确的验证码应该验证成功")
                .isTrue();

        // Step 3: 删除验证码
        verificationCodeService.deleteCode(email);

        // 模拟Redis删除后返回null
        when(valueOperations.get(redisKey)).thenReturn(null);

        // Step 4: 验证删除后的验证码失败
        boolean validationAfterDelete = verificationCodeService.validateCode(email, code);
        assertThat(validationAfterDelete)
                .as("删除后的验证码应该验证失败")
                .isFalse();
    }
}
