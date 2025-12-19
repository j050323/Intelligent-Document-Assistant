package com.docassistant.document.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文档预览响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreviewResponse {
    
    /**
     * 预览类型：url, text, html
     */
    private String type;
    
    /**
     * 预览内容或URL
     */
    private String content;
    
    /**
     * 文件名
     */
    private String filename;
    
    /**
     * 文件类型
     */
    private String fileType;
    
    /**
     * MIME类型
     */
    private String mimeType;
}
