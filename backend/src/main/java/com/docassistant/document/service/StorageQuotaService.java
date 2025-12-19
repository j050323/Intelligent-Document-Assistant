package com.docassistant.document.service;

import com.docassistant.document.dto.StorageInfo;

/**
 * 存储配额管理服务接口
 */
public interface StorageQuotaService {
    
    /**
     * 检查用户是否有足够的存储配额
     * @param userId 用户ID
     * @param fileSize 文件大小（字节）
     * @return 是否有足够配额
     */
    boolean checkQuota(Long userId, long fileSize);
    
    /**
     * 增加用户的存储使用量
     * @param userId 用户ID
     * @param fileSize 文件大小（字节）
     */
    void increaseUsage(Long userId, long fileSize);
    
    /**
     * 减少用户的存储使用量
     * @param userId 用户ID
     * @param fileSize 文件大小（字节）
     */
    void decreaseUsage(Long userId, long fileSize);
    
    /**
     * 获取用户的存储空间信息
     * @param userId 用户ID
     * @return 存储空间信息
     */
    StorageInfo getStorageInfo(Long userId);
    
    /**
     * 更新用户的存储配额
     * @param userId 用户ID
     * @param newQuota 新配额（字节）
     */
    void updateQuota(Long userId, long newQuota);
}
