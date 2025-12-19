package com.docassistant.auth.repository;

import com.docassistant.auth.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 密码重置令牌数据访问接口
 */
@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    
    /**
     * 根据令牌字符串查询
     * @param token 令牌字符串
     * @return 密码重置令牌对象（如果存在）
     */
    Optional<PasswordResetToken> findByToken(String token);
    
    /**
     * 根据用户ID查询未使用的令牌
     * @param userId 用户ID
     * @return 密码重置令牌列表
     */
    @Query("SELECT p FROM PasswordResetToken p WHERE p.userId = ?1 AND p.used = false")
    Optional<PasswordResetToken> findUnusedTokenByUserId(Long userId);
    
    /**
     * 根据令牌字符串和未使用状态查询
     * @param token 令牌字符串
     * @param used 是否已使用
     * @return 密码重置令牌对象（如果存在）
     */
    Optional<PasswordResetToken> findByTokenAndUsed(String token, Boolean used);
    
    /**
     * 删除过期的令牌
     * @param now 当前时间
     */
    @Modifying
    @Query("DELETE FROM PasswordResetToken p WHERE p.expiresAt < ?1")
    void deleteExpiredTokens(LocalDateTime now);
    
    /**
     * 标记用户的所有令牌为已使用
     * @param userId 用户ID
     */
    @Modifying
    @Query("UPDATE PasswordResetToken p SET p.used = true WHERE p.userId = ?1 AND p.used = false")
    void markAllTokensAsUsedByUserId(Long userId);
}
