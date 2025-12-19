package com.docassistant.document.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件存储服务接口
 * 负责文件的物理存储、读取和删除
 */
public interface FileStorageService {
    
    /**
     * 存储文件
     * @param file 要存储的文件
     * @param userId 用户ID
     * @return 文件存储路径
     */
    String storeFile(MultipartFile file, Long userId);
    
    /**
     * 存储文件（从File对象）
     * @param file 要存储的文件
     * @param userId 用户ID
     * @param originalFilename 原始文件名
     * @return 文件存储路径
     */
    String storeFile(java.io.File file, Long userId, String originalFilename);
    
    /**
     * 读取文件
     * @param filePath 文件路径
     * @return 文件内容字节数组
     */
    byte[] loadFile(String filePath);
    
    /**
     * 删除文件
     * @param filePath 文件路径
     */
    void deleteFile(String filePath);
    
    /**
     * 生成唯一文件名
     * @param originalFilename 原始文件名
     * @param userId 用户ID
     * @return 唯一文件名
     */
    String generateUniqueFileName(String originalFilename, Long userId);
    
    /**
     * 检查文件是否存在
     * @param filePath 文件路径
     * @return 文件是否存在
     */
    boolean fileExists(String filePath);
}
