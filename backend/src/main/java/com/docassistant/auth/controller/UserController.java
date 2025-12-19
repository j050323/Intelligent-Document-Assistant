package com.docassistant.auth.controller;

import com.docassistant.auth.dto.ErrorResponse;
import com.docassistant.auth.dto.UpdateEmailRequest;
import com.docassistant.auth.dto.UpdatePasswordRequest;
import com.docassistant.auth.dto.UpdateUserRequest;
import com.docassistant.auth.dto.UpdateUserRoleRequest;
import com.docassistant.auth.dto.UserDTO;
import com.docassistant.auth.entity.UserRole;
import com.docassistant.auth.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户信息管理接口控制器
 */
@Tag(name = "用户管理", description = "用户信息查询、修改、头像上传等用户管理相关接口")
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    
    /**
     * 获取当前用户信息
     * 
     * @param authentication 认证信息
     * @return 用户信息
     */
    @Operation(
            summary = "获取当前用户信息",
            description = "获取当前登录用户的详细信息。需要JWT令牌认证。",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "获取成功",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "未认证或令牌无效",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDTO> getCurrentUser(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("Get current user info request for userId: {}", userId);
        
        UserDTO userDTO = userService.getCurrentUser(userId);
        return ResponseEntity.ok(userDTO);
    }
    
    /**
     * 更新个人信息
     * 
     * @param request 更新请求
     * @param authentication 认证信息
     * @return 更新后的用户信息
     */
    @Operation(
            summary = "更新个人信息",
            description = "更新当前用户的个人信息（如用户名等）。需要JWT令牌认证。",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "更新成功",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "请求参数错误",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "未认证或令牌无效",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDTO> updateUserInfo(
            @Valid @RequestBody UpdateUserRequest request,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("Update user info request for userId: {}", userId);
        
        UserDTO userDTO = userService.updateUserInfo(userId, request);
        return ResponseEntity.ok(userDTO);
    }
    
    /**
     * 上传头像
     * 
     * @param file 头像文件
     * @param authentication 认证信息
     * @return 头像URL
     */
    @Operation(
            summary = "上传头像",
            description = "上传用户头像图片。支持JPG、PNG、GIF格式，文件大小不超过5MB。需要JWT令牌认证。",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "上传成功",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "avatarUrl": "/uploads/avatars/user_1_20251215.jpg",
                                      "message": "头像上传成功"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "文件格式不支持或文件大小超限",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "未认证或令牌无效",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @PostMapping("/me/avatar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> uploadAvatar(
            @Parameter(description = "头像图片文件（JPG/PNG/GIF，最大5MB）", required = true)
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("Upload avatar request for userId: {}", userId);
        
        String avatarUrl = userService.uploadAvatar(userId, file);
        
        Map<String, String> response = new HashMap<>();
        response.put("avatarUrl", avatarUrl);
        response.put("message", "头像上传成功");
        return ResponseEntity.ok(response);
    }
    
    /**
     * 修改邮箱
     * 
     * @param request 更新邮箱请求
     * @param authentication 认证信息
     * @return 修改结果
     */
    @Operation(
            summary = "修改邮箱",
            description = "修改当前用户的邮箱地址。需要提供新邮箱和验证码。需要JWT令牌认证。",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "修改成功",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "message": "邮箱修改成功"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "验证码错误或邮箱已被使用",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @PutMapping("/me/email")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> updateEmail(
            @Valid @RequestBody UpdateEmailRequest request,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("Update email request for userId: {}", userId);
        
        userService.updateEmail(userId, request);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "邮箱修改成功");
        return ResponseEntity.ok(response);
    }
    
    /**
     * 修改密码
     * 
     * @param request 更新密码请求
     * @param authentication 认证信息
     * @return 修改结果
     */
    @Operation(
            summary = "修改密码",
            description = "修改当前用户的密码。需要提供原密码和新密码。新密码必须至少8个字符，包含字母和数字。需要JWT令牌认证。",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "修改成功",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "message": "密码修改成功"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "原密码错误或新密码不符合要求",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @PutMapping("/me/password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> updatePassword(
            @Valid @RequestBody UpdatePasswordRequest request,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("Update password request for userId: {}", userId);
        
        userService.updatePassword(userId, request);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "密码修改成功");
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取指定用户信息（管理员功能）
     * 
     * @param id 用户ID
     * @return 用户信息
     */
    @Operation(
            summary = "获取指定用户信息（管理员）",
            description = "管理员查询指定用户的详细信息。需要管理员权限。",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "获取成功",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "权限不足（非管理员）",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "用户不存在",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<UserDTO> getUserById(
            @Parameter(description = "用户ID", required = true, example = "1")
            @PathVariable Long id) {
        log.info("Get user info request for userId: {} (admin operation)", id);
        
        UserDTO userDTO = userService.getCurrentUser(id);
        return ResponseEntity.ok(userDTO);
    }
    
    /**
     * 修改用户角色（管理员功能）
     * 
     * @param id 用户ID
     * @param request 更新角色请求
     * @param authentication 认证信息
     * @return 修改结果
     */
    @Operation(
            summary = "修改用户角色（管理员）",
            description = "管理员修改指定用户的角色。需要管理员权限。",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "修改成功",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "message": "用户角色修改成功"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "权限不足（非管理员）",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "用户不存在",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<Map<String, String>> updateUserRole(
            @Parameter(description = "用户ID", required = true, example = "1")
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRoleRequest request,
            Authentication authentication) {
        Long adminId = (Long) authentication.getPrincipal();
        log.info("Update user role request for userId: {} by adminId: {}", id, adminId);
        
        UserRole newRole = UserRole.valueOf(request.getRole());
        userService.updateUserRole(adminId, id, newRole);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "用户角色修改成功");
        return ResponseEntity.ok(response);
    }
}
