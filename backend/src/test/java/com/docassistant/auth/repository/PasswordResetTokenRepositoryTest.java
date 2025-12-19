package com.docassistant.auth.repository;

import com.docassistant.auth.entity.PasswordResetToken;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PasswordResetTokenRepository单元测试
 */
@DataJpaTest
@ActiveProfiles("test")
class PasswordResetTokenRepositoryTest {
    
    @Autowired
    private PasswordResetTokenRepository tokenRepository;
    
    @Autowired
    private EntityManager entityManager;
    
    @BeforeEach
    void setUp() {
        tokenRepository.deleteAll();
    }
    
    @Test
    void shouldSavePasswordResetToken() {
        // Given
        PasswordResetToken token = PasswordResetToken.builder()
                .userId(1L)
                .token("test-token-123")
                .expiresAt(LocalDateTime.now().plusHours(1))
                .used(false)
                .build();
        
        // When
        PasswordResetToken savedToken = tokenRepository.save(token);
        
        // Then
        assertThat(savedToken.getId()).isNotNull();
        assertThat(savedToken.getUserId()).isEqualTo(1L);
        assertThat(savedToken.getToken()).isEqualTo("test-token-123");
        assertThat(savedToken.getUsed()).isFalse();
        assertThat(savedToken.getCreatedAt()).isNotNull();
    }
    
    @Test
    void shouldFindTokenByTokenString() {
        // Given
        PasswordResetToken token = createTestToken(1L, "test-token-123", false);
        
        // When
        Optional<PasswordResetToken> found = tokenRepository.findByToken("test-token-123");
        
        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo(1L);
    }
    
    @Test
    void shouldReturnEmptyWhenTokenNotFound() {
        // When
        Optional<PasswordResetToken> found = tokenRepository.findByToken("nonexistent-token");
        
        // Then
        assertThat(found).isEmpty();
    }
    
    @Test
    void shouldFindUnusedTokenByUserId() {
        // Given
        createTestToken(1L, "token-1", true);  // used
        PasswordResetToken unusedToken = createTestToken(1L, "token-2", false);  // unused
        
        // When
        Optional<PasswordResetToken> found = tokenRepository.findUnusedTokenByUserId(1L);
        
        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getToken()).isEqualTo("token-2");
        assertThat(found.get().getUsed()).isFalse();
    }
    
    @Test
    void shouldFindTokenByTokenAndUsedStatus() {
        // Given
        createTestToken(1L, "test-token", false);
        
        // When
        Optional<PasswordResetToken> found = tokenRepository.findByTokenAndUsed("test-token", false);
        
        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getUsed()).isFalse();
    }
    
    @Test
    void shouldNotFindUsedTokenWhenSearchingForUnused() {
        // Given
        createTestToken(1L, "test-token", true);
        
        // When
        Optional<PasswordResetToken> found = tokenRepository.findByTokenAndUsed("test-token", false);
        
        // Then
        assertThat(found).isEmpty();
    }
    
    @Test
    @org.springframework.transaction.annotation.Transactional
    void shouldDeleteExpiredTokens() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        
        PasswordResetToken expiredToken = createTestToken(1L, "expired-token", false);
        expiredToken.setExpiresAt(now.minusHours(1));
        tokenRepository.save(expiredToken);
        
        PasswordResetToken validToken = createTestToken(2L, "valid-token", false);
        validToken.setExpiresAt(now.plusHours(1));
        tokenRepository.save(validToken);
        
        // When
        tokenRepository.deleteExpiredTokens(now);
        tokenRepository.flush();
        
        // Then
        assertThat(tokenRepository.findByToken("expired-token")).isEmpty();
        assertThat(tokenRepository.findByToken("valid-token")).isPresent();
    }
    
    @Test
    @org.springframework.transaction.annotation.Transactional
    void shouldMarkAllTokensAsUsedByUserId() {
        // Given
        createTestToken(1L, "token-1", false);
        createTestToken(1L, "token-2", false);
        createTestToken(2L, "token-3", false);
        entityManager.flush();
        
        // When
        tokenRepository.markAllTokensAsUsedByUserId(1L);
        entityManager.flush();
        entityManager.clear(); // Clear the persistence context to force fresh queries
        
        // Then
        Optional<PasswordResetToken> token1 = tokenRepository.findByToken("token-1");
        Optional<PasswordResetToken> token2 = tokenRepository.findByToken("token-2");
        Optional<PasswordResetToken> token3 = tokenRepository.findByToken("token-3");
        
        assertThat(token1).isPresent();
        assertThat(token1.get().getUsed()).isTrue();
        
        assertThat(token2).isPresent();
        assertThat(token2.get().getUsed()).isTrue();
        
        assertThat(token3).isPresent();
        assertThat(token3.get().getUsed()).isFalse();
    }
    
    @Test
    void shouldEnforceUniqueToken() {
        // Given
        createTestToken(1L, "duplicate-token", false);
        
        PasswordResetToken duplicateToken = PasswordResetToken.builder()
                .userId(2L)
                .token("duplicate-token")
                .expiresAt(LocalDateTime.now().plusHours(1))
                .used(false)
                .build();
        
        // When & Then
        try {
            tokenRepository.saveAndFlush(duplicateToken);
            assertThat(false).as("Should have thrown exception for duplicate token").isTrue();
        } catch (Exception e) {
            // Expected - duplicate token constraint violation
            assertThat(e).isNotNull();
        }
    }
    
    private PasswordResetToken createTestToken(Long userId, String token, Boolean used) {
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .userId(userId)
                .token(token)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .used(used)
                .build();
        return tokenRepository.save(resetToken);
    }
}
