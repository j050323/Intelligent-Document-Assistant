package com.docassistant.document.service;

import java.io.OutputStream;
import java.util.List;

/**
 * 文件压缩服务接口
 */
public interface CompressionService {
    
    /**
     * 将多个文档压缩为ZIP文件
     * 
     * @param userId 用户ID
     * @param documentIds 文档ID列表
     * @param outputStream 输出流
     */
    void compressDocuments(Long userId, List<Long> documentIds, OutputStream outputStream);
    
    /**
     * 将文件夹中的所有文档压缩为ZIP文件
     * 
     * @param userId 用户ID
     * @param folderId 文件夹ID
     * @param outputStream 输出流
     */
    void compressFolderDocuments(Long userId, Long folderId, OutputStream outputStream);
}
