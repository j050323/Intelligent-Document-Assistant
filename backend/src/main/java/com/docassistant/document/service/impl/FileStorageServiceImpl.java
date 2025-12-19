package com.docassistant.document.service.impl;

import com.docassistant.document.service.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 文件存储服务实现类
 * 使用本地文件系统存储文件
 */
@Slf4j
@Service
public class FileStorageServiceImpl implements FileStorageService {
    
    @Value("${app.file-storage.document-path:./uploads/documents}")
    private String documentStoragePath;
    
    @Override
    public String storeFile(MultipartFile file, Long userId) {
        try {
            // 生成唯一文件名
            String uniqueFileName = generateUniqueFileName(file.getOriginalFilename(), userId);
            
            // 构建文件存储路径：basePath/userId/yyyy-MM-dd/uniqueFileName
            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            Path directoryPath = Paths.get(documentStoragePath, userId.toString(), datePath);
            
            // 创建目录（如果不存在）
            Files.createDirectories(directoryPath);
            
            // 完整文件路径
            Path filePath = directoryPath.resolve(uniqueFileName);
            
            // 保存文件
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            
            // 返回相对路径
            String relativePath = userId + "/" + datePath + "/" + uniqueFileName;
            log.info("文件存储成功: {}", relativePath);
            
            return relativePath;
            
        } catch (IOException e) {
            log.error("文件存储失败: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("文件存储失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public byte[] loadFile(String filePath) {
        try {
            Path fullPath = Paths.get(documentStoragePath, filePath);
            
            if (!Files.exists(fullPath)) {
                log.error("文件不存在: {}", filePath);
                throw new RuntimeException("文件不存在: " + filePath);
            }
            
            return Files.readAllBytes(fullPath);
            
        } catch (IOException e) {
            log.error("文件读取失败: {}", filePath, e);
            throw new RuntimeException("文件读取失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void deleteFile(String filePath) {
        try {
            Path fullPath = Paths.get(documentStoragePath, filePath);
            
            if (Files.exists(fullPath)) {
                Files.delete(fullPath);
                log.info("文件删除成功: {}", filePath);
            } else {
                log.warn("文件不存在，无需删除: {}", filePath);
            }
            
        } catch (IOException e) {
            log.error("文件删除失败: {}", filePath, e);
            throw new RuntimeException("文件删除失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public String generateUniqueFileName(String originalFilename, Long userId) {
        // 获取文件扩展名
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        
        // 生成唯一文件名：UUID + 扩展名
        String uniqueFileName = UUID.randomUUID().toString() + extension;
        
        return uniqueFileName;
    }
    
    @Override
    public boolean fileExists(String filePath) {
        Path fullPath = Paths.get(documentStoragePath, filePath);
        return Files.exists(fullPath);
    }
    
    @Override
    public String storeFile(java.io.File file, Long userId, String originalFilename) {
        try {
            // 生成唯一文件名
            String uniqueFileName = generateUniqueFileName(originalFilename, userId);
            
            // 构建文件存储路径：basePath/userId/yyyy-MM-dd/uniqueFileName
            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            Path directoryPath = Paths.get(documentStoragePath, userId.toString(), datePath);
            
            // 创建目录（如果不存在）
            Files.createDirectories(directoryPath);
            
            // 完整文件路径
            Path filePath = directoryPath.resolve(uniqueFileName);
            
            // 复制文件
            Files.copy(file.toPath(), filePath, StandardCopyOption.REPLACE_EXISTING);
            
            // 返回相对路径
            String relativePath = userId + "/" + datePath + "/" + uniqueFileName;
            log.info("文件存储成功: {}", relativePath);
            
            return relativePath;
            
        } catch (IOException e) {
            log.error("文件存储失败: {}", originalFilename, e);
            throw new RuntimeException("文件存储失败: " + e.getMessage(), e);
        }
    }
}
