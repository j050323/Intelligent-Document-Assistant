package com.docassistant.document.service;

import com.docassistant.document.dto.CreateFolderRequest;
import com.docassistant.document.dto.DocumentDTO;
import com.docassistant.document.dto.FolderDTO;
import com.docassistant.document.dto.UpdateFolderRequest;

import java.util.List;

/**
 * 文件夹管理服务接口
 */
public interface FolderService {
    
    /**
     * 创建文件夹
     * @param userId 用户ID
     * @param request 创建文件夹请求
     * @return 文件夹DTO
     */
    FolderDTO createFolder(Long userId, CreateFolderRequest request);
    
    /**
     * 获取文件夹列表
     * @param userId 用户ID
     * @param parentId 父文件夹ID（null表示获取根文件夹）
     * @return 文件夹列表
     */
    List<FolderDTO> getFolders(Long userId, Long parentId);
    
    /**
     * 根据ID获取文件夹详情
     * @param userId 用户ID
     * @param folderId 文件夹ID
     * @return 文件夹DTO
     */
    FolderDTO getFolderById(Long userId, Long folderId);
    
    /**
     * 更新文件夹（重命名）
     * @param userId 用户ID
     * @param folderId 文件夹ID
     * @param request 更新文件夹请求
     * @return 更新后的文件夹DTO
     */
    FolderDTO updateFolder(Long userId, Long folderId, UpdateFolderRequest request);
    
    /**
     * 删除文件夹
     * @param userId 用户ID
     * @param folderId 文件夹ID
     */
    void deleteFolder(Long userId, Long folderId);
    
    /**
     * 获取文件夹中的文档
     * @param userId 用户ID
     * @param folderId 文件夹ID
     * @return 文档列表
     */
    List<DocumentDTO> getDocumentsInFolder(Long userId, Long folderId);
}
