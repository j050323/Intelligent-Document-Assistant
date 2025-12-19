package com.docassistant.auth.service;

/**
 * 密码服务接口
 * 提供密码加密和验证功能
 */
public interface PasswordService {
    
    /**
     * 加密密码
     * 使用BCrypt算法对明文密码进行加密
     * 
     * @param rawPassword 明文密码
     * @return 加密后的密码哈希值
     */
    String encodePassword(String rawPassword);
    
    /**
     * 验证密码
     * 比对明文密码与存储的哈希值是否匹配
     * 
     * @param rawPassword 明文密码
     * @param encodedPassword 存储的加密密码
     * @return 如果密码匹配返回true,否则返回false
     */
    boolean verifyPassword(String rawPassword, String encodedPassword);
    
    /**
     * 验证密码复杂度
     * 检查密码是否符合安全要求:
     * - 长度至少8个字符
     * - 包含字母和数字
     * 
     * @param password 待验证的密码
     * @return 如果密码符合要求返回true,否则返回false
     */
    boolean isPasswordValid(String password);
}
