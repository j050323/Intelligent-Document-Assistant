package com.docassistant.auth.exception;

import org.springframework.http.HttpStatus;

/**
 * 授权异常类
 */
public class AuthorizationException extends BusinessException {
    
    public AuthorizationException(String message, String errorCode) {
        super(message, errorCode, HttpStatus.FORBIDDEN);
    }
    
    public AuthorizationException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, HttpStatus.FORBIDDEN, cause);
    }
}
