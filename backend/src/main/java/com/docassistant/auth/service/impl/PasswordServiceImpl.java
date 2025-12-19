package com.docassistant.auth.service.impl;

import com.docassistant.auth.service.PasswordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * 密码服务实现类
 * 使用BCrypt算法进行密码加密和验证
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordServiceImpl implements PasswordService {

    private final PasswordEncoder passwordEncoder;
    
    // 密码复杂度正则表达式
    private static final Pattern LETTER_PATTERN = Pattern.compile(".*[a-zA-Z].*");
    private static final Pattern DIGIT_PATTERN = Pattern.compile(".*\\d.*");
    private static final int MIN_PASSWORD_LENGTH = 8;

    @Override
    public String encodePassword(String rawPassword) {
        if (rawPassword == null || rawPassword.isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }
        
        log.debug("正在加密密码");
        String encoded = passwordEncoder.encode(rawPassword);
        log.debug("密码加密完成");
        
        return encoded;
    }

    @Override
    public boolean verifyPassword(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            log.warn("密码验证失败: 密码或哈希值为空");
            return false;
        }
        
        boolean matches = passwordEncoder.matches(rawPassword, encodedPassword);
        log.debug("密码验证结果: {}", matches);
        
        return matches;
    }

    @Override
    public boolean isPasswordValid(String password) {
        if (password == null) {
            return false;
        }
        
        // 检查长度
        if (password.length() < MIN_PASSWORD_LENGTH) {
            log.debug("密码验证失败: 长度少于{}个字符", MIN_PASSWORD_LENGTH);
            return false;
        }
        
        // 检查是否包含字母
        if (!LETTER_PATTERN.matcher(password).matches()) {
            log.debug("密码验证失败: 不包含字母");
            return false;
        }
        
        // 检查是否包含数字
        if (!DIGIT_PATTERN.matcher(password).matches()) {
            log.debug("密码验证失败: 不包含数字");
            return false;
        }
        
        return true;
    }
}
