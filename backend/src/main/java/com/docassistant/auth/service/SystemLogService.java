package com.docassistant.auth.service;

import com.docassistant.auth.entity.SystemLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

/**
 * 系统日志服务接口
 */
public interface SystemLogService {
    
    /**
     * 记录登录成功日志
     * 
     * @param userId 用户ID
     * @param ipAddress IP地址
     * @param userAgent 用户代理
     */
    void logLoginSuccess(Long userId, String ipAddress, String userAgent);
    
    /**
     * 记录登录失败日志
     * 
     * @param usernameOrEmail 用户名或邮箱
     * @param ipAddress IP地址
     * @param userAgent 用户代理
     * @param errorMessage 错误信息
     */
    void logLoginFailure(String usernameOrEmail, String ipAddress, String userAgent, String errorMessage);
    
    /**
     * 记录操作日志
     * 
     * @param userId 用户ID
     * @param operationType 操作类型
     * @param ipAddress IP地址
     * @param userAgent 用户代理
     * @param status 状态
     * @param errorMessage 错误信息（可选）
     */
    void logOperation(Long userId, String operationType, String ipAddress, 
                     String userAgent, String status, String errorMessage);
    
    /**
     * 查询所有登录日志（按时间倒序）
     * 
     * @param pageable 分页参数
     * @return 日志分页列表
     */
    Page<SystemLog> findAllLoginLogs(Pageable pageable);
    
    /**
     * 根据用户ID查询登录日志
     * 
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 日志分页列表
     */
    Page<SystemLog> findLoginLogsByUserId(Long userId, Pageable pageable);
    
    /**
     * 根据时间范围查询登录日志
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param pageable 分页参数
     * @return 日志分页列表
     */
    Page<SystemLog> findLoginLogsByTimeRange(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);
    
    /**
     * 根据用户ID和时间范围查询登录日志
     * 
     * @param userId 用户ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param pageable 分页参数
     * @return 日志分页列表
     */
    Page<SystemLog> findLoginLogsByUserIdAndTimeRange(Long userId, LocalDateTime startTime, 
                                                      LocalDateTime endTime, Pageable pageable);
    
    /**
     * 记录文档操作日志
     * 
     * @param userId 用户ID
     * @param operationType 操作类型
     * @param documentId 文档ID
     * @param operationDetails 操作详情（包含操作前后状态）
     * @param status 状态
     * @param errorMessage 错误信息（可选）
     */
    void logDocumentOperation(Long userId, String operationType, Long documentId, 
                             String operationDetails, String status, String errorMessage);
    
    /**
     * 记录文档删除操作（包含操作前状态）
     * 
     * @param userId 用户ID
     * @param documentId 文档ID
     * @param beforeState 删除前的文档状态
     */
    void logDocumentDelete(Long userId, Long documentId, String beforeState);
    
    /**
     * 记录文档移动操作（包含操作前后状态）
     * 
     * @param userId 用户ID
     * @param documentId 文档ID
     * @param beforeState 移动前的状态
     * @param afterState 移动后的状态
     */
    void logDocumentMove(Long userId, Long documentId, String beforeState, String afterState);
    
    /**
     * 查询文档操作日志
     * 
     * @param pageable 分页参数
     * @return 日志分页列表
     */
    Page<SystemLog> findDocumentOperationLogs(Pageable pageable);
    
    /**
     * 根据用户ID查询文档操作日志
     * 
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 日志分页列表
     */
    Page<SystemLog> findDocumentOperationLogsByUserId(Long userId, Pageable pageable);
    
    /**
     * 根据操作类型查询文档操作日志
     * 
     * @param operationType 操作类型
     * @param pageable 分页参数
     * @return 日志分页列表
     */
    Page<SystemLog> findDocumentOperationLogsByType(String operationType, Pageable pageable);
    
    /**
     * 根据用户ID和操作类型查询文档操作日志
     * 
     * @param userId 用户ID
     * @param operationType 操作类型
     * @param pageable 分页参数
     * @return 日志分页列表
     */
    Page<SystemLog> findDocumentOperationLogsByUserIdAndType(Long userId, String operationType, Pageable pageable);
}
