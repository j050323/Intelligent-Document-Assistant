package com.docassistant.auth.controller;

import com.docassistant.auth.dto.ErrorResponse;
import com.docassistant.auth.dto.LoginRequest;
import com.docassistant.auth.dto.LoginResponse;
import com.docassistant.auth.dto.RegisterRequest;
import com.docassistant.auth.dto.RegisterResponse;
import com.docassistant.auth.dto.TokenResponse;
import com.docassistant.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证相关的API接口控制器
 */
@Tag(name = "认证管理", description = "用户注册、登录、登出、密码重置等认证相关接口")
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {
    
    private final AuthService authService;
    
    /**
     * 用户注册
     * 
     * @param request 注册请求
     * @return 注册响应
     */
    @Operation(
            summary = "用户注册",
            description = "创建新用户账户。注册成功后会发送验证码到邮箱，需要调用邮箱验证接口激活账户。"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "注册成功",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = RegisterResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "userId": 1,
                                      "username": "testuser",
                                      "email": "test@example.com",
                                      "message": "注册成功，验证码已发送到邮箱"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "请求参数错误（邮箱或用户名已存在、密码不符合要求等）",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("User registration request received for email: {}", request.getEmail());
        RegisterResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * 验证邮箱
     * 
     * @param email 邮箱地址
     * @param code 验证码
     * @return 验证结果
     */
    @Operation(
            summary = "验证邮箱",
            description = "使用注册时发送到邮箱的验证码激活用户账户。验证码有效期为10分钟。"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "验证成功",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "message": "邮箱验证成功"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "验证码错误或已过期",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @PostMapping("/verify-email")
    public ResponseEntity<Map<String, String>> verifyEmail(
            @Parameter(description = "注册时使用的邮箱地址", required = true, example = "test@example.com")
            @RequestParam @NotBlank(message = "邮箱不能为空") @Email(message = "邮箱格式不正确") String email,
            @Parameter(description = "6位数字验证码", required = true, example = "123456")
            @RequestParam @NotBlank(message = "验证码不能为空") String code) {
        log.info("Email verification request received for email: {}", email);
        authService.verifyEmail(email, code);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "邮箱验证成功");
        return ResponseEntity.ok(response);
    }
    
    /**
     * 用户登录
     * 
     * @param request 登录请求
     * @param httpRequest HTTP请求对象
     * @return 登录响应
     */
    @Operation(
            summary = "用户登录",
            description = "使用用户名或邮箱登录系统。登录成功后返回JWT访问令牌和刷新令牌。"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "登录成功",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = LoginResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                                      "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                                      "expiresIn": 86400,
                                      "user": {
                                        "id": 1,
                                        "username": "testuser",
                                        "email": "test@example.com",
                                        "role": "REGULAR_USER",
                                        "avatarUrl": null,
                                        "isEmailVerified": true,
                                        "createdAt": "2025-12-15T10:30:00"
                                      }
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "用户名或密码错误",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        log.info("User login request received for: {}", request.getUsernameOrEmail());
        
        String ipAddress = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        
        LoginResponse response = authService.login(request, ipAddress, userAgent);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 用户登出
     * 
     * @param httpRequest HTTP请求对象
     * @return 登出结果
     */
    @Operation(
            summary = "用户登出",
            description = "登出当前用户，将JWT令牌加入黑名单。需要在请求头中提供有效的JWT令牌。",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "登出成功",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "message": "登出成功"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "未提供令牌或令牌无效",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest httpRequest) {
        String token = extractTokenFromRequest(httpRequest);
        log.info("User logout request received");
        
        authService.logout(token);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "登出成功");
        return ResponseEntity.ok(response);
    }
    
    /**
     * 刷新令牌
     * 
     * @param refreshToken 刷新令牌
     * @return 新的令牌响应
     */
    @Operation(
            summary = "刷新访问令牌",
            description = "使用刷新令牌获取新的访问令牌。当访问令牌过期时使用此接口。"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "刷新成功",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TokenResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                                      "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                                      "expiresIn": 86400
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "刷新令牌无效或已过期",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @PostMapping("/refresh-token")
    public ResponseEntity<TokenResponse> refreshToken(
            @Parameter(description = "登录时获取的刷新令牌", required = true)
            @RequestParam @NotBlank(message = "刷新令牌不能为空") String refreshToken) {
        log.info("Token refresh request received");
        TokenResponse response = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 请求密码重置
     * 
     * @param email 邮箱地址
     * @return 请求结果
     */
    @Operation(
            summary = "请求密码重置",
            description = "发送密码重置邮件到指定邮箱。邮件中包含重置链接，有效期为1小时。"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "邮件发送成功",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "message": "密码重置邮件已发送"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "邮箱不存在",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Parameter(description = "注册时使用的邮箱地址", required = true, example = "test@example.com")
            @RequestParam @NotBlank(message = "邮箱不能为空") @Email(message = "邮箱格式不正确") String email) {
        log.info("Password reset request received for email: {}", email);
        authService.requestPasswordReset(email);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "密码重置邮件已发送");
        return ResponseEntity.ok(response);
    }
    
    /**
     * 重置密码
     * 
     * @param token 重置令牌
     * @param newPassword 新密码
     * @return 重置结果
     */
    @Operation(
            summary = "重置密码",
            description = "使用邮件中的重置令牌设置新密码。密码必须至少8个字符，包含字母和数字。重置成功后所有现有JWT令牌将失效。"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "密码重置成功",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "message": "密码重置成功"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "重置令牌无效或已过期，或密码不符合要求",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Parameter(description = "邮件中的重置令牌", required = true)
            @RequestParam @NotBlank(message = "重置令牌不能为空") String token,
            @Parameter(description = "新密码（至少8个字符，包含字母和数字）", required = true, example = "NewPass123")
            @RequestParam @NotBlank(message = "新密码不能为空") String newPassword) {
        log.info("Password reset execution request received");
        authService.resetPassword(token, newPassword);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "密码重置成功");
        return ResponseEntity.ok(response);
    }
    
    /**
     * 从请求中提取JWT令牌
     * 
     * @param request HTTP请求对象
     * @return JWT令牌
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
    
    /**
     * 获取客户端IP地址
     * 
     * @param request HTTP请求对象
     * @return IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}
