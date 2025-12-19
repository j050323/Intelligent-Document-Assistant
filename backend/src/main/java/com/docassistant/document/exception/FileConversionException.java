package com.docassistant.document.exception;

import com.docassistant.auth.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * 文件转换异常
 */
public class FileConversionException extends BusinessException {
    
    public FileConversionException(String message) {
        super(message, "FILE_CONVERSION_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    public FileConversionException(String message, Throwable cause) {
        super(message, "FILE_CONVERSION_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }
}
