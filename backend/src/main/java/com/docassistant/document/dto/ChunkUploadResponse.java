package com.docassistant.document.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分块上传响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkUploadResponse {
    
    /**
     * 文件唯一标识符
     */
    private String fileIdentifier;
    
    /**
     * 当前分块索引
     */
    private Integer chunkIndex;
    
    /**
     * 是否已完成所有分块上传
     */
    private Boolean completed;
    
    /**
     * 已上传的分块索引列表
     */
    private List<Integer> uploadedChunks;
    
    /**
     * 如果完成，返回文档信息
     */
    private DocumentDTO document;
    
    /**
     * 上传进度百分比
     */
    private Double progress;
}
