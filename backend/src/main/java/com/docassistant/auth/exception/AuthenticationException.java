package com.docassistant.auth.exception;

import org.springframework.http.HttpStatus;

/**
 * 认证异常类
 */
public class AuthenticationException extends BusinessException {
    
    public AuthenticationException(String message, String errorCode) {
        super(message, errorCode, HttpStatus.UNAUTHORIZED);
    }
    
    public AuthenticationException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, HttpStatus.UNAUTHORIZED, cause);
    }
}
