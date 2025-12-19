package com.docassistant.auth.service;

import com.docassistant.auth.dto.UpdateEmailRequest;
import com.docassistant.auth.dto.UpdatePasswordRequest;
import com.docassistant.auth.dto.UpdateUserRequest;
import com.docassistant.auth.dto.UserDTO;
import com.docassistant.auth.entity.UserRole;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户管理业务逻辑服务接口
 */
public interface UserService {
    
    /**
     * 获取当前用户信息
     * 
     * @param userId 用户ID
     * @return 用户信息DTO（不包含密码字段）
     */
    UserDTO getCurrentUser(Long userId);
    
    /**
     * 更新用户个人信息
     * 
     * @param userId 用户ID
     * @param request 更新请求
     * @return 更新后的用户信息
     */
    UserDTO updateUserInfo(Long userId, UpdateUserRequest request);
    
    /**
     * 上传用户头像
     * 
     * @param userId 用户ID
     * @param file 头像文件
     * @return 头像URL
     */
    String uploadAvatar(Long userId, MultipartFile file);
    
    /**
     * 更新用户邮箱
     * 
     * @param userId 用户ID
     * @param request 更新邮箱请求（包含新邮箱和验证码）
     */
    void updateEmail(Long userId, UpdateEmailRequest request);
    
    /**
     * 更新用户密码
     * 
     * @param userId 用户ID
     * @param request 更新密码请求（包含原密码和新密码）
     */
    void updatePassword(Long userId, UpdatePasswordRequest request);
    
    /**
     * 更新用户角色（管理员功能）
     * 
     * @param adminId 管理员ID
     * @param userId 目标用户ID
     * @param newRole 新角色
     */
    void updateUserRole(Long adminId, Long userId, UserRole newRole);
}
