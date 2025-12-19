package com.docassistant.document.controller;

import com.docassistant.auth.entity.User;
import com.docassistant.auth.entity.UserRole;
import com.docassistant.auth.repository.UserRepository;
import com.docassistant.auth.service.TokenService;
import com.docassistant.document.entity.Document;
import com.docassistant.document.entity.Folder;
import com.docassistant.document.repository.DocumentRepository;
import com.docassistant.document.repository.FolderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 文件夹管理API集成测试
 * 测试文件夹的创建、查询、更新、删除等操作
 */
@SpringBootTest(classes = com.docassistant.auth.UserAuthApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FolderControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private FolderRepository folderRepository;
    
    @Autowired
    private DocumentRepository documentRepository;
    
    @Autowired
    private TokenService tokenService;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
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
     * 测试创建文件夹
     */
    @Test
    void testCreateFolder() throws Exception {
        String requestBody = objectMapper.writeValueAsString(
                java.util.Map.of("name", "TestFolder")
        );
        
        mockMvc.perform(post("/api/folders")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("TestFolder"))
                .andExpect(jsonPath("$.parentId").doesNotExist());
    }
    
    /**
     * 测试创建子文件夹（层级结构）
     */
    @Test
    void testCreateSubfolder() throws Exception {
        // 先创建父文件夹
        Folder parentFolder = createTestFolder("ParentFolder", testUser.getId(), null);
        
        // 创建子文件夹
        String requestBody = objectMapper.writeValueAsString(
                java.util.Map.of(
                        "name", "SubFolder",
                        "parentId", parentFolder.getId()
                )
        );
        
        mockMvc.perform(post("/api/folders")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("SubFolder"))
                .andExpect(jsonPath("$.parentId").value(parentFolder.getId()));
    }
    
    /**
     * 测试获取文件夹列表
     */
    @Test
    void testGetFolders() throws Exception {
        // 创建根文件夹
        createTestFolder("Folder1", testUser.getId(), null);
        createTestFolder("Folder2", testUser.getId(), null);
        
        mockMvc.perform(get("/api/folders")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }
    
    /**
     * 测试获取子文件夹列表
     */
    @Test
    void testGetSubfolders() throws Exception {
        // 创建父文件夹和子文件夹
        Folder parentFolder = createTestFolder("ParentFolder", testUser.getId(), null);
        createTestFolder("SubFolder1", testUser.getId(), parentFolder.getId());
        createTestFolder("SubFolder2", testUser.getId(), parentFolder.getId());
        
        mockMvc.perform(get("/api/folders")
                        .param("parentId", parentFolder.getId().toString())
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }
    
    /**
     * 测试获取文件夹详情
     */
    @Test
    void testGetFolderById() throws Exception {
        Folder folder = createTestFolder("TestFolder", testUser.getId(), null);
        
        mockMvc.perform(get("/api/folders/" + folder.getId())
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(folder.getId()))
                .andExpect(jsonPath("$.name").value("TestFolder"));
    }
    
    /**
     * 测试更新文件夹（重命名）
     */
    @Test
    void testUpdateFolder() throws Exception {
        Folder folder = createTestFolder("OldName", testUser.getId(), null);
        
        String requestBody = objectMapper.writeValueAsString(
                java.util.Map.of("name", "NewName")
        );
        
        mockMvc.perform(put("/api/folders/" + folder.getId())
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(folder.getId()))
                .andExpect(jsonPath("$.name").value("NewName"));
    }
    
    /**
     * 测试删除空文件夹
     */
    @Test
    void testDeleteEmptyFolder() throws Exception {
        Folder folder = createTestFolder("EmptyFolder", testUser.getId(), null);
        
        mockMvc.perform(delete("/api/folders/" + folder.getId())
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNoContent());
        
        // 验证文件夹已删除
        assertThat(folderRepository.findById(folder.getId())).isEmpty();
    }
    
    /**
     * 测试删除非空文件夹（应该失败）
     */
    @Test
    void testDeleteNonEmptyFolder() throws Exception {
        // 创建文件夹和文档
        Folder folder = createTestFolder("NonEmptyFolder", testUser.getId(), null);
        createTestDocument("doc.pdf", testUser.getId(), folder.getId());
        
        mockMvc.perform(delete("/api/folders/" + folder.getId())
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("FOLDER_NOT_EMPTY"));
    }
    
    /**
     * 测试获取文件夹中的文档
     */
    @Test
    void testGetDocumentsInFolder() throws Exception {
        Folder folder = createTestFolder("TestFolder", testUser.getId(), null);
        createTestDocument("doc1.pdf", testUser.getId(), folder.getId());
        createTestDocument("doc2.txt", testUser.getId(), folder.getId());
        
        mockMvc.perform(get("/api/folders/" + folder.getId() + "/documents")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }
    
    /**
     * 测试权限控制 - 用户不能访问其他用户的文件夹
     */
    @Test
    void testAccessControlForFolders() throws Exception {
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
        
        // 为另一个用户创建文件夹
        Folder otherUserFolder = createTestFolder("OtherFolder", otherUser.getId(), null);
        
        // 尝试访问其他用户的文件夹
        mockMvc.perform(get("/api/folders/" + otherUserFolder.getId())
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isForbidden());
        
        // 尝试删除其他用户的文件夹
        mockMvc.perform(delete("/api/folders/" + otherUserFolder.getId())
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isForbidden());
    }
    
    /**
     * 测试文件夹名称重复
     */
    @Test
    void testDuplicateFolderName() throws Exception {
        // 创建第一个文件夹
        createTestFolder("DuplicateName", testUser.getId(), null);
        
        // 尝试创建同名文件夹
        String requestBody = objectMapper.writeValueAsString(
                java.util.Map.of("name", "DuplicateName")
        );
        
        mockMvc.perform(post("/api/folders")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("DUPLICATE_FOLDER_NAME"));
    }
    
    /**
     * 测试未认证访问
     */
    @Test
    void testUnauthorizedAccess() throws Exception {
        mockMvc.perform(get("/api/folders"))
                .andExpect(status().isUnauthorized());
        
        String requestBody = objectMapper.writeValueAsString(
                java.util.Map.of("name", "TestFolder")
        );
        
        mockMvc.perform(post("/api/folders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());
    }
    
    // Helper methods
    
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
    
    private Document createTestDocument(String filename, Long userId, Long folderId) {
        Document doc = Document.builder()
                .userId(userId)
                .folderId(folderId)
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
