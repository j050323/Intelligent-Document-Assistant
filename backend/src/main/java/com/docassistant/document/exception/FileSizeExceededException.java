package com.docassistant.document.exception;

import com.docassistant.auth.exception.ValidationException;

/**
 * 文件大小超限异常
 */
public class FileSizeExceededException extends ValidationException {
    
    public FileSizeExceededException(String message) {
        super(message, "FILE_SIZE_EXCEEDED");
    }
}
