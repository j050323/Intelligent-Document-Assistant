package com.docassistant.document.exception;

import com.docassistant.auth.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * 文件夹名称重复异常
 */
public class DuplicateFolderNameException extends BusinessException {
    
    public DuplicateFolderNameException(String message) {
        super(message, "DUPLICATE_FOLDER_NAME", HttpStatus.CONFLICT);
    }
}
