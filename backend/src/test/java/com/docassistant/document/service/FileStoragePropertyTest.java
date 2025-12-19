package com.docassistant.document.service;

import com.docassistant.document.service.impl.FileStorageServiceImpl;
import net.jqwik.api.*;
import net.jqwik.api.constraints.LongRange;

import java.util.HashSet;
import java.util.Set;

/**
 * FileStorageService属性测试
 * 使用jqwik进行基于属性的测试
 */
class FileStoragePropertyTest {
    
    /**
     * Feature: document-management, Property 26: 文件名唯一性
     * Validates: Requirements 10.1
     * 
     * 对于任何文件存储操作，生成的文件名应该是唯一的，避免冲突
     */
    @Property(tries = 100)
    void generatedFileNamesShouldBeUnique(
            @ForAll("validFilenames") String originalFilename,
            @ForAll @LongRange(min = 1, max = 10000) Long userId) {
        
        FileStorageService fileStorageService = new FileStorageServiceImpl();
        
        // 生成多个文件名
        Set<String> generatedNames = new HashSet<>();
        int iterations = 100;
        
        for (int i = 0; i < iterations; i++) {
            String uniqueName = fileStorageService.generateUniqueFileName(originalFilename, userId);
            
            // 验证文件名是唯一的（之前没有生成过）
            if (!generatedNames.add(uniqueName)) {
                throw new AssertionError("生成了重复的文件名: " + uniqueName);
            }
        }
        
        // 验证生成了预期数量的唯一文件名
        if (generatedNames.size() != iterations) {
            throw new AssertionError(
                String.format("期望生成 %d 个唯一文件名，实际生成 %d 个", 
                    iterations, generatedNames.size())
            );
        }
    }
    
    /**
     * Feature: document-management, Property 27: 目录结构组织正确性
     * Validates: Requirements 10.2
     * 
     * 对于任何文件存储操作，文件应该按用户ID和日期组织在目录结构中
     */
    @Property(tries = 100)
    void filesShouldBeOrganizedByUserIdAndDate(
            @ForAll("validFilenames") String originalFilename,
            @ForAll @LongRange(min = 1, max = 10000) Long userId) {
        
        FileStorageService fileStorageService = new FileStorageServiceImpl();
        
        // 生成文件名
        String uniqueName = fileStorageService.generateUniqueFileName(originalFilename, userId);
        
        // 验证文件名包含正确的扩展名
        if (originalFilename.contains(".")) {
            String expectedExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            if (!uniqueName.endsWith(expectedExtension)) {
                throw new AssertionError(
                    String.format("文件名应该保留扩展名 %s，实际文件名: %s", 
                        expectedExtension, uniqueName)
                );
            }
        }
        
        // 模拟存储文件并验证路径结构
        // 路径格式应该是: userId/yyyy-MM-dd/uniqueFileName
        org.springframework.mock.web.MockMultipartFile mockFile = 
            new org.springframework.mock.web.MockMultipartFile(
                "file",
                originalFilename,
                "application/octet-stream",
                "test content".getBytes()
            );
        
        // 使用临时目录
        java.nio.file.Path tempDir;
        try {
            tempDir = java.nio.file.Files.createTempDirectory("test-storage");
            org.springframework.test.util.ReflectionTestUtils.setField(
                fileStorageService, "documentStoragePath", tempDir.toString());
            
            String filePath = fileStorageService.storeFile(mockFile, userId);
            
            // 验证路径结构：userId/yyyy-MM-dd/filename
            String[] pathParts = filePath.split("/");
            
            if (pathParts.length != 3) {
                throw new AssertionError(
                    String.format("路径应该有3个部分（userId/date/filename），实际: %d 部分, 路径: %s", 
                        pathParts.length, filePath)
                );
            }
            
            // 验证第一部分是用户ID
            if (!pathParts[0].equals(userId.toString())) {
                throw new AssertionError(
                    String.format("路径第一部分应该是用户ID %s，实际: %s", 
                        userId, pathParts[0])
                );
            }
            
            // 验证第二部分是日期格式 yyyy-MM-dd
            if (!pathParts[1].matches("\\d{4}-\\d{2}-\\d{2}")) {
                throw new AssertionError(
                    String.format("路径第二部分应该是日期格式 yyyy-MM-dd，实际: %s", 
                        pathParts[1])
                );
            }
            
            // 验证第三部分是文件名
            if (pathParts[2].isEmpty()) {
                throw new AssertionError("路径第三部分（文件名）不能为空");
            }
            
            // 清理临时文件
            java.nio.file.Files.walk(tempDir)
                .sorted((a, b) -> b.compareTo(a))
                .forEach(path -> {
                    try {
                        java.nio.file.Files.deleteIfExists(path);
                    } catch (java.io.IOException e) {
                        // 忽略清理错误
                    }
                });
            
        } catch (java.io.IOException e) {
            throw new RuntimeException("测试失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 提供有效的文件名
     */
    @Provide
    Arbitrary<String> validFilenames() {
        return Arbitraries.of(
            "document.pdf",
            "report.docx",
            "notes.txt",
            "file.doc",
            "data.PDF",
            "my-file.pdf",
            "test_document.docx",
            "file123.txt",
            "文档.pdf",
            "测试文件.docx"
        );
    }
}
