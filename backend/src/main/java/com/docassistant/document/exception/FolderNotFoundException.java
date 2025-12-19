package com.docassistant.document.exception;

import com.docassistant.auth.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * 文件夹不存在异常
 */
public class FolderNotFoundException extends BusinessException {
    
    public FolderNotFoundException(String message) {
        super(message, "FOLDER_NOT_FOUND", HttpStatus.NOT_FOUND);
    }
}
