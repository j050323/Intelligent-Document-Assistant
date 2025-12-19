package com.docassistant.auth.repository;

import com.docassistant.auth.entity.User;
import com.docassistant.auth.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UserRepository单元测试
 */
@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {
    
    @Autowired
    private UserRepository userRepository;
    
    private User testUser;
    
    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        
        testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .passwordHash("$2a$10$hashedpassword")
                .role(UserRole.REGULAR_USER)
                .isEmailVerified(false)
                .build();
    }
    
    @Test
    void shouldSaveUser() {
        // When
        User savedUser = userRepository.save(testUser);
        
        // Then
        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getUsername()).isEqualTo("testuser");
        assertThat(savedUser.getEmail()).isEqualTo("test@example.com");
        assertThat(savedUser.getRole()).isEqualTo(UserRole.REGULAR_USER);
        assertThat(savedUser.getCreatedAt()).isNotNull();
    }
    
    @Test
    void shouldFindUserByEmail() {
        // Given
        userRepository.save(testUser);
        
        // When
        Optional<User> found = userRepository.findByEmail("test@example.com");
        
        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("testuser");
    }
    
    @Test
    void shouldReturnEmptyWhenEmailNotFound() {
        // When
        Optional<User> found = userRepository.findByEmail("nonexistent@example.com");
        
        // Then
        assertThat(found).isEmpty();
    }
    
    @Test
    void shouldFindUserByUsername() {
        // Given
        userRepository.save(testUser);
        
        // When
        Optional<User> found = userRepository.findByUsername("testuser");
        
        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("test@example.com");
    }
    
    @Test
    void shouldReturnEmptyWhenUsernameNotFound() {
        // When
        Optional<User> found = userRepository.findByUsername("nonexistent");
        
        // Then
        assertThat(found).isEmpty();
    }
    
    @Test
    void shouldFindUserByUsernameOrEmail_WithUsername() {
        // Given
        userRepository.save(testUser);
        
        // When
        Optional<User> found = userRepository.findByUsernameOrEmail("testuser", "wrong@example.com");
        
        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("test@example.com");
    }
    
    @Test
    void shouldFindUserByUsernameOrEmail_WithEmail() {
        // Given
        userRepository.save(testUser);
        
        // When
        Optional<User> found = userRepository.findByUsernameOrEmail("wronguser", "test@example.com");
        
        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("testuser");
    }
    
    @Test
    void shouldCheckEmailExists() {
        // Given
        userRepository.save(testUser);
        
        // When & Then
        assertThat(userRepository.existsByEmail("test@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("nonexistent@example.com")).isFalse();
    }
    
    @Test
    void shouldCheckUsernameExists() {
        // Given
        userRepository.save(testUser);
        
        // When & Then
        assertThat(userRepository.existsByUsername("testuser")).isTrue();
        assertThat(userRepository.existsByUsername("nonexistent")).isFalse();
    }
    
    @Test
    void shouldEnforceUniqueEmail() {
        // Given
        userRepository.save(testUser);
        
        User duplicateEmailUser = User.builder()
                .username("anotheruser")
                .email("test@example.com")
                .passwordHash("$2a$10$hashedpassword")
                .role(UserRole.REGULAR_USER)
                .build();
        
        // When & Then
        try {
            userRepository.saveAndFlush(duplicateEmailUser);
            assertThat(false).as("Should have thrown exception for duplicate email").isTrue();
        } catch (Exception e) {
            // Expected - duplicate email constraint violation
            assertThat(e).isNotNull();
        }
    }
    
    @Test
    void shouldEnforceUniqueUsername() {
        // Given
        userRepository.save(testUser);
        
        User duplicateUsernameUser = User.builder()
                .username("testuser")
                .email("another@example.com")
                .passwordHash("$2a$10$hashedpassword")
                .role(UserRole.REGULAR_USER)
                .build();
        
        // When & Then
        try {
            userRepository.saveAndFlush(duplicateUsernameUser);
            assertThat(false).as("Should have thrown exception for duplicate username").isTrue();
        } catch (Exception e) {
            // Expected - duplicate username constraint violation
            assertThat(e).isNotNull();
        }
    }
}
