package com.docassistant.document.service.impl;

import com.docassistant.document.entity.Document;
import com.docassistant.document.exception.FileNotFoundException;
import com.docassistant.document.repository.DocumentRepository;
import com.docassistant.document.service.CompressionService;
import com.docassistant.document.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 文件压缩服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompressionServiceImpl implements CompressionService {
    
    private final DocumentRepository documentRepository;
    private final FileStorageService fileStorageService;
    
    @Override
    public void compressDocuments(Long userId, List<Long> documentIds, OutputStream outputStream) {
        log.info("开始压缩文档，用户ID: {}, 文档数量: {}", userId, documentIds.size());
        
        try (ZipOutputStream zipOut = new ZipOutputStream(outputStream)) {
            // 设置压缩级别
            zipOut.setLevel(6); // 0-9，6是默认值，平衡压缩率和速度
            
            for (Long documentId : documentIds) {
                try {
                    // 查询文档并验证权限
                    Document document = documentRepository.findByIdAndUserId(documentId, userId)
                            .orElseThrow(() -> new FileNotFoundException("文档不存在或无权访问: " + documentId));
                    
                    // 读取文件内容
                    byte[] fileContent = fileStorageService.loadFile(document.getFilePath());
                    
                    // 创建ZIP条目，使用原始文件名
                    String entryName = document.getOriginalFilename();
                    
                    // 如果文件名重复，添加文档ID作为前缀
                    ZipEntry zipEntry = new ZipEntry(entryName);
                    zipEntry.setSize(fileContent.length);
                    
                    zipOut.putNextEntry(zipEntry);
                    zipOut.write(fileContent);
                    zipOut.closeEntry();
                    
                    log.debug("文档已添加到压缩包: {}", entryName);
                    
                } catch (Exception e) {
                    log.error("压缩文档失败，文档ID: {}", documentId, e);
                    // 继续处理其他文档
                }
            }
            
            zipOut.finish();
            log.info("文档压缩完成，用户ID: {}, 文档数量: {}", userId, documentIds.size());
            
        } catch (IOException e) {
            log.error("创建压缩文件失败，用户ID: {}", userId, e);
            throw new RuntimeException("创建压缩文件失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void compressFolderDocuments(Long userId, Long folderId, OutputStream outputStream) {
        log.info("开始压缩文件夹文档，用户ID: {}, 文件夹ID: {}", userId, folderId);
        
        // 查询文件夹中的所有文档
        List<Document> documents = documentRepository.findByUserIdAndFolderId(userId, folderId);
        
        if (documents.isEmpty()) {
            log.warn("文件夹为空，无文档可压缩，文件夹ID: {}", folderId);
            throw new RuntimeException("文件夹为空，无文档可压缩");
        }
        
        // 提取文档ID列表
        List<Long> documentIds = documents.stream()
                .map(Document::getId)
                .toList();
        
        // 调用压缩方法
        compressDocuments(userId, documentIds, outputStream);
    }
}
