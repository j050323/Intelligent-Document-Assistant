package com.docassistant.auth.repository;

import com.docassistant.auth.entity.SystemLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 系统日志数据访问接口
 */
@Repository
public interface SystemLogRepository extends JpaRepository<SystemLog, Long> {
    
    /**
     * 根据用户ID查询日志
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 日志分页列表
     */
    Page<SystemLog> findByUserId(Long userId, Pageable pageable);
    
    /**
     * 根据用户ID查询日志（不分页）
     * @param userId 用户ID
     * @return 日志列表
     */
    List<SystemLog> findByUserId(Long userId);
    
    /**
     * 根据操作类型查询日志
     * @param operationType 操作类型
     * @param pageable 分页参数
     * @return 日志分页列表
     */
    Page<SystemLog> findByOperationType(String operationType, Pageable pageable);
    
    /**
     * 根据操作类型查询日志（按创建时间倒序）
     * @param operationType 操作类型
     * @param pageable 分页参数
     * @return 日志分页列表
     */
    Page<SystemLog> findByOperationTypeOrderByCreatedAtDesc(String operationType, Pageable pageable);
    
    /**
     * 根据时间范围查询日志
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param pageable 分页参数
     * @return 日志分页列表
     */
    Page<SystemLog> findByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);
    
    /**
     * 根据用户ID和时间范围查询日志
     * @param userId 用户ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param pageable 分页参数
     * @return 日志分页列表
     */
    Page<SystemLog> findByUserIdAndCreatedAtBetween(Long userId, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);
    
    /**
     * 查询所有日志（按创建时间倒序）
     * @param pageable 分页参数
     * @return 日志分页列表
     */
    Page<SystemLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    /**
     * 根据资源类型查询日志（按创建时间倒序）
     * @param resourceType 资源类型
     * @param pageable 分页参数
     * @return 日志分页列表
     */
    Page<SystemLog> findByResourceTypeOrderByCreatedAtDesc(String resourceType, Pageable pageable);
    
    /**
     * 根据用户ID和资源类型查询日志
     * @param userId 用户ID
     * @param resourceType 资源类型
     * @param pageable 分页参数
     * @return 日志分页列表
     */
    Page<SystemLog> findByUserIdAndResourceType(Long userId, String resourceType, Pageable pageable);
    
    /**
     * 根据操作类型和资源类型查询日志
     * @param operationType 操作类型
     * @param resourceType 资源类型
     * @param pageable 分页参数
     * @return 日志分页列表
     */
    Page<SystemLog> findByOperationTypeAndResourceType(String operationType, String resourceType, Pageable pageable);
    
    /**
     * 根据用户ID、操作类型和资源类型查询日志
     * @param userId 用户ID
     * @param operationType 操作类型
     * @param resourceType 资源类型
     * @param pageable 分页参数
     * @return 日志分页列表
     */
    Page<SystemLog> findByUserIdAndOperationTypeAndResourceType(Long userId, String operationType, String resourceType, Pageable pageable);
}
