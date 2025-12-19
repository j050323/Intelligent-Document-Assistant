package com.docassistant.auth.exception;

import org.springframework.http.HttpStatus;

/**
 * 验证异常类
 */
public class ValidationException extends BusinessException {
    
    public ValidationException(String message, String errorCode) {
        super(message, errorCode, HttpStatus.BAD_REQUEST);
    }
    
    public ValidationException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, HttpStatus.BAD_REQUEST, cause);
    }
}
