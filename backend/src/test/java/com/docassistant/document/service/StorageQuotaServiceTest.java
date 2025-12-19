package com.docassistant.document.service;

import com.docassistant.document.dto.StorageInfo;
import com.docassistant.document.entity.Document;
import com.docassistant.document.repository.DocumentRepository;
import com.docassistant.document.service.impl.StorageQuotaServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * StorageQuotaService单元测试
 */
@ExtendWith(MockitoExtension.class)
class StorageQuotaServiceTest {
    
    @Mock
    private DocumentRepository documentRepository;
    
    private StorageQuotaService storageQuotaService;
    
    private static final long DEFAULT_QUOTA = 1073741824L; // 1GB
    
    @BeforeEach
    void setUp() {
        storageQuotaService = new StorageQuotaServiceImpl(documentRepository);
        ReflectionTestUtils.setField(storageQuotaService, "defaultQuota", DEFAULT_QUOTA);
    }
    
    @Test
    void testCheckQuota_HasEnoughSpace() {
        // 准备测试数据
        Long userId = 1L;
        long usedSpace = 100 * 1024 * 1024L; // 100MB
        long fileSize = 50 * 1024 * 1024L; // 50MB
        
        when(documentRepository.calculateTotalStorageUsed(userId)).thenReturn(usedSpace);
        
        // 执行检查
        boolean hasQuota = storageQuotaService.checkQuota(userId, fileSize);
        
        // 验证
        assertThat(hasQuota).isTrue();
        verify(documentRepository).calculateTotalStorageUsed(userId);
    }
    
    @Test
    void testCheckQuota_NotEnoughSpace() {
        // 准备测试数据
        Long userId = 2L;
        long usedSpace = 1000 * 1024 * 1024L; // 1000MB
        long fileSize = 100 * 1024 * 1024L; // 100MB (超过剩余空间)
        
        when(documentRepository.calculateTotalStorageUsed(userId)).thenReturn(usedSpace);
        
        // 执行检查
        boolean hasQuota = storageQuotaService.checkQuota(userId, fileSize);
        
        // 验证
        assertThat(hasQuota).isFalse();
        verify(documentRepository).calculateTotalStorageUsed(userId);
    }
    
    @Test
    void testCheckQuota_ExactlyAtLimit() {
        // 准备测试数据
        Long userId = 3L;
        long usedSpace = 1024 * 1024 * 1024L; // 1024MB
        long fileSize = 0L; // 刚好达到限制
        
        when(documentRepository.calculateTotalStorageUsed(userId)).thenReturn(usedSpace);
        
        // 执行检查
        boolean hasQuota = storageQuotaService.checkQuota(userId, fileSize);
        
        // 验证
        assertThat(hasQuota).isTrue();
    }
    
    @Test
    void testGetStorageInfo_EmptyStorage() {
        // 准备测试数据
        Long userId = 4L;
        long usedSpace = 0L;
        
        when(documentRepository.calculateTotalStorageUsed(userId)).thenReturn(usedSpace);
        
        // 获取存储信息
        StorageInfo storageInfo = storageQuotaService.getStorageInfo(userId);
        
        // 验证
        assertThat(storageInfo).isNotNull();
        assertThat(storageInfo.getUsedSpace()).isEqualTo(0L);
        assertThat(storageInfo.getTotalQuota()).isEqualTo(DEFAULT_QUOTA);
        assertThat(storageInfo.getRemainingSpace()).isEqualTo(DEFAULT_QUOTA);
        assertThat(storageInfo.getUsagePercentage()).isEqualTo(0.0);
        assertThat(storageInfo.getNearLimit()).isFalse();
    }
    
