package com.docassistant.document.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分块上传请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkUploadRequest {
    
    /**
     * 文件唯一标识符（用于标识同一个文件的不同分块）
     */
    @NotBlank(message = "文件标识符不能为空")
    private String fileIdentifier;
    
    /**
     * 当前分块索引（从0开始）
     */
    @NotNull(message = "分块索引不能为空")
    @Min(value = 0, message = "分块索引必须大于等于0")
    private Integer chunkIndex;
    
    /**
     * 总分块数
     */
    @NotNull(message = "总分块数不能为空")
    @Min(value = 1, message = "总分块数必须大于0")
    private Integer totalChunks;
    
    /**
     * 原始文件名
     */
    @NotBlank(message = "文件名不能为空")
    private String filename;
    
    /**
     * 文件总大小（字节）
     */
    @NotNull(message = "文件大小不能为空")
    @Min(value = 1, message = "文件大小必须大于0")
    private Long totalSize;
    
    /**
     * 文件夹ID（可选）
     */
    private Long folderId;
}
