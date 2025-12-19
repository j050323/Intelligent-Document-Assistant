package com.docassistant.document.service;

import com.docassistant.document.dto.ChunkUploadRequest;
import com.docassistant.document.dto.ChunkUploadResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 分块上传服务接口
 */
public interface ChunkedUploadService {
    
    /**
     * 上传文件分块
     * 
     * @param userId 用户ID
     * @param chunk 分块文件
     * @param request 分块上传请求
     * @return 分块上传响应
     */
    ChunkUploadResponse uploadChunk(Long userId, MultipartFile chunk, ChunkUploadRequest request);
    
    /**
     * 获取已上传的分块列表
     * 
     * @param userId 用户ID
     * @param fileIdentifier 文件标识符
     * @return 已上传的分块索引列表
     */
    List<Integer> getUploadedChunks(Long userId, String fileIdentifier);
    
    /**
     * 取消分块上传，清理临时文件
     * 
     * @param userId 用户ID
     * @param fileIdentifier 文件标识符
     */
    void cancelChunkedUpload(Long userId, String fileIdentifier);
}
