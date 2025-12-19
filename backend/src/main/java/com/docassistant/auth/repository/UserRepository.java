package com.docassistant.auth.repository;

import com.docassistant.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户数据访问接口
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * 根据邮箱查询用户
     * @param email 邮箱地址
     * @return 用户对象（如果存在）
     */
    Optional<User> findByEmail(String email);
    
    /**
     * 根据用户名查询用户
     * @param username 用户名
     * @return 用户对象（如果存在）
     */
    Optional<User> findByUsername(String username);
    
    /**
     * 根据用户名或邮箱查询用户
     * @param username 用户名
     * @param email 邮箱地址
     * @return 用户对象（如果存在）
     */
    Optional<User> findByUsernameOrEmail(String username, String email);
    
    /**
     * 检查邮箱是否已存在
     * @param email 邮箱地址
     * @return 如果存在返回true，否则返回false
     */
    boolean existsByEmail(String email);
    
    /**
     * 检查用户名是否已存在
     * @param username 用户名
     * @return 如果存在返回true，否则返回false
     */
    boolean existsByUsername(String username);
}
