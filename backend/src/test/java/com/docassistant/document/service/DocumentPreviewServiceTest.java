package com.docassistant.document.service;

import com.docassistant.document.dto.PreviewResponse;
import com.docassistant.document.exception.FileConversionException;
import com.docassistant.document.exception.FileCorruptedException;
import com.docassistant.document.exception.FileNotFoundException;
import com.docassistant.document.service.impl.DocumentPreviewServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * DocumentPreviewService单元测试
 * 需求：3.1, 3.2, 3.3
 */
@ExtendWith(MockitoExtension.class)
class DocumentPreviewServiceTest {
    
    @Mock
    private FileStorageService fileStorageService;
    
    private DocumentPreviewService documentPreviewService;
    
    @BeforeEach
    void setUp() {
        documentPreviewService = new DocumentPreviewServiceImpl(fileStorageService);
    }
    
    // ==================== PDF预览测试 ====================
    
    @Test
    void testPreviewPdf_Success() {
        // 准备测试数据
        String filePath = "1/2025-01-01/test.pdf";
        byte[] pdfContent = "%PDF-1.4\nTest PDF content".getBytes();
        
        // Mock文件存储服务
        when(fileStorageService.fileExists(filePath)).thenReturn(true);
        when(fileStorageService.loadFile(filePath)).thenReturn(pdfContent);
        
        // 执行预览
        PreviewResponse response = documentPreviewService.previewPdf(filePath);
        
        // 验证响应
        assertThat(response).isNotNull();
        assertThat(response.getType()).isEqualTo("url");
        assertThat(response.getContent()).isEqualTo("/api/documents/files/" + filePath);
        assertThat(response.getFileType()).isEqualTo("pdf");
        assertThat(response.getMimeType()).isEqualTo("application/pdf");
        
        // 验证调用
        verify(fileStorageService).fileExists(filePath);
        verify(fileStorageService).loadFile(filePath);
    }
    
    @Test
    void testPreviewPdf_FileNotFound() {
        // 准备测试数据
        String filePath = "1/2025-01-01/nonexistent.pdf";
        
        // Mock文件不存在
        when(fileStorageService.fileExists(filePath)).thenReturn(false);
        
        // 验证抛出异常
        assertThatThrownBy(() -> documentPreviewService.previewPdf(filePath))
            .isInstanceOf(FileNotFoundException.class)
            .hasMessageContaining(filePath);
        
        // 验证调用
        verify(fileStorageService).fileExists(filePath);
        verify(fileStorageService, never()).loadFile(anyString());
    }
    
    @Test
    void testPreviewPdf_CorruptedFile() {
        // 准备测试数据 - 无效的PDF内容
        String filePath = "1/2025-01-01/corrupted.pdf";
        byte[] invalidContent = "This is not a PDF file".getBytes();
        
        // Mock文件存储服务
        when(fileStorageService.fileExists(filePath)).thenReturn(true);
        when(fileStorageService.loadFile(filePath)).thenReturn(invalidContent);
        
        // 验证抛出异常
        assertThatThrownBy(() -> documentPreviewService.previewPdf(filePath))
            .isInstanceOf(FileCorruptedException.class)
            .hasMessageContaining("PDF文件已损坏或格式无效");
        
        // 验证调用
        verify(fileStorageService).fileExists(filePath);
        verify(fileStorageService).loadFile(filePath);
    }
    
    @Test
    void testPreviewPdf_EmptyFile() {
        // 准备测试数据 - 空文件
        String filePath = "1/2025-01-01/empty.pdf";
        byte[] emptyContent = new byte[0];
        
        // Mock文件存储服务
        when(fileStorageService.fileExists(filePath)).thenReturn(true);
        when(fileStorageService.loadFile(filePath)).thenReturn(emptyContent);
        
        // 验证抛出异常
        assertThatThrownBy(() -> documentPreviewService.previewPdf(filePath))
            .isInstanceOf(FileCorruptedException.class)
            .hasMessageContaining("PDF文件已损坏或格式无效");
        
        // 验证调用
        verify(fileStorageService).fileExists(filePath);
        verify(fileStorageService).loadFile(filePath);
    }
    
    // ==================== TXT预览测试 ====================
    
