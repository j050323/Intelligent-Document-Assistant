package com.docassistant.document.service.impl;

import com.docassistant.document.dto.PreviewResponse;
import com.docassistant.document.exception.FileConversionException;
import com.docassistant.document.exception.FileCorruptedException;
import com.docassistant.document.exception.FileNotFoundException;
import com.docassistant.document.service.DocumentPreviewService;
import com.docassistant.document.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * 文档预览服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentPreviewServiceImpl implements DocumentPreviewService {
    
    private final FileStorageService fileStorageService;
    
    @Override
    public PreviewResponse previewPdf(String filePath) {
        log.info("预览PDF文档，文件路径: {}", filePath);
        
        // 验证文件存在
        if (!fileStorageService.fileExists(filePath)) {
            log.error("PDF文件不存在: {}", filePath);
            throw new FileNotFoundException(filePath);
        }
        
        try {
            // 验证文件可读性和完整性
            byte[] fileContent = fileStorageService.loadFile(filePath);
            
            // 基本的PDF文件头验证（PDF文件应该以 %PDF- 开头）
            if (fileContent.length < 5 || 
                fileContent[0] != '%' || fileContent[1] != 'P' || 
                fileContent[2] != 'D' || fileContent[3] != 'F' || fileContent[4] != '-') {
                log.error("PDF文件已损坏或格式无效: {}", filePath);
                throw new FileCorruptedException("PDF文件已损坏或格式无效");
            }
            
            // 对于PDF，返回文件URL供浏览器直接渲染
            String fileUrl = "/api/documents/files/" + filePath;
            
            return PreviewResponse.builder()
                    .type("url")
                    .content(fileUrl)
                    .fileType("pdf")
                    .mimeType("application/pdf")
                    .build();
                    
        } catch (FileNotFoundException | FileCorruptedException e) {
            // 重新抛出已知的业务异常
            throw e;
        } catch (RuntimeException e) {
            // 处理文件读取时的运行时异常（如文件损坏、IO错误等）
            log.error("读取PDF文件失败，文件路径: {}, 错误: {}", filePath, e.getMessage(), e);
            throw new FileCorruptedException(filePath, e);
        } catch (Exception e) {
            log.error("预览PDF文档时发生未知错误，文件路径: {}, 错误: {}", filePath, e.getMessage(), e);
            throw new FileConversionException("预览PDF文档失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public PreviewResponse previewText(String filePath) {
        log.info("预览TXT文档，文件路径: {}", filePath);
        
        // 验证文件存在
        if (!fileStorageService.fileExists(filePath)) {
            log.error("TXT文件不存在: {}", filePath);
            throw new FileNotFoundException(filePath);
        }
        
        try {
            // 读取文本文件内容
            byte[] fileContent = fileStorageService.loadFile(filePath);
            
            // 验证文件不为空
            if (fileContent == null || fileContent.length == 0) {
                log.warn("TXT文件为空: {}", filePath);
                return PreviewResponse.builder()
                        .type("text")
                        .content("")
                        .fileType("txt")
                        .mimeType("text/plain")
                        .build();
            }
            
            // 尝试使用UTF-8解码
            String textContent;
            try {
                textContent = new String(fileContent, StandardCharsets.UTF_8);
                
                // 检查是否包含无效的UTF-8字符（可能表示文件损坏或编码错误）
                if (textContent.contains("\uFFFD")) {
                    log.warn("TXT文件可能包含无效字符或编码不正确: {}", filePath);
                }
            } catch (Exception e) {
                log.error("TXT文件解码失败，可能已损坏: {}", filePath, e);
                throw new FileCorruptedException("文本文件解码失败，文件可能已损坏或编码不正确", e);
            }
            
            return PreviewResponse.builder()
                    .type("text")
                    .content(textContent)
                    .fileType("txt")
                    .mimeType("text/plain")
                    .build();
                    
        } catch (FileNotFoundException | FileCorruptedException e) {
            // 重新抛出已知的业务异常
            throw e;
        } catch (RuntimeException e) {
            // 处理文件读取时的运行时异常（如文件损坏、IO错误等）
            log.error("读取TXT文件失败，文件路径: {}, 错误: {}", filePath, e.getMessage(), e);
            throw new FileCorruptedException(filePath, e);
        } catch (Exception e) {
            log.error("预览TXT文档时发生未知错误，文件路径: {}, 错误: {}", filePath, e.getMessage(), e);
            throw new FileConversionException("预览TXT文档失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public PreviewResponse previewWord(String filePath) {
        log.info("预览Word文档，文件路径: {}", filePath);
        
        // 验证文件存在
        if (!fileStorageService.fileExists(filePath)) {
            log.error("Word文件不存在: {}", filePath);
            throw new FileNotFoundException(filePath);
        }
        
        try {
            // 验证文件可读性
            byte[] fileContent = fileStorageService.loadFile(filePath);
            
            // 验证文件不为空
            if (fileContent == null || fileContent.length == 0) {
                log.error("Word文件为空或已损坏: {}", filePath);
                throw new FileCorruptedException("Word文件为空或已损坏");
            }
            
            // 基本的Word文件格式验证
            String extension = getFileExtension(filePath);
            if ("docx".equals(extension)) {
                // DOCX文件是ZIP格式，应该以PK开头（ZIP文件头）
                if (fileContent.length < 2 || fileContent[0] != 'P' || fileContent[1] != 'K') {
                    log.error("DOCX文件已损坏或格式无效: {}", filePath);
                    throw new FileCorruptedException("DOCX文件已损坏或格式无效");
                }
            } else if ("doc".equals(extension)) {
                // DOC文件应该以特定的字节序列开头
                if (fileContent.length < 8) {
                    log.error("DOC文件已损坏或格式无效: {}", filePath);
                    throw new FileCorruptedException("DOC文件已损坏或格式无效");
                }
            }
            
            // Word文档预览比较复杂，需要转换
            // 这里简化处理，返回下载链接
            // 实际项目中可以使用 Apache POI 或其他库转换为HTML
            try {
                String downloadUrl = "/api/documents/files/" + filePath;
                
                return PreviewResponse.builder()
                        .type("url")
                        .content(downloadUrl)
                        .fileType(extension)
                        .mimeType(getMimeType(filePath))
                        .build();
            } catch (Exception e) {
                log.error("Word文档转换失败，文件路径: {}, 错误: {}", filePath, e.getMessage(), e);
                throw new FileConversionException("Word文档转换失败: " + e.getMessage(), e);
            }
                    
        } catch (FileNotFoundException | FileCorruptedException | FileConversionException e) {
            // 重新抛出已知的业务异常
            throw e;
        } catch (RuntimeException e) {
            // 处理文件读取时的运行时异常（如文件损坏、IO错误等）
            log.error("读取Word文件失败，文件路径: {}, 错误: {}", filePath, e.getMessage(), e);
            throw new FileCorruptedException(filePath, e);
        } catch (Exception e) {
            log.error("预览Word文档时发生未知错误，文件路径: {}, 错误: {}", filePath, e.getMessage(), e);
            throw new FileConversionException("预览Word文档失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filePath) {
        if (filePath == null || !filePath.contains(".")) {
            return "";
        }
        return filePath.substring(filePath.lastIndexOf(".") + 1).toLowerCase();
    }
    
    /**
     * 获取MIME类型
     */
    private String getMimeType(String filePath) {
        String extension = getFileExtension(filePath);
        switch (extension) {
            case "doc":
                return "application/msword";
            case "docx":
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            default:
                return "application/octet-stream";
        }
    }
}