    @Test
    void testGetStorageInfo_PartiallyUsed() {
        // 准备测试数据
        Long userId = 5L;
        long usedSpace = 500 * 1024 * 1024L; // 500MB
        
        when(documentRepository.calculateTotalStorageUsed(userId)).thenReturn(usedSpace);
        
        // 获取存储信息
        StorageInfo storageInfo = storageQuotaService.getStorageInfo(userId);
        
        // 验证
        assertThat(storageInfo).isNotNull();
        assertThat(storageInfo.getUsedSpace()).isEqualTo(usedSpace);
        assertThat(storageInfo.getTotalQuota()).isEqualTo(DEFAULT_QUOTA);
        assertThat(storageInfo.getRemainingSpace()).isEqualTo(DEFAULT_QUOTA - usedSpace);
        assertThat(storageInfo.getUsagePercentage()).isBetween(48.0, 49.0); // 约48.8%
        assertThat(storageInfo.getNearLimit()).isFalse();
    }
    
    @Test
    void testGetStorageInfo_NearLimit() {
        // 准备测试数据
        Long userId = 6L;
        long usedSpace = 970 * 1024 * 1024L; // 970MB (>90%)
        
        when(documentRepository.calculateTotalStorageUsed(userId)).thenReturn(usedSpace);
        
        // 获取存储信息
        StorageInfo storageInfo = storageQuotaService.getStorageInfo(userId);
        
        // 验证
        assertThat(storageInfo).isNotNull();
        assertThat(storageInfo.getUsedSpace()).isEqualTo(usedSpace);
        assertThat(storageInfo.getUsagePercentage()).isGreaterThan(90.0);
        assertThat(storageInfo.getNearLimit()).isTrue();
    }
    
    @Test
    void testGetStorageInfo_AtLimit() {
        // 准备测试数据
        Long userId = 7L;
        long usedSpace = DEFAULT_QUOTA; // 完全使用
        
        when(documentRepository.calculateTotalStorageUsed(userId)).thenReturn(usedSpace);
        
        // 获取存储信息
        StorageInfo storageInfo = storageQuotaService.getStorageInfo(userId);
        
        // 验证
        assertThat(storageInfo).isNotNull();
        assertThat(storageInfo.getUsedSpace()).isEqualTo(usedSpace);
        assertThat(storageInfo.getRemainingSpace()).isEqualTo(0L);
        assertThat(storageInfo.getUsagePercentage()).isEqualTo(100.0);
        assertThat(storageInfo.getNearLimit()).isTrue();
    }
    
    @Test
    void testGetStorageInfo_OverLimit() {
        // 准备测试数据（理论上不应该发生，但测试边界情况）
        Long userId = 8L;
        long usedSpace = DEFAULT_QUOTA + 100 * 1024 * 1024L; // 超过配额
        
        when(documentRepository.calculateTotalStorageUsed(userId)).thenReturn(usedSpace);
        
        // 获取存储信息
        StorageInfo storageInfo = storageQuotaService.getStorageInfo(userId);
        
        // 验证
        assertThat(storageInfo).isNotNull();
        assertThat(storageInfo.getUsedSpace()).isEqualTo(usedSpace);
        assertThat(storageInfo.getRemainingSpace()).isEqualTo(0L); // 不应该是负数
        assertThat(storageInfo.getUsagePercentage()).isGreaterThan(100.0);
        assertThat(storageInfo.getNearLimit()).isTrue();
    }
    
    @Test
    void testIncreaseUsage() {
        // 测试增加使用量（目前只是日志记录）
        Long userId = 9L;
        long fileSize = 10 * 1024 * 1024L;
        
        // 不应该抛出异常
        assertThatCode(() -> storageQuotaService.increaseUsage(userId, fileSize))
            .doesNotThrowAnyException();
    }
    
    @Test
    void testDecreaseUsage() {
        // 测试减少使用量（目前只是日志记录）
        Long userId = 10L;
        long fileSize = 10 * 1024 * 1024L;
        
        // 不应该抛出异常
        assertThatCode(() -> storageQuotaService.decreaseUsage(userId, fileSize))
            .doesNotThrowAnyException();
    }
    
    @Test
    void testUpdateQuota() {
        // 测试更新配额（目前只是日志记录）
        Long userId = 11L;
        long newQuota = 2 * 1024 * 1024 * 1024L; // 2GB
        
        // 不应该抛出异常
        assertThatCode(() -> storageQuotaService.updateQuota(userId, newQuota))
            .doesNotThrowAnyException();
    }
}
