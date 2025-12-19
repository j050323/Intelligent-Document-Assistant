package com.docassistant.document.repository;

import com.docassistant.auth.UserAuthApplication;
import com.docassistant.document.entity.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DocumentRepository单元测试
 */
@DataJpaTest
@ActiveProfiles("test")
@ContextConfiguration(classes = UserAuthApplication.class)
@org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa
@org.springframework.data.jpa.repository.config.EnableJpaRepositories(basePackages = "com.docassistant.document.repository")
@org.springframework.boot.autoconfigure.domain.EntityScan(basePackages = "com.docassistant.document.entity")
class DocumentRepositoryTest {
    
    @Autowired
    private DocumentRepository documentRepository;
    
    private Document testDocument1;
    private Document testDocument2;
    private Document testDocument3;
    
    @BeforeEach
    void setUp() {
        documentRepository.deleteAll();
        
        testDocument1 = Document.builder()
                .userId(1L)
                .folderId(null)
                .filename("test-file-1.pdf")
                .originalFilename("Test File 1.pdf")
                .filePath("/uploads/user1/test-file-1.pdf")
                .fileType("pdf")
                .fileSize(1024L)
                .mimeType("application/pdf")
                .build();
        
        testDocument2 = Document.builder()
                .userId(1L)
                .folderId(10L)
                .filename("test-file-2.docx")
                .originalFilename("Test File 2.docx")
                .filePath("/uploads/user1/test-file-2.docx")
                .fileType("docx")
                .fileSize(2048L)
                .mimeType("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                .build();
        
        testDocument3 = Document.builder()
                .userId(2L)
                .folderId(null)
                .filename("test-file-3.txt")
                .originalFilename("Test File 3.txt")
                .filePath("/uploads/user2/test-file-3.txt")
                .fileType("txt")
                .fileSize(512L)
                .mimeType("text/plain")
                .build();
    }
    
    @Test
    void shouldSaveDocument() {
        // When
        Document savedDocument = documentRepository.save(testDocument1);
        
        // Then
        assertThat(savedDocument.getId()).isNotNull();
        assertThat(savedDocument.getFilename()).isEqualTo("test-file-1.pdf");
        assertThat(savedDocument.getUserId()).isEqualTo(1L);
        assertThat(savedDocument.getFileSize()).isEqualTo(1024L);
        assertThat(savedDocument.getCreatedAt()).isNotNull();
    }
    
    @Test
    void shouldFindDocumentsByUserId() {
        // Given
        documentRepository.save(testDocument1);
        documentRepository.save(testDocument2);
        documentRepository.save(testDocument3);
        
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        // When
        Page<Document> documents = documentRepository.findByUserId(1L, pageable);
        
        // Then
        assertThat(documents.getContent()).hasSize(2);
        assertThat(documents.getContent()).extracting("userId").containsOnly(1L);
    }
    
    @Test
    void shouldFindDocumentsByUserIdAndFolderId() {
        // Given
        documentRepository.save(testDocument1);
        documentRepository.save(testDocument2);
        
        Pageable pageable = PageRequest.of(0, 10);
        
        // When
        Page<Document> documents = documentRepository.findByUserIdAndFolderId(1L, 10L, pageable);
        
        // Then
        assertThat(documents.getContent()).hasSize(1);
        assertThat(documents.getContent().get(0).getFolderId()).isEqualTo(10L);
    }
    
    @Test
    void shouldFindDocumentsByUserIdAndFileType() {
        // Given
        documentRepository.save(testDocument1);
        documentRepository.save(testDocument2);
        
        Pageable pageable = PageRequest.of(0, 10);
        
        // When
        Page<Document> documents = documentRepository.findByUserIdAndFileType(1L, "pdf", pageable);
        
        // Then
        assertThat(documents.getContent()).hasSize(1);
        assertThat(documents.getContent().get(0).getFileType()).isEqualTo("pdf");
    }
    
    @Test
    void shouldSearchDocumentsByFilename() {
        // Given
        documentRepository.save(testDocument1);
        documentRepository.save(testDocument2);
        
        Pageable pageable = PageRequest.of(0, 10);
        
        // When
        Page<Document> documents = documentRepository.searchByFilename(1L, "file 1", pageable);
        
        // Then
        assertThat(documents.getContent()).hasSize(1);
        assertThat(documents.getContent().get(0).getOriginalFilename()).contains("File 1");
    }
    
    @Test
    void shouldSearchDocumentsCaseInsensitive() {
        // Given
        documentRepository.save(testDocument1);
        
        Pageable pageable = PageRequest.of(0, 10);
        
        // When
        Page<Document> documents = documentRepository.searchByFilename(1L, "TEST", pageable);
        
        // Then
        assertThat(documents.getContent()).hasSize(1);
    }
    
    @Test
    void shouldFindDocumentByIdAndUserId() {
        // Given
        Document saved = documentRepository.save(testDocument1);
        
        // When
        Optional<Document> found = documentRepository.findByIdAndUserId(saved.getId(), 1L);
        
        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getFilename()).isEqualTo("test-file-1.pdf");
    }
    
