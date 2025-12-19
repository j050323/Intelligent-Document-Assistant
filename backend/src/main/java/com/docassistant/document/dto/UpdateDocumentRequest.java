package com.docassistant.document.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新文档请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDocumentRequest {
    
    /**
     * 新文件名（用于重命名）
     */
    @Size(max = 255, message = "文件名长度不能超过255个字符")
    private String filename;
    
    /**
     * 新文件夹ID（用于移动）
     */
    private Long folderId;
}
