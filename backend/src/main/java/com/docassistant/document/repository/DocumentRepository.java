package com.docassistant.document.repository;

import com.docassistant.document.entity.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 文档数据访问接口
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    
    /**
     * 根据用户ID查询所有文档（分页）
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 文档分页列表
     */
    Page<Document> findByUserId(Long userId, Pageable pageable);
    
    /**
     * 根据用户ID和文件夹ID查询文档（分页）
     * @param userId 用户ID
     * @param folderId 文件夹ID
     * @param pageable 分页参数
     * @return 文档分页列表
     */
    Page<Document> findByUserIdAndFolderId(Long userId, Long folderId, Pageable pageable);
    
    /**
     * 根据用户ID和文件类型查询文档（分页）
     * @param userId 用户ID
     * @param fileType 文件类型
     * @param pageable 分页参数
     * @return 文档分页列表
     */
    Page<Document> findByUserIdAndFileType(Long userId, String fileType, Pageable pageable);
    
    /**
     * 根据用户ID和文件名搜索文档（分页）
     * 使用pg_trgm扩展进行高效的模糊搜索
     * @param userId 用户ID
     * @param keyword 搜索关键词
     * @param pageable 分页参数
     * @return 文档分页列表
     */
    @Query(value = "SELECT * FROM documents d WHERE d.user_id = :userId AND " +
           "d.original_filename ILIKE CONCAT('%', :keyword, '%')", 
           nativeQuery = true,
           countQuery = "SELECT COUNT(*) FROM documents d WHERE d.user_id = :userId AND " +
                       "d.original_filename ILIKE CONCAT('%', :keyword, '%')")
    Page<Document> searchByFilename(@Param("userId") Long userId, 
                                     @Param("keyword") String keyword, 
                                     Pageable pageable);
    
    /**
     * 根据用户ID、文件夹ID和文件类型查询文档（分页）
     * @param userId 用户ID
     * @param folderId 文件夹ID
     * @param fileType 文件类型
     * @param pageable 分页参数
     * @return 文档分页列表
     */
    Page<Document> findByUserIdAndFolderIdAndFileType(Long userId, Long folderId, 
                                                       String fileType, Pageable pageable);
    
    /**
     * 根据用户ID和文档ID查询文档
     * @param userId 用户ID
     * @param id 文档ID
     * @return 文档对象（如果存在）
     */
    Optional<Document> findByIdAndUserId(Long id, Long userId);
    
    /**
     * 根据用户ID查询所有文档
     * @param userId 用户ID
     * @return 文档列表
     */
    List<Document> findByUserId(Long userId);
    
    /**
     * 根据文件夹ID查询文档数量
     * @param folderId 文件夹ID
     * @return 文档数量
     */
    long countByFolderId(Long folderId);
    
    /**
     * 根据用户ID和文件夹ID查询文档数量
     * @param userId 用户ID
     * @param folderId 文件夹ID
     * @return 文档数量
     */
    long countByUserIdAndFolderId(Long userId, Long folderId);
    
    /**
     * 根据用户ID和文件夹ID查询文档列表（不分页）
     * @param userId 用户ID
     * @param folderId 文件夹ID
     * @return 文档列表
     */
    List<Document> findByUserIdAndFolderId(Long userId, Long folderId);
    
    /**
     * 计算用户的总存储使用量
     * @param userId 用户ID
     * @return 总文件大小（字节）
     */
    @Query("SELECT COALESCE(SUM(d.fileSize), 0) FROM Document d WHERE d.userId = :userId")
    Long calculateTotalStorageUsed(@Param("userId") Long userId);
    
    /**
     * 根据用户ID和文档ID列表批量查询
     * @param userId 用户ID
     * @param ids 文档ID列表
     * @return 文档列表
     */
    List<Document> findByUserIdAndIdIn(Long userId, List<Long> ids);
}
