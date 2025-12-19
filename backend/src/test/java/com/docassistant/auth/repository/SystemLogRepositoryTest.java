package com.docassistant.auth.repository;

import com.docassistant.auth.entity.SystemLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SystemLogRepository单元测试
 */
@DataJpaTest
@ActiveProfiles("test")
class SystemLogRepositoryTest {
    
    @Autowired
    private SystemLogRepository systemLogRepository;
    
    @BeforeEach
    void setUp() {
        systemLogRepository.deleteAll();
    }
    
    @Test
    void shouldSaveSystemLog() {
        // Given
        SystemLog log = SystemLog.builder()
                .userId(1L)
                .operationType("LOGIN")
                .ipAddress("192.168.1.1")
                .userAgent("Mozilla/5.0")
                .status("SUCCESS")
                .build();
        
        // When
        SystemLog savedLog = systemLogRepository.save(log);
        
        // Then
        assertThat(savedLog.getId()).isNotNull();
        assertThat(savedLog.getUserId()).isEqualTo(1L);
        assertThat(savedLog.getOperationType()).isEqualTo("LOGIN");
        assertThat(savedLog.getCreatedAt()).isNotNull();
    }
    
    @Test
    void shouldFindLogsByUserId() {
        // Given
        createTestLog(1L, "LOGIN", "SUCCESS");
        createTestLog(1L, "LOGOUT", "SUCCESS");
        createTestLog(2L, "LOGIN", "SUCCESS");
        
        // When
        List<SystemLog> logs = systemLogRepository.findByUserId(1L);
        
        // Then
        assertThat(logs).hasSize(2);
        assertThat(logs).allMatch(log -> log.getUserId().equals(1L));
    }
    
    @Test
    void shouldFindLogsByUserIdWithPagination() {
        // Given
        for (int i = 0; i < 5; i++) {
            createTestLog(1L, "LOGIN", "SUCCESS");
        }
        
        // When
        PageRequest pageRequest = PageRequest.of(0, 3);
        Page<SystemLog> page = systemLogRepository.findByUserId(1L, pageRequest);
        
        // Then
        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getContent()).hasSize(3);
    }
    
    @Test
    void shouldFindLogsByOperationType() {
        // Given
        createTestLog(1L, "LOGIN", "SUCCESS");
        createTestLog(2L, "LOGIN", "SUCCESS");
        createTestLog(3L, "LOGOUT", "SUCCESS");
        
        // When
        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<SystemLog> page = systemLogRepository.findByOperationType("LOGIN", pageRequest);
        
        // Then
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).allMatch(log -> log.getOperationType().equals("LOGIN"));
    }
    
    @Test
    void shouldFindLogsByTimeRange() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime yesterday = now.minusDays(1);
        LocalDateTime tomorrow = now.plusDays(1);
        
        SystemLog oldLog = createTestLog(1L, "LOGIN", "SUCCESS");
        oldLog.setCreatedAt(yesterday);
        systemLogRepository.save(oldLog);
        
        SystemLog recentLog = createTestLog(2L, "LOGIN", "SUCCESS");
        recentLog.setCreatedAt(now);
        systemLogRepository.save(recentLog);
        
        // When
        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<SystemLog> page = systemLogRepository.findByCreatedAtBetween(
                yesterday.minusHours(1), 
                now.plusHours(1), 
                pageRequest
        );
        
        // Then
        assertThat(page.getTotalElements()).isEqualTo(2);
    }
    
    @Test
    void shouldFindLogsByUserIdAndTimeRange() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime yesterday = now.minusDays(1);
        
        SystemLog log1 = createTestLog(1L, "LOGIN", "SUCCESS");
        log1.setCreatedAt(yesterday);
        systemLogRepository.save(log1);
        
        SystemLog log2 = createTestLog(1L, "LOGOUT", "SUCCESS");
        log2.setCreatedAt(now);
        systemLogRepository.save(log2);
        
        SystemLog log3 = createTestLog(2L, "LOGIN", "SUCCESS");
        log3.setCreatedAt(now);
        systemLogRepository.save(log3);
        
        // When
        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<SystemLog> page = systemLogRepository.findByUserIdAndCreatedAtBetween(
                1L,
                yesterday.minusHours(1),
                now.plusHours(1),
                pageRequest
        );
        
        // Then
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).allMatch(log -> log.getUserId().equals(1L));
    }
    
    @Test
    void shouldFindAllLogsOrderedByCreatedAtDesc() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        
        SystemLog log1 = createTestLog(1L, "LOGIN", "SUCCESS");
        log1.setCreatedAt(now.minusHours(2));
        systemLogRepository.save(log1);
        
        SystemLog log2 = createTestLog(2L, "LOGIN", "SUCCESS");
        log2.setCreatedAt(now.minusHours(1));
        systemLogRepository.save(log2);
        
        SystemLog log3 = createTestLog(3L, "LOGIN", "SUCCESS");
        log3.setCreatedAt(now);
        systemLogRepository.save(log3);
        
        // When
        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<SystemLog> page = systemLogRepository.findAllByOrderByCreatedAtDesc(pageRequest);
        
        // Then
        assertThat(page.getContent()).hasSize(3);
        List<SystemLog> logs = page.getContent();
        assertThat(logs.get(0).getCreatedAt()).isAfter(logs.get(1).getCreatedAt());
        assertThat(logs.get(1).getCreatedAt()).isAfter(logs.get(2).getCreatedAt());
    }
    
    private SystemLog createTestLog(Long userId, String operationType, String status) {
        SystemLog log = SystemLog.builder()
                .userId(userId)
                .operationType(operationType)
                .ipAddress("192.168.1.1")
                .userAgent("Mozilla/5.0")
                .status(status)
                .build();
        return systemLogRepository.save(log);
    }
}
