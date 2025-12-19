package com.docassistant.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新邮箱请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEmailRequest {
    
    @NotBlank(message = "新邮箱地址不能为空")
    @Email(message = "邮箱格式不正确")
    private String newEmail;
    
    @NotBlank(message = "验证码不能为空")
    private String verificationCode;
}
