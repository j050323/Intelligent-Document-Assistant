package com.docassistant.document.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文档查询请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentQueryRequest {
    
    /**
     * 页码（从0开始）
     */
    @Builder.Default
    private Integer page = 0;
    
    /**
     * 每页数量
     */
    @Builder.Default
    private Integer size = 20;
    
    /**
     * 搜索关键词（文件名）
     */
    private String keyword;
    
    /**
     * 文件类型筛选
     */
    private String fileType;
    
    /**
     * 文件夹ID筛选
     */
    private Long folderId;
    
    /**
     * 排序字段
     */
    @Builder.Default
    private String sortBy = "createdAt";
    
    /**
     * 排序方向（ASC/DESC）
     */
    @Builder.Default
    private String sortDirection = "DESC";
}
