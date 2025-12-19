package com.docassistant.document.exception;

import com.docassistant.auth.exception.ValidationException;

/**
 * 不支持的文件格式异常
 */
public class UnsupportedFileFormatException extends ValidationException {
    
    public UnsupportedFileFormatException(String message) {
        super(message, "UNSUPPORTED_FILE_FORMAT");
    }
}
