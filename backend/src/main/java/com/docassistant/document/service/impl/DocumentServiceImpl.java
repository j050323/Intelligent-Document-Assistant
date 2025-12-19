package com.docassistant.document.service.impl;

import com.docassistant.document.dto.BatchOperationResult;
import com.docassistant.document.dto.DocumentDTO;
import com.docassistant.document.dto.DocumentQueryRequest;
import com.docassistant.document.dto.OperationError;
import com.docassistant.document.dto.PreviewResponse;
import com.docassistant.document.dto.StorageInfo;
import com.docassistant.document.dto.UpdateDocumentRequest;
import com.docassistant.document.entity.Document;
import com.docassistant.document.exception.FileSizeExceededException;
import com.docassistant.document.exception.FileUploadException;
import com.docassistant.document.exception.StorageQuotaExceededException;
import com.docassistant.document.exception.UnsupportedFileFormatException;
import com.docassistant.document.repository.DocumentRepository;
import com.docassistant.document.service.DocumentPreviewService;
import com.docassistant.document.service.DocumentService;
import com.docassistant.document.service.FileStorageService;
import com.docassistant.document.service.StorageQuotaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 文档管理服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {
    
    private final DocumentRepository documentRepository;
    private final FileStorageService fileStorageService;
    private final StorageQuotaService storageQuotaService;
    private final DocumentPreviewService documentPreviewService;
    private final com.docassistant.auth.service.SystemLogService systemLogService;
    
    // 支持的文件格式
    private static final List<String> SUPPORTED_FORMATS = Arrays.asList("pdf", "doc", "docx", "txt");
    
    // 最大文件大小：100MB
    private static final long MAX_FILE_SIZE = 100 * 1024 * 1024;
    
    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "documentList", key = "#userId"),
            @CacheEvict(value = "storageInfo", key = "#userId")
    })
    public DocumentDTO uploadDocument(Long userId, MultipartFile file, Long folderId) {
        log.info("开始上传文档，用户ID: {}, 文件名: {}, 文件夹ID: {}", 
                 userId, file.getOriginalFilename(), folderId);
        
        // 1. 验证文件不为空
        if (file == null || file.isEmpty()) {
            throw new FileUploadException("文件不能为空");
        }
        
        // 2. 验证文件大小
        validateFileSize(file);
        
        // 3. 验证文件格式
        String fileType = validateAndGetFileType(file);
        
        // 4. 检查存储配额
        checkStorageQuota(userId, file.getSize());
        
        try {
            // 5. 存储文件到文件系统
            String filePath = fileStorageService.storeFile(file, userId);
            
            // 6. 创建文档记录
            Document document = Document.builder()
                    .userId(userId)
                    .folderId(folderId)
                    .filename(fileStorageService.generateUniqueFileName(file.getOriginalFilename(), userId))
                    .originalFilename(file.getOriginalFilename())
                    .filePath(filePath)
                    .fileType(fileType)
                    .fileSize(file.getSize())
                    .mimeType(file.getContentType())
                    .build();
            
            document = documentRepository.save(document);
            
            // 7. 更新存储使用量
            storageQuotaService.increaseUsage(userId, file.getSize());
            
            log.info("文档上传成功，文档ID: {}, 用户ID: {}", document.getId(), userId);
            
            // 8. 转换为DTO并返回
            return convertToDTO(document);
            
        } catch (Exception e) {
            log.error("文档上传失败，用户ID: {}, 文件名: {}", userId, file.getOriginalFilename(), e);
            throw new FileUploadException("文件上传失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 验证文件大小
     */
    private void validateFileSize(MultipartFile file) {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileSizeExceededException(
                    String.format("文件大小超过限制（最大100MB），当前文件大小: %.2f MB", 
                                  file.getSize() / (1024.0 * 1024.0)));
        }
    }
    
    /**
     * 验证文件格式并返回文件类型
     */
    private String validateAndGetFileType(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new UnsupportedFileFormatException("无法识别文件格式");
        }
        
        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        
        if (!SUPPORTED_FORMATS.contains(extension)) {
            throw new UnsupportedFileFormatException(
                    String.format("不支持的文件格式: %s，支持的格式: PDF, Word (.doc/.docx), TXT", extension));
        }
        
        return extension;
    }
    
    /**
     * 检查存储配额
     */
    private void checkStorageQuota(Long userId, long fileSize) {
        if (!storageQuotaService.checkQuota(userId, fileSize)) {
            throw new StorageQuotaExceededException("存储空间不足，无法上传文件");
        }
    }
    
    @Override
    @Transactional
    public BatchOperationResult batchUploadDocuments(Long userId, List<MultipartFile> files, Long folderId) {
        log.info("开始批量上传文档，用户ID: {}, 文件数量: {}, 文件夹ID: {}", 
                 userId, files.size(), folderId);
        
        List<DocumentDTO> successDocuments = new ArrayList<>();
        List<Long> successIds = new ArrayList<>();
        List<OperationError> errors = new ArrayList<>();
        
        // 依次处理每个文件
        for (MultipartFile file : files) {
            try {
                // 上传单个文档
                DocumentDTO document = uploadDocument(userId, file, folderId);
                successDocuments.add(document);
                successIds.add(document.getId());
                
            } catch (UnsupportedFileFormatException | FileSizeExceededException | 
                     StorageQuotaExceededException | FileUploadException e) {
                // 记录错误但继续处理其他文件（错误隔离）
                log.warn("文件上传失败，文件名: {}, 错误: {}", file.getOriginalFilename(), e.getMessage());
                
                OperationError error = OperationError.builder()
                        .filename(file.getOriginalFilename())
                        .errorMessage(e.getMessage())
                        .errorCode(e instanceof com.docassistant.auth.exception.BusinessException ? 
                                   ((com.docassistant.auth.exception.BusinessException) e).getErrorCode() : 
                                   "UNKNOWN_ERROR")
                        .build();
                errors.add(error);
                
            } catch (Exception e) {
                // 捕获其他未预期的异常
                log.error("文件上传发生未预期错误，文件名: {}", file.getOriginalFilename(), e);
                
                OperationError error = OperationError.builder()
                        .filename(file.getOriginalFilename())
                        .errorMessage("文件上传失败: " + e.getMessage())
                        .errorCode("UNKNOWN_ERROR")
                        .build();
                errors.add(error);
            }
        }
        
        log.info("批量上传完成，用户ID: {}, 成功: {}, 失败: {}", 
                 userId, successDocuments.size(), errors.size());
        
        return BatchOperationResult.builder()
                .successCount(successDocuments.size())
                .failureCount(errors.size())
                .successIds(successIds)
                .successDocuments(successDocuments)
                .errors(errors)
                .build();
    }
    
    @Override
    @Transactional(readOnly = true)
    // Note: Caching Page objects causes serialization issues with Redis
    // @Cacheable(value = "documentList", key = "#userId + '_' + #request.page + '_' + #request.size + '_' + #request.keyword + '_' + #request.fileType + '_' + #request.folderId + '_' + #request.sortBy + '_' + #request.sortDirection")
    public Page<DocumentDTO> getDocuments(Long userId, DocumentQueryRequest request) {
        log.info("查询文档列表，用户ID: {}, 查询参数: {}", userId, request);
        
        // 构建分页和排序参数
        Pageable pageable = buildPageable(request);
        
        // 根据不同的查询条件执行查询
        Page<Document> documentPage;
        
        if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty()) {
            // 按文件名搜索
            documentPage = documentRepository.searchByFilename(userId, request.getKeyword().trim(), pageable);
            
        } else if (request.getFolderId() != null && request.getFileType() != null && !request.getFileType().trim().isEmpty()) {
            // 同时按文件夹和文件类型筛选
            documentPage = documentRepository.findByUserIdAndFolderIdAndFileType(
                    userId, request.getFolderId(), request.getFileType().toLowerCase(), pageable);
            
        } else if (request.getFolderId() != null) {
            // 按文件夹筛选
            documentPage = documentRepository.findByUserIdAndFolderId(userId, request.getFolderId(), pageable);
            
        } else if (request.getFileType() != null && !request.getFileType().trim().isEmpty()) {
            // 按文件类型筛选
            documentPage = documentRepository.findByUserIdAndFileType(
                    userId, request.getFileType().toLowerCase(), pageable);
            
        } else {
            // 查询所有文档
            documentPage = documentRepository.findByUserId(userId, pageable);
        }
        
        // 转换为DTO
        Page<DocumentDTO> dtoPage = documentPage.map(this::convertToDTO);
        
        log.info("查询文档列表完成，用户ID: {}, 总数: {}, 当前页: {}", 
                 userId, dtoPage.getTotalElements(), dtoPage.getNumber());
        
        return dtoPage;
    }
    
    @Override
    @Transactional(readOnly = true)
    // Note: Caching DTO objects with LocalDateTime causes serialization issues with Redis
    // @Cacheable(value = "documentDetail", key = "#userId + '_' + #documentId")
    public DocumentDTO getDocumentById(Long userId, Long documentId) {
        log.info("查询文档详情，用户ID: {}, 文档ID: {}", userId, documentId);
        
        Document document = documentRepository.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> new FileUploadException("文档不存在或无权访问"));
        
        log.info("查询文档详情成功，文档ID: {}", documentId);
        
        return convertToDTO(document);
    }
    
    /**
     * 构建分页和排序参数
     */
    private Pageable buildPageable(DocumentQueryRequest request) {
        // 验证排序字段，防止SQL注入
        String sortBy = validateSortField(request.getSortBy());
        
        // 构建排序对象
        Sort.Direction direction = "ASC".equalsIgnoreCase(request.getSortDirection()) 
                ? Sort.Direction.ASC 
                : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, sortBy);
        
        // 构建分页对象
        return PageRequest.of(request.getPage(), request.getSize(), sort);
    }
    
    /**
     * 验证排序字段，只允许特定字段排序
     */
    private String validateSortField(String sortBy) {
        if (sortBy == null || sortBy.trim().isEmpty()) {
            return "createdAt";
        }
        
        // 允许的排序字段
        List<String> allowedFields = Arrays.asList(
                "createdAt", "updatedAt", "filename", "originalFilename", 
                "fileSize", "fileType");
        
        String field = sortBy.trim();
        if (allowedFields.contains(field)) {
            return field;
        }
        
        // 默认按创建时间排序
        log.warn("无效的排序字段: {}, 使用默认排序字段: createdAt", sortBy);
        return "createdAt";
    }
    
    @Override
    public PreviewResponse previewDocument(Long userId, Long documentId) {
        log.info("预览文档，用户ID: {}, 文档ID: {}", userId, documentId);
        
        // 查询文档并验证权限
        Document document = documentRepository.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> new FileUploadException("文档不存在或无权访问"));
        
        // 根据文件类型调用相应的预览方法
        String fileType = document.getFileType().toLowerCase();
        PreviewResponse response;
        
        try {
            switch (fileType) {
                case "pdf":
                    response = documentPreviewService.previewPdf(document.getFilePath());
                    break;
                case "txt":
                    response = documentPreviewService.previewText(document.getFilePath());
                    break;
                case "doc":
                case "docx":
                    response = documentPreviewService.previewWord(document.getFilePath());
                    break;
                default:
                    throw new FileUploadException("不支持预览该文件类型: " + fileType);
            }
            
            // 设置文件名
            response.setFilename(document.getOriginalFilename());
            
            log.info("预览文档成功，文档ID: {}, 类型: {}", documentId, fileType);
            return response;
            
        } catch (Exception e) {
            log.error("预览文档失败，文档ID: {}, 错误: {}", documentId, e.getMessage());
            throw new FileUploadException("预览文档失败: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "documentList", key = "#userId"),
            @CacheEvict(value = "documentDetail", key = "#userId + '_' + #documentId")
    })
    public DocumentDTO updateDocument(Long userId, Long documentId, UpdateDocumentRequest request) {
        log.info("更新文档，用户ID: {}, 文档ID: {}, 请求: {}", userId, documentId, request);
        
        // 查询文档并验证权限
        Document document = documentRepository.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> new FileUploadException("文档不存在或无权访问"));
        
        boolean updated = false;
        boolean moved = false;
        Long oldFolderId = document.getFolderId();
        
        // 重命名文档
        if (request.getFilename() != null && !request.getFilename().trim().isEmpty()) {
            String newFilename = request.getFilename().trim();
            if (!newFilename.equals(document.getOriginalFilename())) {
                document.setOriginalFilename(newFilename);
                updated = true;
                log.info("文档重命名，文档ID: {}, 新文件名: {}", documentId, newFilename);
            }
        }
        
        // 移动文档到新文件夹
        if (request.getFolderId() != null) {
            if (!request.getFolderId().equals(document.getFolderId())) {
                document.setFolderId(request.getFolderId());
                updated = true;
                moved = true;
                log.info("文档移动，文档ID: {}, 新文件夹ID: {}", documentId, request.getFolderId());
            }
        }
        
        if (updated) {
            document = documentRepository.save(document);
            
            // 如果是移动操作，记录操作日志
            if (moved) {
                String beforeState = String.format("文件夹ID: %s", oldFolderId);
                String afterState = String.format("文件夹ID: %s", document.getFolderId());
                systemLogService.logDocumentMove(userId, documentId, beforeState, afterState);
            }
            
            log.info("文档更新成功，文档ID: {}", documentId);
        } else {
            log.info("文档无需更新，文档ID: {}", documentId);
        }
        
        return convertToDTO(document);
    }
    
    @Override
    @Transactional(readOnly = true)
    public byte[] downloadDocument(Long userId, Long documentId) {
        log.info("下载文档，用户ID: {}, 文档ID: {}", userId, documentId);
        
        // 查询文档并验证权限
        Document document = documentRepository.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> new FileUploadException("文档不存在或无权访问"));
        
        try {
            // 从文件存储服务加载文件
            byte[] fileContent = fileStorageService.loadFile(document.getFilePath());
            
            log.info("文档下载成功，文档ID: {}, 文件大小: {} bytes", documentId, fileContent.length);
            return fileContent;
            
        } catch (Exception e) {
            log.error("文档下载失败，文档ID: {}, 错误: {}", documentId, e.getMessage());
            throw new FileUploadException("文档下载失败: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "documentList", key = "#userId"),
            @CacheEvict(value = "documentDetail", key = "#userId + '_' + #documentId"),
            @CacheEvict(value = "storageInfo", key = "#userId")
    })
    public void deleteDocument(Long userId, Long documentId) {
        log.info("删除文档，用户ID: {}, 文档ID: {}", userId, documentId);
        
        // 查询文档并验证权限
        Document document = documentRepository.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> new FileUploadException("文档不存在或无权访问"));
        
        // 记录删除前的状态
        String beforeState = String.format("文件名: %s, 文件类型: %s, 文件大小: %d bytes, 文件夹ID: %s", 
                                          document.getOriginalFilename(), 
                                          document.getFileType(), 
                                          document.getFileSize(),
                                          document.getFolderId());
        
        try {
            // 1. 从文件系统删除文件
            fileStorageService.deleteFile(document.getFilePath());
            
            // 2. 从数据库删除记录
            documentRepository.delete(document);
            
            // 3. 更新存储使用量
            storageQuotaService.decreaseUsage(userId, document.getFileSize());
            
            // 4. 记录操作日志
            systemLogService.logDocumentDelete(userId, documentId, beforeState);
            
            log.info("文档删除成功，文档ID: {}, 用户ID: {}", documentId, userId);
            
        } catch (Exception e) {
            log.error("文档删除失败，文档ID: {}, 错误: {}", documentId, e.getMessage());
            
            // 记录失败日志
            systemLogService.logDocumentOperation(userId, "DOCUMENT_DELETE", documentId, 
                                                 beforeState, "FAILURE", e.getMessage());
            
            throw new FileUploadException("文档删除失败: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "documentList", key = "#userId"),
            @CacheEvict(value = "storageInfo", key = "#userId")
    })
    public BatchOperationResult batchDeleteDocuments(Long userId, List<Long> documentIds) {
        log.info("批量删除文档，用户ID: {}, 文档数量: {}", userId, documentIds.size());
        
        List<Long> successIds = new ArrayList<>();
        List<OperationError> errors = new ArrayList<>();
        
        // 依次处理每个文档
        for (Long documentId : documentIds) {
            try {
                // 查询文档并验证权限
                Document document = documentRepository.findByIdAndUserId(documentId, userId)
                        .orElseThrow(() -> new FileUploadException("文档不存在或无权访问"));
                
                // 记录删除前的状态
                String beforeState = String.format("文件名: %s, 文件类型: %s, 文件大小: %d bytes", 
                                                  document.getOriginalFilename(), 
                                                  document.getFileType(), 
                                                  document.getFileSize());
                
                // 删除文档
                fileStorageService.deleteFile(document.getFilePath());
                documentRepository.delete(document);
                storageQuotaService.decreaseUsage(userId, document.getFileSize());
                
                // 记录操作日志
                systemLogService.logDocumentDelete(userId, documentId, beforeState);
                
                successIds.add(documentId);
                log.info("文档删除成功，文档ID: {}", documentId);
                
            } catch (FileUploadException e) {
                // 记录错误但继续处理其他文档（错误隔离）
                log.warn("文档删除失败，文档ID: {}, 错误: {}", documentId, e.getMessage());
                
                // 记录失败日志
                systemLogService.logDocumentOperation(userId, "DOCUMENT_DELETE", documentId, 
                                                     "", "FAILURE", e.getMessage());
                
                OperationError error = OperationError.builder()
                        .documentId(documentId)
                        .errorMessage(e.getMessage())
                        .errorCode("DOCUMENT_DELETE_FAILED")
                        .build();
                errors.add(error);
                
            } catch (Exception e) {
                // 捕获其他未预期的异常
                log.error("文档删除发生未预期错误，文档ID: {}", documentId, e);
                
                // 记录失败日志
                systemLogService.logDocumentOperation(userId, "DOCUMENT_DELETE", documentId, 
                                                     "", "FAILURE", e.getMessage());
                
                OperationError error = OperationError.builder()
                        .documentId(documentId)
                        .errorMessage("文档删除失败: " + e.getMessage())
                        .errorCode("UNKNOWN_ERROR")
                        .build();
                errors.add(error);
            }
        }
        
        log.info("批量删除完成，用户ID: {}, 成功: {}, 失败: {}", 
                 userId, successIds.size(), errors.size());
        
        return BatchOperationResult.builder()
                .successCount(successIds.size())
                .failureCount(errors.size())
                .successIds(successIds)
                .errors(errors)
                .build();
    }
    
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "storageInfo", key = "#userId")
    public StorageInfo getStorageInfo(Long userId) {
        log.info("获取存储空间信息，用户ID: {}", userId);
        
        StorageInfo storageInfo = storageQuotaService.getStorageInfo(userId);
        
        log.info("获取存储空间信息成功，用户ID: {}, 已使用: {} bytes, 总配额: {} bytes", 
                 userId, storageInfo.getUsedSpace(), storageInfo.getTotalQuota());
        
        return storageInfo;
    }
    
    /**
     * 将Document实体转换为DTO
     */
    private DocumentDTO convertToDTO(Document document) {
        return DocumentDTO.builder()
                .id(document.getId())
                .filename(document.getFilename())
                .originalFilename(document.getOriginalFilename())
                .fileType(document.getFileType())
                .fileSize(document.getFileSize())
                .mimeType(document.getMimeType())
                .folderId(document.getFolderId())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }
}
