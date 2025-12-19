package com.docassistant.document.service.impl;

import com.docassistant.document.dto.StorageInfo;
import com.docassistant.document.repository.DocumentRepository;
import com.docassistant.document.service.StorageQuotaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 存储配额管理服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StorageQuotaServiceImpl implements StorageQuotaService {
    
    private final DocumentRepository documentRepository;
    
    /**
     * 默认存储配额：1GB
     */
    @Value("${app.storage.default-quota:1073741824}")
    private long defaultQuota;
    
    @Override
    public boolean checkQuota(Long userId, long fileSize) {
        StorageInfo storageInfo = getStorageInfo(userId);
        long availableSpace = storageInfo.getRemainingSpace();
        
        boolean hasEnoughSpace = availableSpace >= fileSize;
        
        if (!hasEnoughSpace) {
            log.warn("用户 {} 存储空间不足。需要: {} 字节, 可用: {} 字节", 
                userId, fileSize, availableSpace);
        }
        
        return hasEnoughSpace;
    }
    
    @Override
    @Transactional
    public void increaseUsage(Long userId, long fileSize) {
        // 注意：实际使用量是通过DocumentRepository计算的
        // 这个方法主要用于日志记录和验证
        log.debug("用户 {} 增加存储使用量: {} 字节", userId, fileSize);
    }
    
    @Override
    @Transactional
    public void decreaseUsage(Long userId, long fileSize) {
        // 注意：实际使用量是通过DocumentRepository计算的
        // 这个方法主要用于日志记录和验证
        log.debug("用户 {} 减少存储使用量: {} 字节", userId, fileSize);
    }
    
    @Override
    public StorageInfo getStorageInfo(Long userId) {
        // 从数据库计算实际使用量
        Long usedSpace = documentRepository.calculateTotalStorageUsed(userId);
        
        // 获取用户配额（目前使用默认值，后续可以从用户统计表获取）
        long totalQuota = defaultQuota;
        
        // 计算剩余空间
        long remainingSpace = Math.max(0, totalQuota - usedSpace);
        
        // 计算使用百分比
        double usagePercentage = totalQuota > 0 
            ? (usedSpace * 100.0 / totalQuota) 
            : 0.0;
        
        // 判断是否接近限制（90%以上）
        boolean nearLimit = usagePercentage >= 90.0;
        
        return StorageInfo.builder()
            .usedSpace(usedSpace)
            .totalQuota(totalQuota)
            .remainingSpace(remainingSpace)
            .usagePercentage(usagePercentage)
            .nearLimit(nearLimit)
            .build();
    }
    
    @Override
    @Transactional
    public void updateQuota(Long userId, long newQuota) {
        // TODO: 实现用户配额更新逻辑
        // 需要在用户统计表中存储配额信息
        log.info("更新用户 {} 的存储配额为: {} 字节", userId, newQuota);
    }
}