    @Test
    void testPreviewText_Success() {
        // 准备测试数据
        String filePath = "1/2025-01-01/test.txt";
        String textContent = "This is a test text file.\nWith multiple lines.";
        byte[] content = textContent.getBytes();
        
        // Mock文件存储服务
        when(fileStorageService.fileExists(filePath)).thenReturn(true);
        when(fileStorageService.loadFile(filePath)).thenReturn(content);
        
        // 执行预览
        PreviewResponse response = documentPreviewService.previewText(filePath);
        
        // 验证响应
        assertThat(response).isNotNull();
        assertThat(response.getType()).isEqualTo("text");
        assertThat(response.getContent()).isEqualTo(textContent);
        assertThat(response.getFileType()).isEqualTo("txt");
        assertThat(response.getMimeType()).isEqualTo("text/plain");
        
        // 验证调用
        verify(fileStorageService).fileExists(filePath);
        verify(fileStorageService).loadFile(filePath);
    }
    
    @Test
    void testPreviewText_EmptyFile() {
        // 准备测试数据 - 空文件
        String filePath = "1/2025-01-01/empty.txt";
        byte[] emptyContent = new byte[0];
        
        // Mock文件存储服务
        when(fileStorageService.fileExists(filePath)).thenReturn(true);
        when(fileStorageService.loadFile(filePath)).thenReturn(emptyContent);
        
        // 执行预览
        PreviewResponse response = documentPreviewService.previewText(filePath);
        
        // 验证响应 - 空文件应该返回空字符串，不抛出异常
        assertThat(response).isNotNull();
        assertThat(response.getType()).isEqualTo("text");
        assertThat(response.getContent()).isEmpty();
        assertThat(response.getFileType()).isEqualTo("txt");
        assertThat(response.getMimeType()).isEqualTo("text/plain");
        
        // 验证调用
        verify(fileStorageService).fileExists(filePath);
        verify(fileStorageService).loadFile(filePath);
    }
    
    @Test
    void testPreviewText_FileNotFound() {
        // 准备测试数据
        String filePath = "1/2025-01-01/nonexistent.txt";
        
        // Mock文件不存在
        when(fileStorageService.fileExists(filePath)).thenReturn(false);
        
        // 验证抛出异常
        assertThatThrownBy(() -> documentPreviewService.previewText(filePath))
            .isInstanceOf(FileNotFoundException.class)
            .hasMessageContaining(filePath);
        
        // 验证调用
        verify(fileStorageService).fileExists(filePath);
        verify(fileStorageService, never()).loadFile(anyString());
    }
    
    @Test
    void testPreviewText_WithUTF8Content() {
        // 准备测试数据 - 包含UTF-8字符
        String filePath = "1/2025-01-01/utf8.txt";
        String textContent = "测试文本文件\n包含中文字符\n和特殊符号：©®™";
        byte[] content = textContent.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        
        // Mock文件存储服务
        when(fileStorageService.fileExists(filePath)).thenReturn(true);
        when(fileStorageService.loadFile(filePath)).thenReturn(content);
        
        // 执行预览
        PreviewResponse response = documentPreviewService.previewText(filePath);
        
        // 验证响应
        assertThat(response).isNotNull();
        assertThat(response.getType()).isEqualTo("text");
        assertThat(response.getContent()).isEqualTo(textContent);
        assertThat(response.getFileType()).isEqualTo("txt");
        assertThat(response.getMimeType()).isEqualTo("text/plain");
        
        // 验证调用
        verify(fileStorageService).fileExists(filePath);
        verify(fileStorageService).loadFile(filePath);
    }
    
    // ==================== Word预览测试 ====================
    
