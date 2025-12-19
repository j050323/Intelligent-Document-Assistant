package com.docassistant.document.service;

import com.docassistant.document.dto.StorageInfo;
import com.docassistant.document.repository.DocumentRepository;
import com.docassistant.document.service.impl.StorageQuotaServiceImpl;
import net.jqwik.api.*;
import net.jqwik.api.constraints.LongRange;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.*;

/**
 * StorageQuotaService属性测试
 * 使用jqwik进行基于属性的测试
 */
class StorageQuotaPropertyTest {
    
    private static final long DEFAULT_QUOTA = 1073741824L; // 1GB
    
    /**
     * Feature: document-management, Property 3: 配额限制正确执行
     * Validates: Requirements 1.5
     * 
     * 对于任何用户，当存储空间使用量超过配额时，新的文件上传应该被拒绝
     */
    @Property(tries = 100)
    void quotaLimitShouldBeEnforced(
            @ForAll @LongRange(min = 1, max = 10000) Long userId,
            @ForAll @LongRange(min = 0, max = DEFAULT_QUOTA) long usedSpace,
            @ForAll @LongRange(min = 1, max = 100 * 1024 * 1024) long fileSize) {
        
        // 创建mock repository
        DocumentRepository documentRepository = mock(DocumentRepository.class);
        when(documentRepository.calculateTotalStorageUsed(userId)).thenReturn(usedSpace);
        
        // 创建服务
        StorageQuotaService storageQuotaService = new StorageQuotaServiceImpl(documentRepository);
        ReflectionTestUtils.setField(storageQuotaService, "defaultQuota", DEFAULT_QUOTA);
        
        // 检查配额
        boolean hasQuota = storageQuotaService.checkQuota(userId, fileSize);
        
        // 验证：只有当剩余空间足够时才应该允许上传
        long remainingSpace = DEFAULT_QUOTA - usedSpace;
        boolean shouldHaveQuota = remainingSpace >= fileSize;
        
        if (hasQuota != shouldHaveQuota) {
            throw new AssertionError(
                String.format("配额检查不正确。用户ID: %d, 已用: %d, 文件大小: %d, 剩余: %d, 检查结果: %b, 期望: %b",
                    userId, usedSpace, fileSize, remainingSpace, hasQuota, shouldHaveQuota)
            );
        }
    }
    
    /**
     * Feature: document-management, Property 21: 存储空间计算准确性
     * Validates: Requirements 8.1, 8.2
     * 
     * 对于任何用户，存储空间使用量应该等于该用户所有文档大小的总和
     */
    @Property(tries = 100)
    void storageUsageShouldEqualSumOfDocumentSizes(
            @ForAll @LongRange(min = 1, max = 10000) Long userId,
            @ForAll @LongRange(min = 0, max = DEFAULT_QUOTA) long totalDocumentSize) {
        
        // 创建mock repository
        DocumentRepository documentRepository = mock(DocumentRepository.class);
        when(documentRepository.calculateTotalStorageUsed(userId)).thenReturn(totalDocumentSize);
        
        // 创建服务
        StorageQuotaService storageQuotaService = new StorageQuotaServiceImpl(documentRepository);
        ReflectionTestUtils.setField(storageQuotaService, "defaultQuota", DEFAULT_QUOTA);
        
        // 获取存储信息
        StorageInfo storageInfo = storageQuotaService.getStorageInfo(userId);
        
        // 验证：使用量应该等于文档总大小
        if (!storageInfo.getUsedSpace().equals(totalDocumentSize)) {
            throw new AssertionError(
                String.format("存储使用量计算不正确。用户ID: %d, 期望: %d, 实际: %d",
                    userId, totalDocumentSize, storageInfo.getUsedSpace())
            );
        }
    }
    
    /**
     * Feature: document-management, Property 22: 存储空间信息完整性
     * Validates: Requirements 8.3
     * 
     * 对于任何存储空间查询，应该返回已使用空间、总配额和剩余空间
     */
    @Property(tries = 100)
    void storageInfoShouldBeComplete(
            @ForAll @LongRange(min = 1, max = 10000) Long userId,
            @ForAll @LongRange(min = 0, max = DEFAULT_QUOTA) long usedSpace) {
        
        // 创建mock repository
        DocumentRepository documentRepository = mock(DocumentRepository.class);
        when(documentRepository.calculateTotalStorageUsed(userId)).thenReturn(usedSpace);
        
        // 创建服务
        StorageQuotaService storageQuotaService = new StorageQuotaServiceImpl(documentRepository);
        ReflectionTestUtils.setField(storageQuotaService, "defaultQuota", DEFAULT_QUOTA);
        
        // 获取存储信息
        StorageInfo storageInfo = storageQuotaService.getStorageInfo(userId);
        
        // 验证：所有必需字段都应该存在
        if (storageInfo.getUsedSpace() == null) {
            throw new AssertionError("已使用空间不能为null");
        }
        
        if (storageInfo.getTotalQuota() == null) {
            throw new AssertionError("总配额不能为null");
        }
        
        if (storageInfo.getRemainingSpace() == null) {
            throw new AssertionError("剩余空间不能为null");
        }
        
        if (storageInfo.getUsagePercentage() == null) {
            throw new AssertionError("使用百分比不能为null");
        }
        
        if (storageInfo.getNearLimit() == null) {
            throw new AssertionError("接近限制标志不能为null");
        }
        
        // 验证：剩余空间 = 总配额 - 已使用空间（但不能为负）
        long expectedRemaining = Math.max(0, DEFAULT_QUOTA - usedSpace);
        if (!storageInfo.getRemainingSpace().equals(expectedRemaining)) {
            throw new AssertionError(
                String.format("剩余空间计算不正确。期望: %d, 实际: %d",
                    expectedRemaining, storageInfo.getRemainingSpace())
            );
        }
        
        // 验证：使用百分比计算正确
        double expectedPercentage = DEFAULT_QUOTA > 0 
            ? (usedSpace * 100.0 / DEFAULT_QUOTA) 
            : 0.0;
        double actualPercentage = storageInfo.getUsagePercentage();
        
        // 允许小的浮点误差
        if (Math.abs(actualPercentage - expectedPercentage) > 0.01) {
            throw new AssertionError(
                String.format("使用百分比计算不正确。期望: %.2f%%, 实际: %.2f%%",
                    expectedPercentage, actualPercentage)
            );
        }
        
        // 验证：nearLimit标志正确
        boolean expectedNearLimit = expectedPercentage >= 90.0;
        if (storageInfo.getNearLimit() != expectedNearLimit) {
            throw new AssertionError(
                String.format("接近限制标志不正确。使用率: %.2f%%, 期望: %b, 实际: %b",
                    expectedPercentage, expectedNearLimit, storageInfo.getNearLimit())
            );
        }
    }
}
