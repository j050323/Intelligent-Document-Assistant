package com.docassistant.document.repository;

import com.docassistant.document.entity.Folder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 文件夹数据访问接口
 */
@Repository
public interface FolderRepository extends JpaRepository<Folder, Long> {
    
    /**
     * 根据用户ID查询所有文件夹
     * @param userId 用户ID
     * @return 文件夹列表
     */
    List<Folder> findByUserId(Long userId);
    
    /**
     * 根据用户ID和父文件夹ID查询子文件夹
     * @param userId 用户ID
     * @param parentId 父文件夹ID
     * @return 文件夹列表
     */
    List<Folder> findByUserIdAndParentId(Long userId, Long parentId);
    
    /**
     * 根据用户ID查询根文件夹（parentId为null）
     * @param userId 用户ID
     * @return 根文件夹列表
     */
    List<Folder> findByUserIdAndParentIdIsNull(Long userId);
    
    /**
     * 根据用户ID和文件夹ID查询文件夹
     * @param id 文件夹ID
     * @param userId 用户ID
     * @return 文件夹对象（如果存在）
     */
    Optional<Folder> findByIdAndUserId(Long id, Long userId);
    
    /**
     * 检查文件夹名称在同一父文件夹下是否已存在
     * @param userId 用户ID
     * @param parentId 父文件夹ID
     * @param name 文件夹名称
     * @return 如果存在返回true，否则返回false
     */
    boolean existsByUserIdAndParentIdAndName(Long userId, Long parentId, String name);
    
    /**
     * 检查文件夹名称在根目录下是否已存在
     * @param userId 用户ID
     * @param name 文件夹名称
     * @return 如果存在返回true，否则返回false
     */
    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM Folder f " +
           "WHERE f.userId = :userId AND f.parentId IS NULL AND f.name = :name")
    boolean existsByUserIdAndNullParentIdAndName(@Param("userId") Long userId, 
                                                  @Param("name") String name);
    
    /**
     * 根据父文件夹ID查询所有子文件夹
     * @param parentId 父文件夹ID
     * @return 子文件夹列表
     */
    List<Folder> findByParentId(Long parentId);
    
    /**
     * 根据用户ID和路径查询文件夹
     * @param userId 用户ID
     * @param path 文件夹路径
     * @return 文件夹对象（如果存在）
     */
    Optional<Folder> findByUserIdAndPath(Long userId, String path);
}
