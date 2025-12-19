package com.docassistant.document.service;

import com.docassistant.document.dto.BatchOperationResult;
import com.docassistant.document.dto.DocumentDTO;
import com.docassistant.document.dto.DocumentQueryRequest;
import com.docassistant.document.dto.PreviewResponse;
import com.docassistant.document.dto.StorageInfo;
import com.docassistant.document.dto.UpdateDocumentRequest;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文档管理服务接口
 */
public interface DocumentService {
    
    /**
     * 上传单个文档
     * @param userId 用户ID
     * @param file 上传的文件
     * @param folderId 文件夹ID（可选）
     * @return 文档DTO
     */
    DocumentDTO uploadDocument(Long userId, MultipartFile file, Long folderId);
    
    /**
     * 批量上传文档
     * @param userId 用户ID
     * @param files 上传的文件列表
     * @param folderId 文件夹ID（可选）
     * @return 批量操作结果
     */
    BatchOperationResult batchUploadDocuments(Long userId, List<MultipartFile> files, Long folderId);
    
    /**
     * 查询文档列表（支持分页、搜索、筛选、排序）
     * @param userId 用户ID
     * @param request 查询请求参数
     * @return 文档分页列表
     */
    Page<DocumentDTO> getDocuments(Long userId, DocumentQueryRequest request);
    
    /**
     * 根据ID获取文档详情
     * @param userId 用户ID
     * @param documentId 文档ID
     * @return 文档DTO
     */
    DocumentDTO getDocumentById(Long userId, Long documentId);
    
    /**
     * 预览文档
     * @param userId 用户ID
     * @param documentId 文档ID
     * @return 预览响应
     */
    PreviewResponse previewDocument(Long userId, Long documentId);
    
    /**
     * 更新文档信息（重命名或移动）
     * @param userId 用户ID
     * @param documentId 文档ID
     * @param request 更新请求
     * @return 更新后的文档DTO
     */
    DocumentDTO updateDocument(Long userId, Long documentId, UpdateDocumentRequest request);
    
    /**
     * 下载文档
     * @param userId 用户ID
     * @param documentId 文档ID
     * @return 文件字节数组
     */
    byte[] downloadDocument(Long userId, Long documentId);
    
    /**
     * 删除文档
     * @param userId 用户ID
     * @param documentId 文档ID
     */
    void deleteDocument(Long userId, Long documentId);
    
    /**
     * 批量删除文档
     * @param userId 用户ID
     * @param documentIds 文档ID列表
     * @return 批量操作结果
     */
    BatchOperationResult batchDeleteDocuments(Long userId, List<Long> documentIds);
    
    /**
     * 获取存储空间信息
     * @param userId 用户ID
     * @return 存储空间信息
     */
    StorageInfo getStorageInfo(Long userId);
}
