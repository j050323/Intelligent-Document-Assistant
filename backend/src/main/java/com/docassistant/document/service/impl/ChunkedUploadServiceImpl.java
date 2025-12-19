package com.docassistant.document.service.impl;

import com.docassistant.document.dto.ChunkUploadRequest;
import com.docassistant.document.dto.ChunkUploadResponse;
import com.docassistant.document.dto.DocumentDTO;
import com.docassistant.document.entity.Document;
import com.docassistant.document.exception.FileUploadException;
import com.docassistant.document.repository.DocumentRepository;
import com.docassistant.document.service.ChunkedUploadService;
import com.docassistant.document.service.FileStorageService;
import com.docassistant.document.service.StorageQuotaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 分块上传服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChunkedUploadServiceImpl implements ChunkedUploadService {
    
    private final FileStorageService fileStorageService;
    private final StorageQuotaService storageQuotaService;
    private final DocumentRepository documentRepository;
    
    @Value("${app.upload.temp-dir:uploads/temp}")
    private String tempDir;
    
    private static final List<String> SUPPORTED_FORMATS = Arrays.asList("pdf", "doc", "docx", "txt");
    private static final long MAX_FILE_SIZE = 100 * 1024 * 1024; // 100MB
    
    @Override
    @Transactional
    public ChunkUploadResponse uploadChunk(Long userId, MultipartFile chunk, ChunkUploadRequest request) {
        log.info("上传分块，用户ID: {}, 文件标识: {}, 分块: {}/{}", 
                 userId, request.getFileIdentifier(), request.getChunkIndex() + 1, request.getTotalChunks());
        
        // 验证文件格式
        validateFileFormat(request.getFilename());
        
        // 验证文件大小
        if (request.getTotalSize() > MAX_FILE_SIZE) {
            throw new FileUploadException(
                    String.format("文件大小超过限制（最大100MB），当前文件大小: %.2f MB", 
                                  request.getTotalSize() / (1024.0 * 1024.0)));
        }
        
        // 检查存储配额（仅在第一个分块时检查）
        if (request.getChunkIndex() == 0) {
            if (!storageQuotaService.checkQuota(userId, request.getTotalSize())) {
                throw new FileUploadException("存储空间不足，无法上传文件");
            }
        }
        
        try {
            // 创建临时目录
            String userTempDir = tempDir + File.separator + userId + File.separator + request.getFileIdentifier();
            Path tempDirPath = Paths.get(userTempDir);
            Files.createDirectories(tempDirPath);
            
            // 保存分块文件
            String chunkFilename = request.getChunkIndex() + ".chunk";
            Path chunkPath = tempDirPath.resolve(chunkFilename);
            chunk.transferTo(chunkPath.toFile());
            
            log.info("分块保存成功: {}", chunkPath);
            
            // 获取已上传的分块列表
            List<Integer> uploadedChunks = getUploadedChunks(userId, request.getFileIdentifier());
            
            // 计算上传进度
            double progress = (uploadedChunks.size() * 100.0) / request.getTotalChunks();
            
            // 检查是否所有分块都已上传
            boolean completed = uploadedChunks.size() == request.getTotalChunks();
            
            ChunkUploadResponse response = ChunkUploadResponse.builder()
                    .fileIdentifier(request.getFileIdentifier())
                    .chunkIndex(request.getChunkIndex())
                    .completed(completed)
                    .uploadedChunks(uploadedChunks)
                    .progress(progress)
                    .build();
            
            // 如果所有分块都已上传，合并文件
            if (completed) {
                DocumentDTO document = mergeChunksAndCreateDocument(userId, request, userTempDir);
                response.setDocument(document);
                
                // 清理临时文件
                cleanupTempFiles(userTempDir);
            }
            
            return response;
            
        } catch (IOException e) {
            log.error("保存分块失败，用户ID: {}, 文件标识: {}", userId, request.getFileIdentifier(), e);
            throw new FileUploadException("保存分块失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public List<Integer> getUploadedChunks(Long userId, String fileIdentifier) {
        String userTempDir = tempDir + File.separator + userId + File.separator + fileIdentifier;
        Path tempDirPath = Paths.get(userTempDir);
        
        if (!Files.exists(tempDirPath)) {
            return new ArrayList<>();
        }
        
        try {
            return Files.list(tempDirPath)
                    .filter(path -> path.toString().endsWith(".chunk"))
                    .map(path -> {
                        String filename = path.getFileName().toString();
                        return Integer.parseInt(filename.replace(".chunk", ""));
                    })
                    .sorted()
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("获取已上传分块列表失败，用户ID: {}, 文件标识: {}", userId, fileIdentifier, e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public void cancelChunkedUpload(Long userId, String fileIdentifier) {
        log.info("取消分块上传，用户ID: {}, 文件标识: {}", userId, fileIdentifier);
        
        String userTempDir = tempDir + File.separator + userId + File.separator + fileIdentifier;
        cleanupTempFiles(userTempDir);
    }
    
    /**
     * 合并分块并创建文档记录
     */
    private DocumentDTO mergeChunksAndCreateDocument(Long userId, ChunkUploadRequest request, String tempDir) {
        log.info("开始合并分块，用户ID: {}, 文件标识: {}", userId, request.getFileIdentifier());
        
        try {
            // 创建合并后的文件
            String mergedFilename = request.getFileIdentifier() + "_merged";
            Path mergedFilePath = Paths.get(tempDir, mergedFilename);
            
            // 按顺序合并所有分块
            try (FileOutputStream fos = new FileOutputStream(mergedFilePath.toFile())) {
                for (int i = 0; i < request.getTotalChunks(); i++) {
                    Path chunkPath = Paths.get(tempDir, i + ".chunk");
                    if (!Files.exists(chunkPath)) {
                        throw new FileUploadException("分块文件缺失: " + i);
                    }
                    
                    byte[] chunkData = Files.readAllBytes(chunkPath);
                    fos.write(chunkData);
                }
            }
            
            log.info("分块合并完成，开始存储文件");
            
            // 存储合并后的文件
            String storedFilePath = fileStorageService.storeFile(mergedFilePath.toFile(), userId, request.getFilename());
            
            // 获取文件类型
            String fileType = getFileExtension(request.getFilename());
            
            // 创建文档记录
            Document document = Document.builder()
                    .userId(userId)
                    .folderId(request.getFolderId())
                    .filename(fileStorageService.generateUniqueFileName(request.getFilename(), userId))
                    .originalFilename(request.getFilename())
                    .filePath(storedFilePath)
                    .fileType(fileType)
                    .fileSize(request.getTotalSize())
                    .mimeType(getMimeType(fileType))
                    .build();
            
            document = documentRepository.save(document);
            
            // 更新存储使用量
            storageQuotaService.increaseUsage(userId, request.getTotalSize());
            
            log.info("文档创建成功，文档ID: {}", document.getId());
            
            // 转换为DTO
            return DocumentDTO.builder()
                    .id(document.getId())
                    .filename(document.getFilename())
                    .originalFilename(document.getOriginalFilename())
                    .fileType(document.getFileType())
                    .fileSize(document.getFileSize())
                    .mimeType(document.getMimeType())
                    .folderId(document.getFolderId())
                    .createdAt(document.getCreatedAt())
                    .updatedAt(document.getUpdatedAt())
                    .build();
            
        } catch (IOException e) {
            log.error("合并分块失败，用户ID: {}, 文件标识: {}", userId, request.getFileIdentifier(), e);
            throw new FileUploadException("合并分块失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 清理临时文件
     */
    private void cleanupTempFiles(String tempDir) {
        try {
            Path tempDirPath = Paths.get(tempDir);
            if (Files.exists(tempDirPath)) {
                Files.walk(tempDirPath)
                        .sorted((a, b) -> b.compareTo(a)) // 先删除文件，再删除目录
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                log.warn("删除临时文件失败: {}", path, e);
                            }
                        });
                log.info("临时文件清理完成: {}", tempDir);
            }
        } catch (IOException e) {
            log.error("清理临时文件失败: {}", tempDir, e);
        }
    }
    
    /**
     * 验证文件格式
     */
    private void validateFileFormat(String filename) {
        String extension = getFileExtension(filename);
        if (!SUPPORTED_FORMATS.contains(extension)) {
            throw new FileUploadException(
                    String.format("不支持的文件格式: %s，支持的格式: PDF, Word (.doc/.docx), TXT", extension));
        }
    }
    
    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new FileUploadException("无法识别文件格式");
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
    
    /**
     * 获取MIME类型
     */
    private String getMimeType(String fileType) {
        switch (fileType.toLowerCase()) {
            case "pdf":
                return "application/pdf";
            case "doc":
                return "application/msword";
            case "docx":
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "txt":
                return "text/plain";
            default:
                return "application/octet-stream";
        }
    }
}
