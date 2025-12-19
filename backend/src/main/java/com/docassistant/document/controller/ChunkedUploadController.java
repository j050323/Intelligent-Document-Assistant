package com.docassistant.document.controller;

import com.docassistant.auth.dto.ErrorResponse;
import com.docassistant.document.dto.ChunkUploadRequest;
import com.docassistant.document.dto.ChunkUploadResponse;
import com.docassistant.document.service.ChunkedUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 分块上传控制器
 */
@Tag(name = "分块上传", description = "大文件分块上传和断点续传相关接口")
@Slf4j
@RestController
@RequestMapping("/api/documents/chunked")
@RequiredArgsConstructor
@Validated
public class ChunkedUploadController {
    
    private final ChunkedUploadService chunkedUploadService;
    
    /**
     * 上传文件分块
     */
    @Operation(
            summary = "上传文件分块",
            description = "上传大文件的一个分块。支持断点续传，可以查询已上传的分块并继续上传。需要JWT令牌认证。",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "分块上传成功",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ChunkUploadResponse.class)
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
    @PostMapping("/upload")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ChunkUploadResponse> uploadChunk(
            @Parameter(description = "分块文件", required = true)
            @RequestParam("chunk") MultipartFile chunk,
            @Parameter(description = "文件唯一标识符", required = true)
            @RequestParam("fileIdentifier") String fileIdentifier,
            @Parameter(description = "当前分块索引（从0开始）", required = true)
            @RequestParam("chunkIndex") Integer chunkIndex,
            @Parameter(description = "总分块数", required = true)
            @RequestParam("totalChunks") Integer totalChunks,
            @Parameter(description = "原始文件名", required = true)
            @RequestParam("filename") String filename,
            @Parameter(description = "文件总大小（字节）", required = true)
            @RequestParam("totalSize") Long totalSize,
            @Parameter(description = "文件夹ID（可选）")
            @RequestParam(value = "folderId", required = false) Long folderId,
            Authentication authentication) {
        
        Long userId = (Long) authentication.getPrincipal();
        log.info("Chunked upload request for userId: {}, fileIdentifier: {}, chunk: {}/{}", 
                 userId, fileIdentifier, chunkIndex + 1, totalChunks);
        
        ChunkUploadRequest request = ChunkUploadRequest.builder()
                .fileIdentifier(fileIdentifier)
                .chunkIndex(chunkIndex)
                .totalChunks(totalChunks)
                .filename(filename)
                .totalSize(totalSize)
                .folderId(folderId)
                .build();
        
        ChunkUploadResponse response = chunkedUploadService.uploadChunk(userId, chunk, request);
        
        // 如果完成，返回201 Created，否则返回200 OK
        HttpStatus status = response.getCompleted() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(response);
    }
    
    /**
     * 获取已上传的分块列表
     */
    @Operation(
            summary = "获取已上传的分块列表",
            description = "查询指定文件已上传的分块索引列表，用于断点续传。需要JWT令牌认证。",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "查询成功",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = List.class)
                    )
            )
    })
    @GetMapping("/uploaded-chunks")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Integer>> getUploadedChunks(
            @Parameter(description = "文件唯一标识符", required = true)
            @RequestParam("fileIdentifier") String fileIdentifier,
            Authentication authentication) {
        
        Long userId = (Long) authentication.getPrincipal();
        log.info("Get uploaded chunks request for userId: {}, fileIdentifier: {}", userId, fileIdentifier);
        
        List<Integer> uploadedChunks = chunkedUploadService.getUploadedChunks(userId, fileIdentifier);
        return ResponseEntity.ok(uploadedChunks);
    }
    
    /**
     * 取消分块上传
     */
    @Operation(
            summary = "取消分块上传",
            description = "取消正在进行的分块上传，清理临时文件。需要JWT令牌认证。",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "取消成功"
            )
    })
    @DeleteMapping("/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> cancelChunkedUpload(
            @Parameter(description = "文件唯一标识符", required = true)
            @RequestParam("fileIdentifier") String fileIdentifier,
            Authentication authentication) {
        
        Long userId = (Long) authentication.getPrincipal();
        log.info("Cancel chunked upload request for userId: {}, fileIdentifier: {}", userId, fileIdentifier);
        
        chunkedUploadService.cancelChunkedUpload(userId, fileIdentifier);
        return ResponseEntity.noContent().build();
    }
}
