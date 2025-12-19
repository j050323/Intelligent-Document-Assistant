package com.docassistant.document.controller;

import com.docassistant.auth.entity.User;
import com.docassistant.auth.entity.UserRole;
import com.docassistant.auth.repository.UserRepository;
import com.docassistant.auth.service.TokenService;
import com.docassistant.document.dto.DocumentDTO;
import com.docassistant.document.dto.StorageInfo;
import com.docassistant.document.entity.Document;
import com.docassistant.document.entity.Folder;
import com.docassistant.document.repository.DocumentRepository;
import com.docassistant.document.repository.FolderRepository;
import com.docassistant.document.service.FileStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 文档管理API集成测试
 * 测试完整的文档上传-下载-删除流程、批量操作、权限控制等
 */
@SpringBootTest(classes = com.docassistant.auth.UserAuthApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DocumentControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private DocumentRepository documentRepository;
    
    @Autowired
    private FolderRepository folderRepository;
    
    @Autowired
    private TokenService tokenService;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private FileStorageService fileStorageService;
    
    private User testUser;
    private String authToken;
    
    @BeforeEach
    void setUp() {
        // 创建测试用户
        testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .role(UserRole.REGULAR_USER)
                .isEmailVerified(true)
                .createdAt(LocalDateTime.now())
                .build();
        testUser = userRepository.save(testUser);
        
        // 生成JWT令牌
        authToken = tokenService.generateAccessToken(testUser, false);
    }
    
    @AfterEach
    void tearDown() {
        // 清理测试数据
        documentRepository.deleteAll();
        folderRepository.deleteAll();
        userRepository.deleteAll();
    }
    
    /**
     * 测试完整的文档上传-下载-删除流程
     */
    @Test
    void testCompleteDocumentLifecycle() throws Exception {
        // 1. 上传文档
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-document.pdf",
                "application/pdf",
                "Test PDF content".getBytes()
        );
        
        MvcResult uploadResult = mockMvc.perform(multipart("/api/documents/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.filename").exists())
                .andExpect(jsonPath("$.originalFilename").value("test-document.pdf"))
                .andExpect(jsonPath("$.fileType").value("pdf"))
                .andReturn();
        
        String uploadResponse = uploadResult.getResponse().getContentAsString();
        DocumentDTO uploadedDoc = objectMapper.readValue(uploadResponse, DocumentDTO.class);
        Long documentId = uploadedDoc.getId();
        
        // 2. 获取文档详情
        mockMvc.perform(get("/api/documents/" + documentId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(documentId))
                .andExpect(jsonPath("$.originalFilename").value("test-document.pdf"));
        
        // 3. 下载文档
        MvcResult downloadResult = mockMvc.perform(get("/api/documents/" + documentId + "/download")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Disposition"))
                .andReturn();
        
        byte[] downloadedContent = downloadResult.getResponse().getContentAsByteArray();
        assertThat(downloadedContent).isNotEmpty();
        
        // 4. 删除文档
        mockMvc.perform(delete("/api/documents/" + documentId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNoContent());
        
        // 5. 验证文档已删除
        mockMvc.perform(get("/api/documents/" + documentId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound());
    }
    
    /**
     * 测试批量上传操作
     */
    @Test
    void testBatchUpload() throws Exception {
        MockMultipartFile file1 = new MockMultipartFile(
                "files",
                "doc1.pdf",
                "application/pdf",
                "Content 1".getBytes()
        );
        
        MockMultipartFile file2 = new MockMultipartFile(
                "files",
                "doc2.txt",
                "text/plain",
                "Content 2".getBytes()
        );
        
        mockMvc.perform(multipart("/api/documents/batch-upload")
                        .file(file1)
                        .file(file2)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.successCount").value(2))
                .andExpect(jsonPath("$.failureCount").value(0))
                .andExpect(jsonPath("$.successIds").isArray())
                .andExpect(jsonPath("$.successIds.length()").value(2));
    }
    
    /**
     * 测试批量删除操作
     */
    @Test
    void testBatchDelete() throws Exception {
        // 先创建两个文档
        Document doc1 = createTestDocument("doc1.pdf", testUser.getId());
        Document doc2 = createTestDocument("doc2.txt", testUser.getId());
        
        String requestBody = objectMapper.writeValueAsString(
                java.util.Arrays.asList(doc1.getId(), doc2.getId())
        );
        
        mockMvc.perform(delete("/api/documents/batch")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.successCount").value(2))
                .andExpect(jsonPath("$.failureCount").value(0));
        
        // 验证文档已删除
        assertThat(documentRepository.findById(doc1.getId())).isEmpty();
        assertThat(documentRepository.findById(doc2.getId())).isEmpty();
    }
    
    /**
     * 测试文档列表查询（分页、搜索、筛选）
     */
    @Test
    void testGetDocumentsWithFilters() throws Exception {
        // 创建测试文档
        createTestDocument("report.pdf", testUser.getId());
        createTestDocument("notes.txt", testUser.getId());
        createTestDocument("presentation.pdf", testUser.getId());
        
        // 测试分页
        mockMvc.perform(get("/api/documents")
                        .param("page", "0")
                        .param("size", "2")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(3));
        
        // 测试搜索
        mockMvc.perform(get("/api/documents")
                        .param("keyword", "report")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].filename").value("report.pdf"));
        
        // 测试文件类型筛选
        mockMvc.perform(get("/api/documents")
                        .param("fileType", "pdf")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2));
    }
    
    /**
     * 测试权限控制 - 用户不能访问其他用户的文档
     */
    @Test
    void testAccessControlForDocuments() throws Exception {
        // 创建另一个用户
        User otherUser = User.builder()
                .username("otheruser")
                .email("other@example.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .role(UserRole.REGULAR_USER)
                .isEmailVerified(true)
                .createdAt(LocalDateTime.now())
                .build();
        otherUser = userRepository.save(otherUser);
        
        // 为另一个用户创建文档
        Document otherUserDoc = createTestDocument("other-doc.pdf", otherUser.getId());
        
        // 尝试访问其他用户的文档
        mockMvc.perform(get("/api/documents/" + otherUserDoc.getId())
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isForbidden());
        
        // 尝试删除其他用户的文档
        mockMvc.perform(delete("/api/documents/" + otherUserDoc.getId())
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isForbidden());
    }
    
    /**
     * 测试文档更新（重命名和移动）
     */
    @Test
    void testUpdateDocument() throws Exception {
        // 创建文档和文件夹
        Document doc = createTestDocument("original.pdf", testUser.getId());
        Folder folder = createTestFolder("TestFolder", testUser.getId(), null);
        
        // 测试重命名
        String renameRequest = objectMapper.writeValueAsString(
                java.util.Map.of("filename", "renamed.pdf")
        );
        
        mockMvc.perform(put("/api/documents/" + doc.getId())
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(renameRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filename").value("renamed.pdf"));
        
        // 测试移动到文件夹
        String moveRequest = objectMapper.writeValueAsString(
                java.util.Map.of("folderId", folder.getId())
        );
        
        mockMvc.perform(put("/api/documents/" + doc.getId())
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(moveRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.folderId").value(folder.getId()));
    }
    
    /**
     * 测试存储空间信息查询
     */
    @Test
    void testGetStorageInfo() throws Exception {
        // 创建一些文档
        createTestDocument("doc1.pdf", testUser.getId());
        createTestDocument("doc2.txt", testUser.getId());
        
        mockMvc.perform(get("/api/documents/storage-info")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usedSpace").exists())
                .andExpect(jsonPath("$.totalQuota").exists())
                .andExpect(jsonPath("$.remainingSpace").exists())
                .andExpect(jsonPath("$.usagePercentage").exists())
                .andExpect(jsonPath("$.nearLimit").exists());
    }
    
    /**
     * 测试文档预览
     */
    @Test
    void testDocumentPreview() throws Exception {
        Document doc = createTestDocument("test.txt", testUser.getId());
        
        mockMvc.perform(get("/api/documents/" + doc.getId() + "/preview")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").exists())
                .andExpect(jsonPath("$.content").exists())
                .andExpect(jsonPath("$.filename").value("test.txt"));
    }
    
    /**
     * 测试不支持的文件格式
     */
    @Test
    void testUnsupportedFileFormat() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.exe",
                "application/x-msdownload",
                "Executable content".getBytes()
        );
        
        mockMvc.perform(multipart("/api/documents/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("UNSUPPORTED_FILE_FORMAT"));
    }
    
    /**
     * 测试未认证访问
     */
    @Test
    void testUnauthorizedAccess() throws Exception {
        mockMvc.perform(get("/api/documents"))
                .andExpect(status().isUnauthorized());
        
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                "Content".getBytes()
        );
        
        mockMvc.perform(multipart("/api/documents/upload")
                        .file(file))
                .andExpect(status().isUnauthorized());
    }
    
    // Helper methods
    
    private Document createTestDocument(String filename, Long userId) {
        Document doc = Document.builder()
                .userId(userId)
                .filename(filename)
                .originalFilename(filename)
                .filePath("/test/path/" + filename)
                .fileType(getFileExtension(filename))
                .fileSize(1024L)
                .mimeType(getMimeType(filename))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return documentRepository.save(doc);
    }
    
    private Folder createTestFolder(String name, Long userId, Long parentId) {
        Folder folder = Folder.builder()
                .userId(userId)
                .name(name)
                .parentId(parentId)
                .path(parentId == null ? "/" + name : null)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return folderRepository.save(folder);
    }
    
    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot + 1) : "";
    }
    
    private String getMimeType(String filename) {
        String ext = getFileExtension(filename);
        return switch (ext) {
            case "pdf" -> "application/pdf";
            case "txt" -> "text/plain";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            default -> "application/octet-stream";
        };
    }
}
