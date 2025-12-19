package com.docassistant.document.exception;

import com.docassistant.auth.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * 存储配额超限异常
 */
public class StorageQuotaExceededException extends BusinessException {
    
    public StorageQuotaExceededException(String message) {
        super(message, "STORAGE_QUOTA_EXCEEDED", HttpStatus.INSUFFICIENT_STORAGE);
    }
}
