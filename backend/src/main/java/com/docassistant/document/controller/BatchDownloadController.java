package com.docassistant.document.controller;

import com.docassistant.auth.dto.ErrorResponse;
import com.docassistant.document.service.CompressionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 批量下载控制器
 */
@Tag(name = "批量下载", description = "批量下载和压缩文档相关接口")
@Slf4j
@RestController
@RequestMapping("/api/documents/batch-download")
@RequiredArgsConstructor
@Validated
public class BatchDownloadController {
    
    private final CompressionService compressionService;
    
    /**
     * 批量下载文档（压缩为ZIP）
     */
    @Operation(
            summary = "批量下载文档",
            description = "将多个文档压缩为ZIP文件并下载。需要JWT令牌认证。",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "下载成功",
                    content = @Content(
                            mediaType = "application/zip"
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "请求参数错误",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public void batchDownloadDocuments(
            @Parameter(description = "文档ID列表", required = true)
            @RequestBody @NotEmpty(message = "文档ID列表不能为空") List<Long> documentIds,
            Authentication authentication,
            HttpServletResponse response) throws IOException {
        
        Long userId = (Long) authentication.getPrincipal();
        log.info("Batch download documents request for userId: {}, document count: {}", 
                 userId, documentIds.size());
        
        // 设置响应头
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = "documents_" + timestamp + ".zip";
        
        response.setContentType("application/zip");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, 
                          "attachment; filename=\"" + filename + "\"");
        
        // 压缩并写入响应流
        compressionService.compressDocuments(userId, documentIds, response.getOutputStream());
        
        log.info("Batch download completed for userId: {}", userId);
    }
    
    /**
     * 下载文件夹中的所有文档（压缩为ZIP）
     */
    @Operation(
            summary = "下载文件夹中的所有文档",
            description = "将文件夹中的所有文档压缩为ZIP文件并下载。需要JWT令牌认证。",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "下载成功",
                    content = @Content(
                            mediaType = "application/zip"
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "文件夹不存在或为空",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @GetMapping("/folder/{folderId}")
    @PreAuthorize("isAuthenticated()")
    public void downloadFolderDocuments(
            @Parameter(description = "文件夹ID", required = true, example = "1")
            @PathVariable Long folderId,
            Authentication authentication,
            HttpServletResponse response) throws IOException {
        
        Long userId = (Long) authentication.getPrincipal();
        log.info("Download folder documents request for userId: {}, folderId: {}", userId, folderId);
        
        // 设置响应头
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = "folder_" + folderId + "_" + timestamp + ".zip";
        
        response.setContentType("application/zip");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, 
                          "attachment; filename=\"" + filename + "\"");
        
        // 压缩并写入响应流
        compressionService.compressFolderDocuments(userId, folderId, response.getOutputStream());
        
        log.info("Folder download completed for userId: {}, folderId: {}", userId, folderId);
    }
}
