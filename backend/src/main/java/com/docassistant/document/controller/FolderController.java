package com.docassistant.document.controller;

import com.docassistant.auth.dto.ErrorResponse;
import com.docassistant.document.dto.CreateFolderRequest;
import com.docassistant.document.dto.DocumentDTO;
import com.docassistant.document.dto.FolderDTO;
import com.docassistant.document.dto.UpdateFolderRequest;
import com.docassistant.document.service.FolderService;
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

import java.util.List;

/**
 * 文件夹管理API接口控制器
 */
@Tag(name = "文件夹管理", description = "文件夹创建、查询、更新、删除等文件夹管理相关接口")
@Slf4j
@RestController
@RequestMapping("/api/folders")
@RequiredArgsConstructor
@Validated
public class FolderController {
    
    private final FolderService folderService;
    
    /**
     * 创建文件夹
     * 
     * @param request 创建文件夹请求
     * @param authentication 认证信息
     * @return 文件夹信息
     */
    @Operation(
            summary = "创建文件夹",
            description = "创建新文件夹，支持在根目录或指定父文件夹下创建。需要JWT令牌认证。",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "创建成功",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = FolderDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "请求参数错误或文件夹名称重复",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "父文件夹不存在",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FolderDTO> createFolder(
            @Valid @RequestBody CreateFolderRequest request,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("Create folder request for userId: {}, folder name: {}", userId, request.getName());
        
        FolderDTO folderDTO = folderService.createFolder(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(folderDTO);
    }
    
    /**
     * 获取文件夹列表
     * 
     * @param parentId 父文件夹ID（可选，null表示获取根文件夹）
     * @param authentication 认证信息
     * @return 文件夹列表
     */
    @Operation(
            summary = "获取文件夹列表",
            description = "获取当前用户的文件夹列表。可以指定父文件夹ID获取子文件夹，不指定则获取根文件夹。需要JWT令牌认证。",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "获取成功",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = FolderDTO.class)
                    )
            )
    })
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<FolderDTO>> getFolders(
            @Parameter(description = "父文件夹ID（不指定则获取根文件夹）")
            @RequestParam(value = "parentId", required = false) Long parentId,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("Get folders request for userId: {}, parentId: {}", userId, parentId);
        
        List<FolderDTO> folders = folderService.getFolders(userId, parentId);
        return ResponseEntity.ok(folders);
    }
    
    /**
     * 获取文件夹详情
     * 
     * @param id 文件夹ID
     * @param authentication 认证信息
     * @return 文件夹信息
     */
    @Operation(
            summary = "获取文件夹详情",
            description = "根据ID获取文件夹的详细信息。需要JWT令牌认证。",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "获取成功",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = FolderDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "文件夹不存在",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "无权访问该文件夹",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FolderDTO> getFolderById(
            @Parameter(description = "文件夹ID", required = true, example = "1")
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("Get folder by id request for userId: {}, folderId: {}", userId, id);
        
        FolderDTO folderDTO = folderService.getFolderById(userId, id);
        return ResponseEntity.ok(folderDTO);
    }
    
    /**
     * 更新文件夹
     * 
     * @param id 文件夹ID
     * @param request 更新请求
     * @param authentication 认证信息
     * @return 更新后的文件夹信息
     */
    @Operation(
            summary = "更新文件夹",
            description = "更新文件夹信息（重命名）。需要JWT令牌认证。",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "更新成功",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = FolderDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "请求参数错误或文件夹名称重复",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "文件夹不存在",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "无权访问该文件夹",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FolderDTO> updateFolder(
            @Parameter(description = "文件夹ID", required = true, example = "1")
            @PathVariable Long id,
            @Valid @RequestBody UpdateFolderRequest request,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("Update folder request for userId: {}, folderId: {}", userId, id);
        
        FolderDTO folderDTO = folderService.updateFolder(userId, id, request);
        return ResponseEntity.ok(folderDTO);
    }
    
    /**
     * 删除文件夹
     * 
     * @param id 文件夹ID
     * @param authentication 认证信息
     * @return 删除结果
     */
    @Operation(
            summary = "删除文件夹",
            description = "删除指定的文件夹。只能删除空文件夹，如果文件夹中包含文档或子文件夹则拒绝删除。需要JWT令牌认证。",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "删除成功"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "文件夹不为空",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "文件夹不存在",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "无权访问该文件夹",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteFolder(
            @Parameter(description = "文件夹ID", required = true, example = "1")
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("Delete folder request for userId: {}, folderId: {}", userId, id);
        
        folderService.deleteFolder(userId, id);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * 获取文件夹中的文档
     * 
     * @param id 文件夹ID
     * @param authentication 认证信息
     * @return 文档列表
     */
    @Operation(
            summary = "获取文件夹中的文档",
            description = "获取指定文件夹中的所有文档。需要JWT令牌认证。",
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
                    description = "文件夹不存在",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "无权访问该文件夹",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @GetMapping("/{id}/documents")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DocumentDTO>> getDocumentsInFolder(
            @Parameter(description = "文件夹ID", required = true, example = "1")
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("Get documents in folder request for userId: {}, folderId: {}", userId, id);
        
        List<DocumentDTO> documents = folderService.getDocumentsInFolder(userId, id);
        return ResponseEntity.ok(documents);
    }
}
