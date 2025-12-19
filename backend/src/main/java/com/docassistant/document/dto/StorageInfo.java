package com.docassistant.document.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 存储空间信息DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorageInfo {
    
    /**
     * 已使用空间（字节）
     */
    private Long usedSpace;
    
    /**
     * 总配额（字节）
     */
    private Long totalQuota;
    
    /**
     * 剩余空间（字节）
     */
    private Long remainingSpace;
    
    /**
     * 使用百分比
     */
    private Double usagePercentage;
    
    /**
     * 是否接近限制（90%以上）
     */
    private Boolean nearLimit;
}
