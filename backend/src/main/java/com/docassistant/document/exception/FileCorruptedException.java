package com.docassistant.document.exception;

import com.docassistant.auth.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * 文件损坏异常
 */
public class FileCorruptedException extends BusinessException {
    
    public FileCorruptedException(String message) {
        super(message, "FILE_CORRUPTED", HttpStatus.UNPROCESSABLE_ENTITY);
    }
    
    public FileCorruptedException(String filePath, Throwable cause) {
        super("文件已损坏或无法读取: " + filePath, "FILE_CORRUPTED", HttpStatus.UNPROCESSABLE_ENTITY, cause);
    }
}
