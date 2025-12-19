package com.docassistant.document.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 文件夹数据传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FolderDTO {
    
    private Long id;
    private String name;
    private Long parentId;
    private String path;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
