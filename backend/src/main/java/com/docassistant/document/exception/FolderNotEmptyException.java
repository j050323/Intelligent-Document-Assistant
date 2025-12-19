package com.docassistant.document.exception;

import com.docassistant.auth.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * 文件夹不为空异常
 */
public class FolderNotEmptyException extends BusinessException {
    
    public FolderNotEmptyException(String message) {
        super(message, "FOLDER_NOT_EMPTY", HttpStatus.BAD_REQUEST);
    }
}
