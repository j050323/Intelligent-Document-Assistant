package com.docassistant.document.controller;

import com.docassistant.auth.dto.ErrorResponse;
import com.docassistant.document.dto.BatchOperationResult;
import com.docassistant.document.dto.DocumentDTO;
import com.docassistant.document.dto.DocumentQueryRequest;
import com.docassistant.document.dto.PreviewResponse;
import com.docassistant.document.dto.StorageInfo;
import com.docassistant.document.dto.UpdateDocumentRequest;
import com.docassistant.document.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文档管理API接口控制器
 */
@Tag(name = "文档管理", description = "文档上传、下载、预览、查询、删除等文档管理相关接口")
@Slf4j
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Validated
public class DocumentController {
    
    private final DocumentService documentService;
    
    /**
     * 上传单个文档
     * 
     * @param file 上传的文件
     * @param folderId 文件夹ID（可选）
     * @param authentication 认证信息
     * @return 文档信息
     */
    @Operation(
            summary = "上传单个文档",
            description = "上传单个文档到系统。支持PDF、Word（.doc/.docx）、TXT格式，文件大小不超过100MB。需要JWT令牌认证。",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "上传成功",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = DocumentDTO.class)
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
                    responseCode = "507",
                    description = "存储空间不足",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @PostMapping("/upload")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DocumentDTO> uploadDocument(
            @Parameter(description = "上传的文件（PDF/Word/TXT，最大100MB）", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "文件夹ID（可选）")
            @RequestParam(value = "folderId", required = false) Long folderId,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("Upload document request for userId: {}, filename: {}", userId, file.getOriginalFilename());
        
        DocumentDTO documentDTO = documentService.uploadDocument(userId, file, folderId);
        return ResponseEntity.status(HttpStatus.CREATED).body(documentDTO);
    }
    
