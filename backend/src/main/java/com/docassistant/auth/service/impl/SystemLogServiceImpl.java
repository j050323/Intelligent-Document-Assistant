package com.docassistant.auth.service.impl;

import com.docassistant.auth.entity.SystemLog;
import com.docassistant.auth.repository.SystemLogRepository;
import com.docassistant.auth.service.SystemLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 系统日志服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemLogServiceImpl implements SystemLogService {
    
    private final SystemLogRepository systemLogRepository;
    
    private static final String OPERATION_LOGIN = "LOGIN";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILURE = "FAILURE";
    
    @Override
    @Transactional
    public void logLoginSuccess(Long userId, String ipAddress, String userAgent) {
        log.debug("Logging successful login for user ID: {}", userId);
        
        SystemLog systemLog = SystemLog.builder()
                .userId(userId)
                .operationType(OPERATION_LOGIN)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .status(STATUS_SUCCESS)
                .build();
        
        systemLogRepository.save(systemLog);
    }
    
    @Override
    @Transactional
    public void logLoginFailure(String usernameOrEmail, String ipAddress, String userAgent, String errorMessage) {
        log.debug("Logging failed login attempt for: {}", usernameOrEmail);
        
        SystemLog systemLog = SystemLog.builder()
                .userId(null) // 登录失败时可能没有用户ID
                .operationType(OPERATION_LOGIN)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .status(STATUS_FAILURE)
                .errorMessage(errorMessage)
                .build();
        
        systemLogRepository.save(systemLog);
    }
    
    @Override
    @Transactional
    public void logOperation(Long userId, String operationType, String ipAddress, 
                            String userAgent, String status, String errorMessage) {
        log.debug("Logging operation: {} for user ID: {} with status: {}", operationType, userId, status);
        
        SystemLog systemLog = SystemLog.builder()
                .userId(userId)
                .operationType(operationType)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .status(status)
                .errorMessage(errorMessage)
                .build();
        
        systemLogRepository.save(systemLog);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<SystemLog> findAllLoginLogs(Pageable pageable) {
        log.debug("Querying all login logs with pagination");
        return systemLogRepository.findByOperationTypeOrderByCreatedAtDesc(OPERATION_LOGIN, pageable);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<SystemLog> findLoginLogsByUserId(Long userId, Pageable pageable) {
        log.debug("Querying login logs for user ID: {}", userId);
        return systemLogRepository.findByUserId(userId, pageable);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<SystemLog> findLoginLogsByTimeRange(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable) {
        log.debug("Querying login logs between {} and {}", startTime, endTime);
        return systemLogRepository.findByCreatedAtBetween(startTime, endTime, pageable);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<SystemLog> findLoginLogsByUserIdAndTimeRange(Long userId, LocalDateTime startTime, 
                                                             LocalDateTime endTime, Pageable pageable) {
        log.debug("Querying login logs for user ID: {} between {} and {}", userId, startTime, endTime);
        return systemLogRepository.findByUserIdAndCreatedAtBetween(userId, startTime, endTime, pageable);
    }
    
    @Override
    @Transactional
    public void logDocumentOperation(Long userId, String operationType, Long documentId, 
                                    String operationDetails, String status, String errorMessage) {
        log.debug("Logging document operation: {} for user ID: {} on document ID: {} with status: {}", 
                  operationType, userId, documentId, status);
        
        SystemLog systemLog = SystemLog.builder()
                .userId(userId)
                .operationType(operationType)
                .resourceId(documentId)
                .resourceType("DOCUMENT")
                .operationDetails(operationDetails)
                .status(status)
                .errorMessage(errorMessage)
                .build();
        
        systemLogRepository.save(systemLog);
    }
    
    @Override
    @Transactional
    public void logDocumentDelete(Long userId, Long documentId, String beforeState) {
        log.debug("Logging document delete for user ID: {} on document ID: {}", userId, documentId);
        
        String operationDetails = String.format("删除前状态: %s", beforeState);
        
        SystemLog systemLog = SystemLog.builder()
                .userId(userId)
                .operationType("DOCUMENT_DELETE")
                .resourceId(documentId)
                .resourceType("DOCUMENT")
                .operationDetails(operationDetails)
                .status(STATUS_SUCCESS)
                .build();
        
        systemLogRepository.save(systemLog);
    }
    
    @Override
    @Transactional
    public void logDocumentMove(Long userId, Long documentId, String beforeState, String afterState) {
        log.debug("Logging document move for user ID: {} on document ID: {}", userId, documentId);
        
        String operationDetails = String.format("移动前: %s, 移动后: %s", beforeState, afterState);
        
        SystemLog systemLog = SystemLog.builder()
                .userId(userId)
                .operationType("DOCUMENT_MOVE")
                .resourceId(documentId)
                .resourceType("DOCUMENT")
                .operationDetails(operationDetails)
                .status(STATUS_SUCCESS)
                .build();
        
        systemLogRepository.save(systemLog);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<SystemLog> findDocumentOperationLogs(Pageable pageable) {
        log.debug("Querying all document operation logs with pagination");
        return systemLogRepository.findByResourceTypeOrderByCreatedAtDesc("DOCUMENT", pageable);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<SystemLog> findDocumentOperationLogsByUserId(Long userId, Pageable pageable) {
        log.debug("Querying document operation logs for user ID: {}", userId);
        return systemLogRepository.findByUserIdAndResourceType(userId, "DOCUMENT", pageable);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<SystemLog> findDocumentOperationLogsByType(String operationType, Pageable pageable) {
        log.debug("Querying document operation logs by type: {}", operationType);
        return systemLogRepository.findByOperationTypeAndResourceType(operationType, "DOCUMENT", pageable);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<SystemLog> findDocumentOperationLogsByUserIdAndType(Long userId, String operationType, Pageable pageable) {
        log.debug("Querying document operation logs for user ID: {} and type: {}", userId, operationType);
        return systemLogRepository.findByUserIdAndOperationTypeAndResourceType(userId, operationType, "DOCUMENT", pageable);
    }
}
