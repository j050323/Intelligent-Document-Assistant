package com.docassistant.document.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 批量操作结果DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchOperationResult {
    
    /**
     * 成功数量
     */
    private Integer successCount;
    
    /**
     * 失败数量
     */
    private Integer failureCount;
    
    /**
     * 成功的文档ID列表
     */
    private List<Long> successIds;
    
    /**
     * 成功的文档DTO列表
     */
    private List<DocumentDTO> successDocuments;
    
    /**
     * 错误列表
     */
    private List<OperationError> errors;
}
