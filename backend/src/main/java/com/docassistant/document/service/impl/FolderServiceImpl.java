package com.docassistant.document.service.impl;

import com.docassistant.document.dto.CreateFolderRequest;
import com.docassistant.document.dto.DocumentDTO;
import com.docassistant.document.dto.FolderDTO;
import com.docassistant.document.dto.UpdateFolderRequest;
import com.docassistant.document.entity.Document;
import com.docassistant.document.entity.Folder;
import com.docassistant.document.exception.DuplicateFolderNameException;
import com.docassistant.document.exception.FolderNotEmptyException;
import com.docassistant.document.exception.FolderNotFoundException;
import com.docassistant.document.repository.DocumentRepository;
import com.docassistant.document.repository.FolderRepository;
import com.docassistant.document.service.FolderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 文件夹管理服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FolderServiceImpl implements FolderService {
    
    private final FolderRepository folderRepository;
    private final DocumentRepository documentRepository;
    
    @Override
    @Transactional
    @CacheEvict(value = "folderTree", key = "#userId")
    public FolderDTO createFolder(Long userId, CreateFolderRequest request) {
        log.info("创建文件夹，用户ID: {}, 文件夹名称: {}, 父文件夹ID: {}", 
                 userId, request.getName(), request.getParentId());
        
        // 验证文件夹名称是否重复
        validateFolderName(userId, request.getParentId(), request.getName());
        
        // 计算文件夹路径
        String path = calculateFolderPath(userId, request.getParentId(), request.getName());
        
        // 创建文件夹实体
        Folder folder = Folder.builder()
                .userId(userId)
                .parentId(request.getParentId())
                .name(request.getName())
                .path(path)
                .build();
        
        folder = folderRepository.save(folder);
        
        log.info("文件夹创建成功，文件夹ID: {}, 用户ID: {}", folder.getId(), userId);
        
        return convertToDTO(folder);
    }
    
    /**
     * 验证文件夹名称是否重复
     */
    private void validateFolderName(Long userId, Long parentId, String name) {
        boolean exists;
        
        if (parentId == null) {
            // 检查根目录下是否存在同名文件夹
            exists = folderRepository.existsByUserIdAndNullParentIdAndName(userId, name);
        } else {
            // 检查指定父文件夹下是否存在同名文件夹
            exists = folderRepository.existsByUserIdAndParentIdAndName(userId, parentId, name);
        }
        
        if (exists) {
            throw new DuplicateFolderNameException(
                    String.format("文件夹名称 '%s' 已存在", name));
        }
    }
    
    /**
     * 计算文件夹路径
     */
    private String calculateFolderPath(Long userId, Long parentId, String folderName) {
        if (parentId == null) {
            // 根文件夹，路径就是文件夹名称
            return "/" + folderName;
        }
        
        // 获取父文件夹
        Folder parentFolder = folderRepository.findByIdAndUserId(parentId, userId)
                .orElseThrow(() -> new FolderNotFoundException("父文件夹不存在"));
        
        // 拼接路径
        String parentPath = parentFolder.getPath();
        if (parentPath == null || parentPath.isEmpty()) {
            return "/" + folderName;
        }
        
        // 确保路径格式正确
        if (parentPath.endsWith("/")) {
            return parentPath + folderName;
        } else {
            return parentPath + "/" + folderName;
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "folderTree", key = "#userId + '_' + #parentId")
    public List<FolderDTO> getFolders(Long userId, Long parentId) {
        log.info("查询文件夹列表，用户ID: {}, 父文件夹ID: {}", userId, parentId);
        
        List<Folder> folders;
        
        if (parentId == null) {
            // 查询根文件夹
            folders = folderRepository.findByUserIdAndParentIdIsNull(userId);
        } else {
            // 查询指定父文件夹下的子文件夹
            folders = folderRepository.findByUserIdAndParentId(userId, parentId);
        }
        
        log.info("查询文件夹列表完成，用户ID: {}, 数量: {}", userId, folders.size());
        
        return folders.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public FolderDTO getFolderById(Long userId, Long folderId) {
        log.info("查询文件夹详情，用户ID: {}, 文件夹ID: {}", userId, folderId);
        
        Folder folder = folderRepository.findByIdAndUserId(folderId, userId)
                .orElseThrow(() -> new FolderNotFoundException("文件夹不存在或无权访问"));
        
        log.info("查询文件夹详情成功，文件夹ID: {}", folderId);
        
        return convertToDTO(folder);
    }
    
    @Override
    @Transactional
    @CacheEvict(value = "folderTree", key = "#userId")
    public FolderDTO updateFolder(Long userId, Long folderId, UpdateFolderRequest request) {
        log.info("更新文件夹，用户ID: {}, 文件夹ID: {}, 新名称: {}", 
                 userId, folderId, request.getName());
        
        // 查询文件夹并验证权限
        Folder folder = folderRepository.findByIdAndUserId(folderId, userId)
                .orElseThrow(() -> new FolderNotFoundException("文件夹不存在或无权访问"));
        
        // 如果名称没有变化，直接返回
        if (folder.getName().equals(request.getName())) {
            log.info("文件夹名称未变化，无需更新，文件夹ID: {}", folderId);
            return convertToDTO(folder);
        }
        
        // 验证新名称是否重复
        validateFolderName(userId, folder.getParentId(), request.getName());
        
        // 更新文件夹名称
        String oldName = folder.getName();
        folder.setName(request.getName());
        
        // 重新计算路径
        String newPath = calculateFolderPath(userId, folder.getParentId(), request.getName());
        String oldPath = folder.getPath();
        folder.setPath(newPath);
        
        folder = folderRepository.save(folder);
        
        // 更新所有子文件夹的路径
        updateChildFolderPaths(folderId, oldPath, newPath);
        
        log.info("文件夹更新成功，文件夹ID: {}, 旧名称: {}, 新名称: {}", 
                 folderId, oldName, request.getName());
        
        return convertToDTO(folder);
    }
    
    /**
     * 递归更新子文件夹的路径
     */
    private void updateChildFolderPaths(Long parentId, String oldParentPath, String newParentPath) {
        List<Folder> childFolders = folderRepository.findByParentId(parentId);
        
        for (Folder child : childFolders) {
            // 替换路径中的父路径部分
            String oldChildPath = child.getPath();
            if (oldChildPath != null && oldChildPath.startsWith(oldParentPath)) {
                String newChildPath = oldChildPath.replace(oldParentPath, newParentPath);
                child.setPath(newChildPath);
                folderRepository.save(child);
                
                // 递归更新子文件夹的子文件夹
                updateChildFolderPaths(child.getId(), oldChildPath, newChildPath);
            }
        }
    }
    
    @Override
    @Transactional
    @CacheEvict(value = "folderTree", key = "#userId")
    public void deleteFolder(Long userId, Long folderId) {
        log.info("删除文件夹，用户ID: {}, 文件夹ID: {}", userId, folderId);
        
        // 查询文件夹并验证权限
        Folder folder = folderRepository.findByIdAndUserId(folderId, userId)
                .orElseThrow(() -> new FolderNotFoundException("文件夹不存在或无权访问"));
        
        // 检查文件夹是否为空（不包含文档）
        long documentCount = documentRepository.countByUserIdAndFolderId(userId, folderId);
        if (documentCount > 0) {
            throw new FolderNotEmptyException(
                    String.format("文件夹 '%s' 不为空，包含 %d 个文档，无法删除", 
                                  folder.getName(), documentCount));
        }
        
        // 检查文件夹是否为空（不包含子文件夹）
        List<Folder> childFolders = folderRepository.findByParentId(folderId);
        if (!childFolders.isEmpty()) {
            throw new FolderNotEmptyException(
                    String.format("文件夹 '%s' 不为空，包含 %d 个子文件夹，无法删除", 
                                  folder.getName(), childFolders.size()));
        }
        
        // 删除文件夹
        folderRepository.delete(folder);
        
        log.info("文件夹删除成功，文件夹ID: {}, 用户ID: {}", folderId, userId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<DocumentDTO> getDocumentsInFolder(Long userId, Long folderId) {
        log.info("查询文件夹中的文档，用户ID: {}, 文件夹ID: {}", userId, folderId);
        
        // 验证文件夹存在且用户有权限
        folderRepository.findByIdAndUserId(folderId, userId)
                .orElseThrow(() -> new FolderNotFoundException("文件夹不存在或无权访问"));
        
        // 查询文件夹中的所有文档
        List<Document> documents = documentRepository.findByUserIdAndFolderId(userId, folderId);
        
        log.info("查询文件夹中的文档完成，用户ID: {}, 文件夹ID: {}, 文档数量: {}", 
                 userId, folderId, documents.size());
        
        return documents.stream()
                .map(this::convertDocumentToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 将Folder实体转换为DTO
     */
    private FolderDTO convertToDTO(Folder folder) {
        return FolderDTO.builder()
                .id(folder.getId())
                .name(folder.getName())
                .parentId(folder.getParentId())
                .path(folder.getPath())
                .createdAt(folder.getCreatedAt())
                .updatedAt(folder.getUpdatedAt())
                .build();
    }
    
    /**
     * 将Document实体转换为DTO
     */
    private DocumentDTO convertDocumentToDTO(Document document) {
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
