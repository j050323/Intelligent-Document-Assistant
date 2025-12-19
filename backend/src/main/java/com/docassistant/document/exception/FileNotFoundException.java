package com.docassistant.document.exception;

import com.docassistant.auth.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * 文件不存在异常
 */
public class FileNotFoundException extends BusinessException {
    
    public FileNotFoundException(String filePath) {
        super("文件不存在或已被删除: " + filePath, "DOCUMENT_NOT_FOUND", HttpStatus.NOT_FOUND);
    }
}
