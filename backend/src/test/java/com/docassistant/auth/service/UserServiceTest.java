package com.docassistant.auth.service;

import com.docassistant.auth.dto.UpdateEmailRequest;
import com.docassistant.auth.dto.UpdatePasswordRequest;
import com.docassistant.auth.dto.UpdateUserRequest;
import com.docassistant.auth.dto.UserDTO;
import com.docassistant.auth.entity.User;
import com.docassistant.auth.entity.UserRole;
import com.docassistant.auth.exception.AuthenticationException;
import com.docassistant.auth.exception.AuthorizationException;
import com.docassistant.auth.exception.ValidationException;
import com.docassistant.auth.repository.UserRepository;
import com.docassistant.auth.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 用户服务单元测试
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private PasswordService passwordService;
    
    @Mock
    private VerificationCodeService verificationCodeService;
    
    @Mock
    private EmailService emailService;
    
    @InjectMocks
    private UserServiceImpl userService;
    
    private User testUser;
    
    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .passwordHash("$2a$10$hashedpassword")
                .role(UserRole.REGULAR_USER)
                .isEmailVerified(true)
                .createdAt(LocalDateTime.now())
                .build();
    }
    
    @Test
    void getCurrentUser_ShouldReturnUserDTO_WhenUserExists() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        
        // When
        UserDTO result = userService.getCurrentUser(1L);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getRole()).isEqualTo("REGULAR_USER");
        
        verify(userRepository).findById(1L);
    }
    
    @Test
    void getCurrentUser_ShouldThrowException_WhenUserNotFound() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> userService.getCurrentUser(999L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("用户不存在");
    }
    
    @Test
    void updateUserInfo_ShouldUpdateUsername_WhenUsernameIsUnique() {
        // Given
        UpdateUserRequest request = UpdateUserRequest.builder()
                .username("newusername")
                .build();
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByUsername("newusername")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        
        // When
        UserDTO result = userService.updateUserInfo(1L, request);
        
        // Then
        assertThat(result).isNotNull();
        verify(userRepository).existsByUsername("newusername");
        verify(userRepository).save(any(User.class));
    }
    
    @Test
    void updateUserInfo_ShouldThrowException_WhenUsernameAlreadyExists() {
        // Given
        UpdateUserRequest request = UpdateUserRequest.builder()
                .username("existinguser")
                .build();
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByUsername("existinguser")).thenReturn(true);
        
        // When & Then
        assertThatThrownBy(() -> userService.updateUserInfo(1L, request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("用户名已被使用");
    }
    
    @Test
    void uploadAvatar_ShouldThrowException_WhenFileIsEmpty() {
        // Given
        MockMultipartFile emptyFile = new MockMultipartFile(
                "avatar", 
                "test.jpg", 
                "image/jpeg", 
                new byte[0]
        );
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        
        // When & Then
        assertThatThrownBy(() -> userService.uploadAvatar(1L, emptyFile))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("文件不能为空");
    }
    
    // Note: File format validation test is skipped in unit tests due to @Value injection complexity
    // This validation is covered by integration tests
    
    @Test
    void updateEmail_ShouldUpdateEmail_WhenVerificationCodeIsValid() {
        // Given
        UpdateEmailRequest request = UpdateEmailRequest.builder()
                .newEmail("newemail@example.com")
                .verificationCode("123456")
                .build();
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmail("newemail@example.com")).thenReturn(false);
        when(verificationCodeService.validateCode("newemail@example.com", "123456")).thenReturn(true);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        
        // When
        userService.updateEmail(1L, request);
        
        // Then
        verify(verificationCodeService).validateCode("newemail@example.com", "123456");
        verify(verificationCodeService).deleteCode("newemail@example.com");
        verify(userRepository).save(any(User.class));
    }
    
    @Test
    void updateEmail_ShouldThrowException_WhenEmailAlreadyExists() {
        // Given
        UpdateEmailRequest request = UpdateEmailRequest.builder()
                .newEmail("existing@example.com")
                .verificationCode("123456")
                .build();
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);
        
        // When & Then
        assertThatThrownBy(() -> userService.updateEmail(1L, request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("邮箱已被使用");
    }
    
    @Test
    void updateEmail_ShouldThrowException_WhenVerificationCodeInvalid() {
        // Given
        UpdateEmailRequest request = UpdateEmailRequest.builder()
                .newEmail("newemail@example.com")
                .verificationCode("wrong")
                .build();
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmail("newemail@example.com")).thenReturn(false);
        when(verificationCodeService.validateCode("newemail@example.com", "wrong")).thenReturn(false);
        
        // When & Then
        assertThatThrownBy(() -> userService.updateEmail(1L, request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("验证码错误或已过期");
    }
    
    @Test
    void updatePassword_ShouldUpdatePassword_WhenOldPasswordIsCorrect() {
        // Given
        UpdatePasswordRequest request = UpdatePasswordRequest.builder()
                .oldPassword("oldpassword")
                .newPassword("NewPassword123")
                .build();
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordService.verifyPassword("oldpassword", testUser.getPasswordHash())).thenReturn(true);
        when(passwordService.isPasswordValid("NewPassword123")).thenReturn(true);
        when(passwordService.encodePassword("NewPassword123")).thenReturn("$2a$10$newhashedpassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        
        // When
        userService.updatePassword(1L, request);
        
        // Then
        verify(passwordService).verifyPassword(eq("oldpassword"), eq("$2a$10$hashedpassword"));
        verify(passwordService).isPasswordValid("NewPassword123");
        verify(passwordService).encodePassword("NewPassword123");
        verify(userRepository).save(any(User.class));
    }
    
    @Test
    void updatePassword_ShouldThrowException_WhenOldPasswordIsIncorrect() {
        // Given
        UpdatePasswordRequest request = UpdatePasswordRequest.builder()
                .oldPassword("wrongpassword")
                .newPassword("NewPassword123")
                .build();
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordService.verifyPassword("wrongpassword", testUser.getPasswordHash())).thenReturn(false);
        
        // When & Then
        assertThatThrownBy(() -> userService.updatePassword(1L, request))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("原密码错误");
    }
    
    @Test
    void updatePassword_ShouldThrowException_WhenNewPasswordIsWeak() {
        // Given
        UpdatePasswordRequest request = UpdatePasswordRequest.builder()
                .oldPassword("oldpassword")
                .newPassword("weak")
                .build();
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordService.verifyPassword("oldpassword", testUser.getPasswordHash())).thenReturn(true);
        when(passwordService.isPasswordValid("weak")).thenReturn(false);
        
        // When & Then
        assertThatThrownBy(() -> userService.updatePassword(1L, request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("密码必须至少8个字符");
    }
    
    @Test
    void updateUserRole_ShouldUpdateRole_WhenAdminHasPermission() {
        // Given
        User admin = User.builder()
                .id(2L)
                .username("admin")
                .email("admin@example.com")
                .role(UserRole.ADMINISTRATOR)
                .build();
        
        when(userRepository.findById(2L)).thenReturn(Optional.of(admin));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        
        // When
        userService.updateUserRole(2L, 1L, UserRole.ADMINISTRATOR);
        
        // Then
        verify(userRepository).save(any(User.class));
    }
    
    @Test
    void updateUserRole_ShouldThrowException_WhenUserIsNotAdmin() {
        // Given
        User regularUser = User.builder()
                .id(2L)
                .username("regular")
                .email("regular@example.com")
                .role(UserRole.REGULAR_USER)
                .build();
        
        when(userRepository.findById(2L)).thenReturn(Optional.of(regularUser));
        
        // When & Then
        assertThatThrownBy(() -> userService.updateUserRole(2L, 1L, UserRole.ADMINISTRATOR))
                .isInstanceOf(AuthorizationException.class)
                .hasMessageContaining("权限不足");
    }
}
