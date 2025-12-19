package com.docassistant.document.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新文件夹请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateFolderRequest {
    
    @NotBlank(message = "文件夹名称不能为空")
    @Size(max = 255, message = "文件夹名称长度不能超过255个字符")
    private String name;
}