    /**
     * 批量上传文档
     * 
     * @param files 上传的文件列表
     * @param folderId 文件夹ID（可选）
     * @param authentication 认证信息
     * @return 批量操作结果
     */
    @Operation(
            summary = "批量上传文档",
            description = "批量上传多个文档到系统。支持PDF、Word（.doc/.docx）、TXT格式，单个文件大小不超过100MB。需要JWT令牌认证。",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "批量上传完成（包含成功和失败的详细信息）",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = BatchOperationResult.class)
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
    @PostMapping("/batch-upload")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BatchOperationResult> batchUploadDocuments(
            @Parameter(description = "上传的文件列表", required = true)
            @RequestParam("files") @NotEmpty(message = "文件列表不能为空") List<MultipartFile> files,
            @Parameter(description = "文件夹ID（可选）")
            @RequestParam(value = "folderId", required = false) Long folderId,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("Batch upload documents request for userId: {}, file count: {}", userId, files.size());
        
        BatchOperationResult result = documentService.batchUploadDocuments(userId, files, folderId);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 获取文档列表
     * 
     * @param page 页码（从0开始）
     * @param size 每页数量
     * @param keyword 搜索关键词
     * @param fileType 文件类型筛选
     * @param folderId 文件夹ID筛选
     * @param sortBy 排序字段
     * @param sortDirection 排序方向
     * @param authentication 认证信息
     * @return 文档分页列表
     */
    @Operation(
            summary = "获取文档列表",
            description = "获取当前用户的文档列表，支持分页、搜索、筛选和排序。需要JWT令牌认证。",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "获取成功",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Page.class)
                    )
            )
    })
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<DocumentDTO>> getDocuments(
            @Parameter(description = "页码（从0开始）", example = "0")
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            @Parameter(description = "每页数量", example = "20")
            @RequestParam(value = "size", defaultValue = "20") Integer size,
            @Parameter(description = "搜索关键词（文件名）")
            @RequestParam(value = "keyword", required = false) String keyword,
            @Parameter(description = "文件类型筛选（pdf/docx/txt）")
            @RequestParam(value = "fileType", required = false) String fileType,
            @Parameter(description = "文件夹ID筛选")
            @RequestParam(value = "folderId", required = false) Long folderId,
            @Parameter(description = "排序字段", example = "createdAt")
            @RequestParam(value = "sortBy", defaultValue = "createdAt") String sortBy,
            @Parameter(description = "排序方向（ASC/DESC）", example = "DESC")
            @RequestParam(value = "sortDirection", defaultValue = "DESC") String sortDirection,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("Get documents request for userId: {}", userId);
        
        DocumentQueryRequest request = DocumentQueryRequest.builder()
                .page(page)
                .size(size)
                .keyword(keyword)
                .fileType(fileType)
                .folderId(folderId)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();
        
        Page<DocumentDTO> documents = documentService.getDocuments(userId, request);
        return ResponseEntity.ok(documents);
    }
    
    /**
     * 获取文档详情
     * 
     * @param id 文档ID
     * @param authentication 认证信息
     * @return 文档信息
     */
    @Operation(
            summary = "获取文档详情",
            description = "根据ID获取文档的详细信息。需要JWT令牌认证。",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "获取成功",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = DocumentDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "文档不存在",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "无权访问该文档",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DocumentDTO> getDocumentById(
            @Parameter(description = "文档ID", required = true, example = "1")
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("Get document by id request for userId: {}, documentId: {}", userId, id);
        
        DocumentDTO documentDTO = documentService.getDocumentById(userId, id);
        return ResponseEntity.ok(documentDTO);
    }
    
    /**
     * 预览文档
     * 
     * @param id 文档ID
     * @param authentication 认证信息
     * @return 预览响应
     */
    @Operation(
            summary = "预览文档",
            description = "在线预览文档内容。PDF返回URL，TXT返回文本内容，Word返回转换后的内容或下载链接。需要JWT令牌认证。",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "预览成功",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PreviewResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "文档不存在",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "无权访问该文档",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @GetMapping("/{id}/preview")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PreviewResponse> previewDocument(
            @Parameter(description = "文档ID", required = true, example = "1")
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("Preview document request for userId: {}, documentId: {}", userId, id);
        
        PreviewResponse previewResponse = documentService.previewDocument(userId, id);
        return ResponseEntity.ok(previewResponse);
    }
    
    /**
     * 下载文档
     * 
     * @param id 文档ID
     * @param authentication 认证信息
     * @return 文档文件
     */
    @Operation(
            summary = "下载文档",
            description = "下载文档文件到本地。需要JWT令牌认证。",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "下载成功",
                    content = @Content(
                            mediaType = "application/octet-stream"
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "文档不存在",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "无权访问该文档",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @GetMapping("/{id}/download")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> downloadDocument(
            @Parameter(description = "文档ID", required = true, example = "1")
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("Download document request for userId: {}, documentId: {}", userId, id);
        
        DocumentDTO documentDTO = documentService.getDocumentById(userId, id);
        byte[] fileContent = documentService.downloadDocument(userId, id);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", documentDTO.getOriginalFilename());
        headers.setContentLength(fileContent.length);
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(fileContent);
    }
    
    /**
     * 更新文档信息
     * 
     * @param id 文档ID
     * @param request 更新请求
     * @param authentication 认证信息
     * @return 更新后的文档信息
     */
    @Operation(
            summary = "更新文档信息",
            description = "更新文档信息，支持重命名和移动到其他文件夹。需要JWT令牌认证。",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "更新成功",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = DocumentDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "文档不存在",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "无权访问该文档",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DocumentDTO> updateDocument(
            @Parameter(description = "文档ID", required = true, example = "1")
            @PathVariable Long id,
            @Valid @RequestBody UpdateDocumentRequest request,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("Update document request for userId: {}, documentId: {}", userId, id);
        
        DocumentDTO documentDTO = documentService.updateDocument(userId, id, request);
        return ResponseEntity.ok(documentDTO);
    }
    
    /**
     * 删除文档
     * 
     * @param id 文档ID
     * @param authentication 认证信息
     * @return 删除结果
     */
    @Operation(
            summary = "删除文档",
            description = "删除指定的文档，同时删除数据库记录和文件系统中的文件。需要JWT令牌认证。",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "删除成功"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "文档不存在",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "无权访问该文档",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteDocument(
            @Parameter(description = "文档ID", required = true, example = "1")
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("Delete document request for userId: {}, documentId: {}", userId, id);
        
        documentService.deleteDocument(userId, id);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * 批量删除文档
     * 
     * @param documentIds 文档ID列表
     * @param authentication 认证信息
     * @return 批量操作结果
     */
    @Operation(
            summary = "批量删除文档",
            description = "批量删除多个文档。需要JWT令牌认证。",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "批量删除完成（包含成功和失败的详细信息）",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = BatchOperationResult.class)
                    )
            )
    })
    @DeleteMapping("/batch")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BatchOperationResult> batchDeleteDocuments(
            @Parameter(description = "文档ID列表", required = true)
            @RequestBody @NotEmpty(message = "文档ID列表不能为空") List<Long> documentIds,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("Batch delete documents request for userId: {}, document count: {}", userId, documentIds.size());
        
        BatchOperationResult result = documentService.batchDeleteDocuments(userId, documentIds);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 获取存储空间信息
     * 
     * @param authentication 认证信息
     * @return 存储空间信息
     */
    @Operation(
            summary = "获取存储空间信息",
            description = "获取当前用户的存储空间使用情况。需要JWT令牌认证。",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "获取成功",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StorageInfo.class)
                    )
            )
    })
    @GetMapping("/storage-info")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StorageInfo> getStorageInfo(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("Get storage info request for userId: {}", userId);
        
        StorageInfo storageInfo = documentService.getStorageInfo(userId);
        return ResponseEntity.ok(storageInfo);
    }
}
