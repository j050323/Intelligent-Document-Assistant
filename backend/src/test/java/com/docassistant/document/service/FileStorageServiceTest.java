package com.docassistant.document.service;

import com.docassistant.document.service.impl.FileStorageServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.*;

/**
 * FileStorageService单元测试
 */
class FileStorageServiceTest {
    
    private FileStorageService fileStorageService;
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() {
        fileStorageService = new FileStorageServiceImpl();
        // 使用临时目录作为存储路径
        ReflectionTestUtils.setField(fileStorageService, "documentStoragePath", tempDir.toString());
    }
    
    @AfterEach
    void tearDown() throws IOException {
        // 清理测试文件
        if (Files.exists(tempDir)) {
            Files.walk(tempDir)
                .sorted((a, b) -> b.compareTo(a))
                .forEach(path -> {
                    try {
                        if (!path.equals(tempDir)) {
                            Files.deleteIfExists(path);
                        }
                    } catch (IOException e) {
                        // 忽略清理错误
                    }
                });
        }
    }
    
    @Test
    void testStoreFile_Success() {
        // 准备测试数据
        String originalFilename = "test-document.pdf";
        byte[] content = "Test PDF content".getBytes();
        MockMultipartFile file = new MockMultipartFile(
            "file",
            originalFilename,
            "application/pdf",
            content
        );
        Long userId = 1L;
        
        // 执行存储
        String filePath = fileStorageService.storeFile(file, userId);
        
        // 验证
        assertThat(filePath).isNotNull();
        assertThat(filePath).contains(userId.toString());
        assertThat(filePath).endsWith(".pdf");
        
        // 验证文件存在
        assertThat(fileStorageService.fileExists(filePath)).isTrue();
    }
    
    @Test
    void testLoadFile_Success() {
        // 先存储文件
        String originalFilename = "test-document.txt";
        byte[] content = "Test content for loading".getBytes();
        MockMultipartFile file = new MockMultipartFile(
            "file",
            originalFilename,
            "text/plain",
            content
        );
        Long userId = 2L;
        
        String filePath = fileStorageService.storeFile(file, userId);
        
        // 读取文件
        byte[] loadedContent = fileStorageService.loadFile(filePath);
        
        // 验证内容一致
        assertThat(loadedContent).isEqualTo(content);
    }
    
    @Test
    void testLoadFile_FileNotExists() {
        // 尝试读取不存在的文件
        String nonExistentPath = "999/2025-01-01/non-existent.pdf";
        
        // 验证抛出异常
        assertThatThrownBy(() -> fileStorageService.loadFile(nonExistentPath))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("文件不存在");
    }
    
    @Test
    void testDeleteFile_Success() {
        // 先存储文件
        String originalFilename = "test-to-delete.docx";
        byte[] content = "Content to be deleted".getBytes();
        MockMultipartFile file = new MockMultipartFile(
            "file",
            originalFilename,
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            content
        );
        Long userId = 3L;
        
        String filePath = fileStorageService.storeFile(file, userId);
        
        // 验证文件存在
        assertThat(fileStorageService.fileExists(filePath)).isTrue();
        
        // 删除文件
        fileStorageService.deleteFile(filePath);
        
        // 验证文件已删除
        assertThat(fileStorageService.fileExists(filePath)).isFalse();
    }
    
    @Test
    void testDeleteFile_FileNotExists() {
        // 删除不存在的文件不应抛出异常
        String nonExistentPath = "999/2025-01-01/non-existent.pdf";
        
        assertThatCode(() -> fileStorageService.deleteFile(nonExistentPath))
            .doesNotThrowAnyException();
    }
    
    @Test
    void testGenerateUniqueFileName_WithExtension() {
        String originalFilename = "my-document.pdf";
        Long userId = 4L;
        
        String uniqueFileName = fileStorageService.generateUniqueFileName(originalFilename, userId);
        
        // 验证文件名格式
        assertThat(uniqueFileName).isNotNull();
        assertThat(uniqueFileName).endsWith(".pdf");
        assertThat(uniqueFileName).hasSize(40); // UUID (36) + ".pdf" (4)
    }
    
    @Test
    void testGenerateUniqueFileName_WithoutExtension() {
        String originalFilename = "document-without-extension";
        Long userId = 5L;
        
        String uniqueFileName = fileStorageService.generateUniqueFileName(originalFilename, userId);
        
        // 验证文件名格式
        assertThat(uniqueFileName).isNotNull();
        assertThat(uniqueFileName).hasSize(36); // UUID only
    }
    
    @Test
    void testGenerateUniqueFileName_Uniqueness() {
        String originalFilename = "test.pdf";
        Long userId = 6L;
        
        // 生成多个文件名
        String fileName1 = fileStorageService.generateUniqueFileName(originalFilename, userId);
        String fileName2 = fileStorageService.generateUniqueFileName(originalFilename, userId);
        String fileName3 = fileStorageService.generateUniqueFileName(originalFilename, userId);
        
        // 验证每个文件名都是唯一的
        assertThat(fileName1).isNotEqualTo(fileName2);
        assertThat(fileName1).isNotEqualTo(fileName3);
        assertThat(fileName2).isNotEqualTo(fileName3);
    }
    
    @Test
    void testFileExists_True() {
        // 先存储文件
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "exists-test.txt",
            "text/plain",
            "Test content".getBytes()
        );
        Long userId = 7L;
        
        String filePath = fileStorageService.storeFile(file, userId);
        
        // 验证文件存在
        assertThat(fileStorageService.fileExists(filePath)).isTrue();
    }
    
    @Test
    void testFileExists_False() {
        String nonExistentPath = "999/2025-01-01/non-existent.pdf";
        
        // 验证文件不存在
        assertThat(fileStorageService.fileExists(nonExistentPath)).isFalse();
    }
    
    @Test
    void testStoreFile_CreatesDirectoryStructure() {
        // 准备测试数据
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "directory-test.pdf",
            "application/pdf",
            "Test content".getBytes()
        );
        Long userId = 8L;
        
        // 执行存储
        String filePath = fileStorageService.storeFile(file, userId);
        
        // 验证目录结构：userId/yyyy-MM-dd/filename
        String[] pathParts = filePath.split("/");
        assertThat(pathParts).hasSize(3);
        assertThat(pathParts[0]).isEqualTo(userId.toString());
        assertThat(pathParts[1]).matches("\\d{4}-\\d{2}-\\d{2}"); // 日期格式
        assertThat(pathParts[2]).endsWith(".pdf");
    }
}
