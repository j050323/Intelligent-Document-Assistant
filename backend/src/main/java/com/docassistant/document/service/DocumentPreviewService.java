package com.docassistant.document.service;

import com.docassistant.document.dto.PreviewResponse;

/**
 * 文档预览服务接口
 */
public interface DocumentPreviewService {
    
    /**
     * 预览PDF文档
     * 
     * @param filePath 文件路径
     * @return 预览响应
     */
    PreviewResponse previewPdf(String filePath);
    
    /**
     * 预览TXT文档
     * 
     * @param filePath 文件路径
     * @return 预览响应
     */
    PreviewResponse previewText(String filePath);
    
    /**
     * 预览Word文档
     * 
     * @param filePath 文件路径
     * @return 预览响应
     */
    PreviewResponse previewWord(String filePath);
}
