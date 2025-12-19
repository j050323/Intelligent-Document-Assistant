package com.docassistant.auth.service.impl;

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
import com.docassistant.auth.service.EmailService;
import com.docassistant.auth.service.PasswordService;
import com.docassistant.auth.service.UserService;
import com.docassistant.auth.service.VerificationCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 用户管理业务逻辑服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    
    private final UserRepository userRepository;
    private final PasswordService passwordService;
    private final VerificationCodeService verificationCodeService;
    private final EmailService emailService;
    
    @Value("${app.upload.avatar-dir:uploads/avatars}")
    private String avatarUploadDir;
    
    @Value("${app.upload.max-avatar-size:5242880}") // 5MB default
    private long maxAvatarSize;
    
    private static final List<String> ALLOWED_AVATAR_FORMATS = Arrays.asList("jpg", "jpeg", "png", "gif");
    
    @Override
    @Transactional(readOnly = true)
    public UserDTO getCurrentUser(Long userId) {
        log.info("Getting user info for userId: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ValidationException("用户不存在", "USER_NOT_FOUND"));
        
        return convertToDTO(user);
    }
    
    @Override
    @Transactional
    public UserDTO updateUserInfo(Long userId, UpdateUserRequest request) {
        log.info("Updating user info for userId: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ValidationException("用户不存在", "USER_NOT_FOUND"));
        
        // 验证用户名唯一性（如果要更新用户名）
        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new ValidationException("用户名已被使用", "USERNAME_ALREADY_EXISTS");
            }
            user.setUsername(request.getUsername());
        }
        
        // 更新头像URL（如果提供）
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        
        User savedUser = userRepository.save(user);
        log.info("User info updated successfully for userId: {}", userId);
        
        return convertToDTO(savedUser);
    }
    
    @Override
    @Transactional
    public String uploadAvatar(Long userId, MultipartFile file) {
        log.info("Uploading avatar for userId: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ValidationException("用户不存在", "USER_NOT_FOUND"));
        
        // 验证文件不为空
        if (file.isEmpty()) {
            throw new ValidationException("文件不能为空", "FILE_EMPTY");
        }
        
        // 验证文件大小
        if (file.getSize() > maxAvatarSize) {
            throw new ValidationException("文件大小超过5MB限制", "FILE_SIZE_EXCEEDED");
        }
        
        // 验证文件格式
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new ValidationException("文件名无效", "INVALID_FILENAME");
        }
        
        String fileExtension = getFileExtension(originalFilename).toLowerCase();
        if (!ALLOWED_AVATAR_FORMATS.contains(fileExtension)) {
            throw new ValidationException(
                    "不支持的文件格式，仅支持JPG、PNG、GIF", 
                    "INVALID_FILE_FORMAT"
            );
        }
        
        try {
            // 创建上传目录
            Path uploadPath = Paths.get(avatarUploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            
            // 生成唯一文件名
            String newFilename = UUID.randomUUID().toString() + "." + fileExtension;
            Path filePath = uploadPath.resolve(newFilename);
            
            // 保存文件
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            
            // 生成访问URL
            String avatarUrl = "/avatars/" + newFilename;
            
            // 更新用户头像URL
            user.setAvatarUrl(avatarUrl);
            userRepository.save(user);
            
            log.info("Avatar uploaded successfully for userId: {}, url: {}", userId, avatarUrl);
            return avatarUrl;
            
        } catch (IOException e) {
            log.error("Failed to upload avatar for userId: {}", userId, e);
            throw new ValidationException("文件上传失败", "FILE_UPLOAD_FAILED", e);
        }
    }
    
    @Override
    @Transactional
    public void updateEmail(Long userId, UpdateEmailRequest request) {
        log.info("Updating email for userId: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ValidationException("用户不存在", "USER_NOT_FOUND"));
        
        // 验证新邮箱是否已被使用
        if (userRepository.existsByEmail(request.getNewEmail())) {
            throw new ValidationException("邮箱已被使用", "EMAIL_ALREADY_EXISTS");
        }
        
        // 验证验证码
        if (!verificationCodeService.validateCode(request.getNewEmail(), request.getVerificationCode())) {
            throw new ValidationException("验证码错误或已过期", "INVALID_VERIFICATION_CODE");
        }
        
        // 更新邮箱
        user.setEmail(request.getNewEmail());
        user.setIsEmailVerified(true);
        userRepository.save(user);
        
        // 删除验证码
        verificationCodeService.deleteCode(request.getNewEmail());
        
        log.info("Email updated successfully for userId: {}", userId);
    }
    
    @Override
    @Transactional
    public void updatePassword(Long userId, UpdatePasswordRequest request) {
        log.info("Updating password for userId: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ValidationException("用户不存在", "USER_NOT_FOUND"));
        
        // 验证原密码
        if (!passwordService.verifyPassword(request.getOldPassword(), user.getPasswordHash())) {
            throw new AuthenticationException("原密码错误", "OLD_PASSWORD_INCORRECT");
        }
        
        // 验证新密码复杂度
        if (!passwordService.isPasswordValid(request.getNewPassword())) {
            throw new ValidationException(
                    "密码必须至少8个字符，且包含字母和数字", 
                    "PASSWORD_TOO_WEAK"
            );
        }
        
        // 更新密码
        String encodedPassword = passwordService.encodePassword(request.getNewPassword());
        user.setPasswordHash(encodedPassword);
        user.setPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);
        
        log.info("Password updated successfully for userId: {}", userId);
    }
    
    @Override
    @Transactional
    public void updateUserRole(Long adminId, Long userId, UserRole newRole) {
        log.info("Admin {} updating role for userId: {} to {}", adminId, userId, newRole);
        
        // 验证管理员权限
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ValidationException("管理员不存在", "USER_NOT_FOUND"));
        
        if (admin.getRole() != UserRole.ADMINISTRATOR) {
            throw new AuthorizationException("权限不足，需要管理员权限", "INSUFFICIENT_PERMISSIONS");
        }
        
        // 获取目标用户
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ValidationException("目标用户不存在", "USER_NOT_FOUND"));
        
        // 更新角色
        user.setRole(newRole);
        userRepository.save(user);
        
        log.info("User role updated successfully for userId: {} to {}", userId, newRole);
    }
    
    /**
     * 将User实体转换为UserDTO
     * 确保不包含密码字段
     */
    private UserDTO convertToDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .avatarUrl(user.getAvatarUrl())
                .isEmailVerified(user.getIsEmailVerified())
                .createdAt(user.getCreatedAt())
                .build();
    }
    
    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1);
    }
}
