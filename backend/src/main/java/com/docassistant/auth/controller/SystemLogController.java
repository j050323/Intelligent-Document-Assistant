package com.docassistant.auth.controller;

import com.docassistant.auth.dto.ErrorResponse;
import com.docassistant.auth.entity.SystemLog;
import com.docassistant.auth.service.SystemLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 系统日志查询接口控制器（管理员专用）
 */
@Tag(name = "系统日志", description = "系统日志查询接口（仅管理员可访问）")
@Slf4j
@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class SystemLogController {
    
    private final SystemLogService systemLogService;
    
    /**
     * 查询登录日志（管理员功能）
     * 支持分页、排序和时间范围筛选
     * 
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @param startTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @return 登录日志分页列表
     */
    @Operation(
            summary = "查询所有登录日志（管理员）",
            description = "查询系统所有用户的登录日志，支持分页、按时间倒序排列和时间范围筛选。需要管理员权限。",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "查询成功",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Page.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "权限不足（非管理员）",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @GetMapping("/login")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<Page<SystemLog>> getLoginLogs(
            @Parameter(description = "页码（从0开始）", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "开始时间（ISO 8601格式）", example = "2025-12-01T00:00:00")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "结束时间（ISO 8601格式）", example = "2025-12-31T23:59:59")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        
        log.info("Get login logs request - page: {}, size: {}, startTime: {}, endTime: {}", 
                page, size, startTime, endTime);
        
        // 创建分页参数，按时间倒序排列
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        Page<SystemLog> logs;
        
        // 根据是否提供时间范围参数选择不同的查询方法
        if (startTime != null && endTime != null) {
            logs = systemLogService.findLoginLogsByTimeRange(startTime, endTime, pageable);
        } else {
            logs = systemLogService.findAllLoginLogs(pageable);
        }
        
        return ResponseEntity.ok(logs);
    }
    
    /**
     * 查询指定用户的登录日志（管理员功能）
     * 支持分页、排序和时间范围筛选
     * 
     * @param userId 用户ID
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @param startTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @return 登录日志分页列表
     */
    @Operation(
            summary = "查询指定用户的登录日志（管理员）",
            description = "查询指定用户的登录日志，支持分页、按时间倒序排列和时间范围筛选。需要管理员权限。",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "查询成功",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Page.class)
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
    @GetMapping("/login/user/{userId}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<Page<SystemLog>> getLoginLogsByUserId(
            @Parameter(description = "用户ID", required = true, example = "1")
            @PathVariable Long userId,
            @Parameter(description = "页码（从0开始）", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "开始时间（ISO 8601格式）", example = "2025-12-01T00:00:00")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "结束时间（ISO 8601格式）", example = "2025-12-31T23:59:59")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        
        log.info("Get login logs for userId: {} - page: {}, size: {}, startTime: {}, endTime: {}", 
                userId, page, size, startTime, endTime);
        
        // 创建分页参数，按时间倒序排列
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        Page<SystemLog> logs;
        
        // 根据是否提供时间范围参数选择不同的查询方法
        if (startTime != null && endTime != null) {
            logs = systemLogService.findLoginLogsByUserIdAndTimeRange(userId, startTime, endTime, pageable);
        } else {
            logs = systemLogService.findLoginLogsByUserId(userId, pageable);
        }
        
        return ResponseEntity.ok(logs);
    }
}
