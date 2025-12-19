package com.docassistant.document.controller;

import com.docassistant.auth.entity.SystemLog;
import com.docassistant.auth.service.SystemLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 文档操作日志控制器（管理员）
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/document-logs")
@RequiredArgsConstructor
@Tag(name = "Document Logs (Admin)", description = "文档操作日志管理接口（管理员）")
public class DocumentLogController {
    
    private final SystemLogService systemLogService;
    
    /**
     * 查询所有文档操作日志
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @Operation(summary = "查询所有文档操作日志", description = "管理员查询所有文档操作日志（分页）")
    public ResponseEntity<Page<SystemLog>> getAllDocumentLogs(
            @Parameter(description = "页码", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("管理员查询所有文档操作日志，页码: {}, 每页数量: {}", page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<SystemLog> logs = systemLogService.findDocumentOperationLogs(pageable);
        
        log.info("查询完成，共 {} 条日志", logs.getTotalElements());
        
        return ResponseEntity.ok(logs);
    }
    
    /**
     * 根据用户ID查询文档操作日志
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @Operation(summary = "根据用户ID查询文档操作日志", description = "管理员查询指定用户的文档操作日志（分页）")
    public ResponseEntity<Page<SystemLog>> getDocumentLogsByUserId(
            @Parameter(description = "用户ID", required = true)
            @PathVariable Long userId,
            @Parameter(description = "页码", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("管理员查询用户 {} 的文档操作日志，页码: {}, 每页数量: {}", userId, page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<SystemLog> logs = systemLogService.findDocumentOperationLogsByUserId(userId, pageable);
        
        log.info("查询完成，用户 {} 共 {} 条日志", userId, logs.getTotalElements());
        
        return ResponseEntity.ok(logs);
    }
    
    /**
     * 根据操作类型查询文档操作日志
     */
    @GetMapping("/type/{operationType}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @Operation(summary = "根据操作类型查询文档操作日志", description = "管理员查询指定操作类型的文档操作日志（分页）")
    public ResponseEntity<Page<SystemLog>> getDocumentLogsByType(
            @Parameter(description = "操作类型", required = true, example = "DOCUMENT_DELETE")
            @PathVariable String operationType,
            @Parameter(description = "页码", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("管理员查询操作类型 {} 的文档操作日志，页码: {}, 每页数量: {}", operationType, page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<SystemLog> logs = systemLogService.findDocumentOperationLogsByType(operationType, pageable);
        
        log.info("查询完成，操作类型 {} 共 {} 条日志", operationType, logs.getTotalElements());
        
        return ResponseEntity.ok(logs);
    }
    
    /**
     * 根据用户ID和操作类型查询文档操作日志
     */
    @GetMapping("/user/{userId}/type/{operationType}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @Operation(summary = "根据用户ID和操作类型查询文档操作日志", description = "管理员查询指定用户和操作类型的文档操作日志（分页）")
    public ResponseEntity<Page<SystemLog>> getDocumentLogsByUserIdAndType(
            @Parameter(description = "用户ID", required = true)
            @PathVariable Long userId,
            @Parameter(description = "操作类型", required = true, example = "DOCUMENT_DELETE")
            @PathVariable String operationType,
            @Parameter(description = "页码", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("管理员查询用户 {} 操作类型 {} 的文档操作日志，页码: {}, 每页数量: {}", 
                 userId, operationType, page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<SystemLog> logs = systemLogService.findDocumentOperationLogsByUserIdAndType(
                userId, operationType, pageable);
        
        log.info("查询完成，用户 {} 操作类型 {} 共 {} 条日志", 
                 userId, operationType, logs.getTotalElements());
        
        return ResponseEntity.ok(logs);
    }
}
