package com.docassistant.document.exception;

import com.docassistant.auth.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * 文件上传异常
 */
public class FileUploadException extends BusinessException {
    
    public FileUploadException(String message) {
        super(message, "FILE_UPLOAD_FAILED", HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    public FileUploadException(String message, Throwable cause) {
        super(message, "FILE_UPLOAD_FAILED", HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }
}