    @Test
    void shouldNotFindDocumentWithWrongUserId() {
        // Given
        Document saved = documentRepository.save(testDocument1);
        
        // When
        Optional<Document> found = documentRepository.findByIdAndUserId(saved.getId(), 999L);
        
        // Then
        assertThat(found).isEmpty();
    }
    
    @Test
    void shouldCountDocumentsByFolderId() {
        // Given
        documentRepository.save(testDocument1);
        documentRepository.save(testDocument2);
        
        // When
        long count = documentRepository.countByFolderId(10L);
        
        // Then
        assertThat(count).isEqualTo(1);
    }
    
    @Test
    void shouldCalculateTotalStorageUsed() {
        // Given
        documentRepository.save(testDocument1); // 1024 bytes
        documentRepository.save(testDocument2); // 2048 bytes
        
        // When
        Long totalStorage = documentRepository.calculateTotalStorageUsed(1L);
        
        // Then
        assertThat(totalStorage).isEqualTo(3072L);
    }
    
    @Test
    void shouldReturnZeroForUserWithNoDocuments() {
        // When
        Long totalStorage = documentRepository.calculateTotalStorageUsed(999L);
        
        // Then
        assertThat(totalStorage).isEqualTo(0L);
    }
    
    @Test
    void shouldFindDocumentsByUserIdAndIdIn() {
        // Given
        Document saved1 = documentRepository.save(testDocument1);
        Document saved2 = documentRepository.save(testDocument2);
        documentRepository.save(testDocument3);
        
        // When
        List<Document> documents = documentRepository.findByUserIdAndIdIn(
                1L, Arrays.asList(saved1.getId(), saved2.getId()));
        
        // Then
        assertThat(documents).hasSize(2);
        assertThat(documents).extracting("userId").containsOnly(1L);
    }
    
    @Test
    void shouldDeleteDocument() {
        // Given
        Document saved = documentRepository.save(testDocument1);
        
        // When
        documentRepository.deleteById(saved.getId());
        
        // Then
        Optional<Document> found = documentRepository.findById(saved.getId());
        assertThat(found).isEmpty();
    }
    
    @Test
    void shouldSupportPagination() {
        // Given
        for (int i = 0; i < 15; i++) {
            Document doc = Document.builder()
                    .userId(1L)
                    .filename("file-" + i + ".pdf")
                    .originalFilename("File " + i + ".pdf")
                    .filePath("/uploads/user1/file-" + i + ".pdf")
                    .fileType("pdf")
                    .fileSize(1024L)
                    .mimeType("application/pdf")
                    .build();
            documentRepository.save(doc);
        }
        
        // When
        Pageable pageable = PageRequest.of(0, 10);
        Page<Document> page1 = documentRepository.findByUserId(1L, pageable);
        
        pageable = PageRequest.of(1, 10);
        Page<Document> page2 = documentRepository.findByUserId(1L, pageable);
        
        // Then
        assertThat(page1.getContent()).hasSize(10);
        assertThat(page2.getContent()).hasSize(5);
        assertThat(page1.getTotalElements()).isEqualTo(15);
    }
}
