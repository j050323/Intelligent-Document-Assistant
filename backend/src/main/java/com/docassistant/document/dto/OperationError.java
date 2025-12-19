package com.docassistant.document.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 操作错误信息DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationError {
    
    /**
     * 文档ID（如果有）
     */
    private Long documentId;
    
    /**
     * 文件名
     */
    private String filename;
    
    /**
     * 错误消息
     */
    private String errorMessage;
    
    /**
     * 错误代码
     */
    private String errorCode;
}