    @Test
    void testPreviewWord_DocxSuccess() {
        // 准备测试数据 - DOCX文件（ZIP格式，以PK开头）
        String filePath = "1/2025-01-01/test.docx";
        byte[] docxContent = new byte[100];
        docxContent[0] = 'P';
        docxContent[1] = 'K';
        // 填充其余内容
        for (int i = 2; i < docxContent.length; i++) {
            docxContent[i] = (byte) i;
        }
        
        // Mock文件存储服务
        when(fileStorageService.fileExists(filePath)).thenReturn(true);
        when(fileStorageService.loadFile(filePath)).thenReturn(docxContent);
        
        // 执行预览
        PreviewResponse response = documentPreviewService.previewWord(filePath);
        
        // 验证响应
        assertThat(response).isNotNull();
        assertThat(response.getType()).isEqualTo("url");
        assertThat(response.getContent()).isEqualTo("/api/documents/files/" + filePath);
        assertThat(response.getFileType()).isEqualTo("docx");
        assertThat(response.getMimeType()).isEqualTo("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        
        // 验证调用
        verify(fileStorageService).fileExists(filePath);
        verify(fileStorageService).loadFile(filePath);
    }
    
    @Test
    void testPreviewWord_DocSuccess() {
        // 准备测试数据 - DOC文件
        String filePath = "1/2025-01-01/test.doc";
        byte[] docContent = new byte[100];
        // DOC文件有特定的文件头
        for (int i = 0; i < docContent.length; i++) {
            docContent[i] = (byte) i;
        }
        
        // Mock文件存储服务
        when(fileStorageService.fileExists(filePath)).thenReturn(true);
        when(fileStorageService.loadFile(filePath)).thenReturn(docContent);
        
        // 执行预览
        PreviewResponse response = documentPreviewService.previewWord(filePath);
        
        // 验证响应
        assertThat(response).isNotNull();
        assertThat(response.getType()).isEqualTo("url");
        assertThat(response.getContent()).isEqualTo("/api/documents/files/" + filePath);
        assertThat(response.getFileType()).isEqualTo("doc");
        assertThat(response.getMimeType()).isEqualTo("application/msword");
        
        // 验证调用
        verify(fileStorageService).fileExists(filePath);
        verify(fileStorageService).loadFile(filePath);
    }
    
    @Test
    void testPreviewWord_FileNotFound() {
        // 准备测试数据
        String filePath = "1/2025-01-01/nonexistent.docx";
        
        // Mock文件不存在
        when(fileStorageService.fileExists(filePath)).thenReturn(false);
        
        // 验证抛出异常
        assertThatThrownBy(() -> documentPreviewService.previewWord(filePath))
            .isInstanceOf(FileNotFoundException.class)
            .hasMessageContaining(filePath);
        
        // 验证调用
        verify(fileStorageService).fileExists(filePath);
        verify(fileStorageService, never()).loadFile(anyString());
    }
    
    @Test
    void testPreviewWord_EmptyFile() {
        // 准备测试数据 - 空文件
        String filePath = "1/2025-01-01/empty.docx";
        byte[] emptyContent = new byte[0];
        
        // Mock文件存储服务
        when(fileStorageService.fileExists(filePath)).thenReturn(true);
        when(fileStorageService.loadFile(filePath)).thenReturn(emptyContent);
        
        // 验证抛出异常
        assertThatThrownBy(() -> documentPreviewService.previewWord(filePath))
            .isInstanceOf(FileCorruptedException.class)
            .hasMessageContaining("Word文件为空或已损坏");
        
        // 验证调用
        verify(fileStorageService).fileExists(filePath);
        verify(fileStorageService).loadFile(filePath);
    }
    
    @Test
    void testPreviewWord_CorruptedDocx() {
        // 准备测试数据 - 损坏的DOCX文件（不以PK开头）
        String filePath = "1/2025-01-01/corrupted.docx";
        byte[] invalidContent = "This is not a valid DOCX file".getBytes();
        
        // Mock文件存储服务
        when(fileStorageService.fileExists(filePath)).thenReturn(true);
        when(fileStorageService.loadFile(filePath)).thenReturn(invalidContent);
        
        // 验证抛出异常
        assertThatThrownBy(() -> documentPreviewService.previewWord(filePath))
            .isInstanceOf(FileCorruptedException.class)
            .hasMessageContaining("DOCX文件已损坏或格式无效");
        
        // 验证调用
        verify(fileStorageService).fileExists(filePath);
        verify(fileStorageService).loadFile(filePath);
    }
    
    @Test
    void testPreviewWord_CorruptedDoc() {
        // 准备测试数据 - 损坏的DOC文件（文件太小）
        String filePath = "1/2025-01-01/corrupted.doc";
        byte[] invalidContent = new byte[5]; // 小于8字节
        
        // Mock文件存储服务
        when(fileStorageService.fileExists(filePath)).thenReturn(true);
        when(fileStorageService.loadFile(filePath)).thenReturn(invalidContent);
        
        // 验证抛出异常
        assertThatThrownBy(() -> documentPreviewService.previewWord(filePath))
            .isInstanceOf(FileCorruptedException.class)
            .hasMessageContaining("DOC文件已损坏或格式无效");
        
        // 验证调用
        verify(fileStorageService).fileExists(filePath);
        verify(fileStorageService).loadFile(filePath);
    }
}
