package com.docassistant.document.repository;

import com.docassistant.auth.UserAuthApplication;
import com.docassistant.document.entity.Folder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FolderRepository单元测试
 */
@DataJpaTest
@ActiveProfiles("test")
@ContextConfiguration(classes = UserAuthApplication.class)
@org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa
@org.springframework.data.jpa.repository.config.EnableJpaRepositories(basePackages = "com.docassistant.document.repository")
@org.springframework.boot.autoconfigure.domain.EntityScan(basePackages = "com.docassistant.document.entity")
class FolderRepositoryTest {
    
    @Autowired
    private FolderRepository folderRepository;
    
    private Folder rootFolder;
    private Folder subFolder;
    private Folder anotherUserFolder;
    
    @BeforeEach
    void setUp() {
        folderRepository.deleteAll();
        
        rootFolder = Folder.builder()
                .userId(1L)
                .parentId(null)
                .name("Documents")
                .path("/Documents")
                .build();
        
        subFolder = Folder.builder()
                .userId(1L)
                .parentId(null) // Will be set after saving rootFolder
                .name("Work")
                .path("/Documents/Work")
                .build();
        
        anotherUserFolder = Folder.builder()
                .userId(2L)
                .parentId(null)
                .name("Documents")
                .path("/Documents")
                .build();
    }
    
    @Test
    void shouldSaveFolder() {
        // When
        Folder savedFolder = folderRepository.save(rootFolder);
        
        // Then
        assertThat(savedFolder.getId()).isNotNull();
        assertThat(savedFolder.getName()).isEqualTo("Documents");
        assertThat(savedFolder.getUserId()).isEqualTo(1L);
        assertThat(savedFolder.getParentId()).isNull();
        assertThat(savedFolder.getCreatedAt()).isNotNull();
    }
    
    @Test
    void shouldFindFoldersByUserId() {
        // Given
        folderRepository.save(rootFolder);
        folderRepository.save(anotherUserFolder);
        
        // When
        List<Folder> folders = folderRepository.findByUserId(1L);
        
        // Then
        assertThat(folders).hasSize(1);
        assertThat(folders.get(0).getUserId()).isEqualTo(1L);
    }
    
    @Test
    void shouldFindFoldersByUserIdAndParentId() {
        // Given
        Folder savedRoot = folderRepository.save(rootFolder);
        subFolder.setParentId(savedRoot.getId());
        folderRepository.save(subFolder);
        
        // When
        List<Folder> subFolders = folderRepository.findByUserIdAndParentId(1L, savedRoot.getId());
        
        // Then
        assertThat(subFolders).hasSize(1);
        assertThat(subFolders.get(0).getName()).isEqualTo("Work");
        assertThat(subFolders.get(0).getParentId()).isEqualTo(savedRoot.getId());
    }
    
    @Test
    void shouldFindRootFolders() {
        // Given
        folderRepository.save(rootFolder);
        Folder savedRoot = folderRepository.save(rootFolder);
        subFolder.setParentId(savedRoot.getId());
        folderRepository.save(subFolder);
        
        // When
        List<Folder> rootFolders = folderRepository.findByUserIdAndParentIdIsNull(1L);
        
        // Then
        assertThat(rootFolders).hasSize(1);
        assertThat(rootFolders.get(0).getParentId()).isNull();
    }
    
    @Test
    void shouldFindFolderByIdAndUserId() {
        // Given
        Folder saved = folderRepository.save(rootFolder);
        
        // When
        Optional<Folder> found = folderRepository.findByIdAndUserId(saved.getId(), 1L);
        
        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Documents");
    }
    
    @Test
    void shouldNotFindFolderWithWrongUserId() {
        // Given
        Folder saved = folderRepository.save(rootFolder);
        
        // When
        Optional<Folder> found = folderRepository.findByIdAndUserId(saved.getId(), 999L);
        
        // Then
        assertThat(found).isEmpty();
    }
    
    @Test
    void shouldCheckFolderNameExistsUnderParent() {
        // Given
        Folder savedRoot = folderRepository.save(rootFolder);
        subFolder.setParentId(savedRoot.getId());
        folderRepository.save(subFolder);
        
        // When & Then
        assertThat(folderRepository.existsByUserIdAndParentIdAndName(
                1L, savedRoot.getId(), "Work")).isTrue();
        assertThat(folderRepository.existsByUserIdAndParentIdAndName(
                1L, savedRoot.getId(), "Personal")).isFalse();
    }
    
    @Test
    void shouldCheckFolderNameExistsInRoot() {
        // Given
        folderRepository.save(rootFolder);
        
        // When & Then
        assertThat(folderRepository.existsByUserIdAndNullParentIdAndName(
                1L, "Documents")).isTrue();
        assertThat(folderRepository.existsByUserIdAndNullParentIdAndName(
                1L, "NonExistent")).isFalse();
    }
    
    @Test
    void shouldFindSubFoldersByParentId() {
        // Given
        Folder savedRoot = folderRepository.save(rootFolder);
        subFolder.setParentId(savedRoot.getId());
        folderRepository.save(subFolder);
        
        Folder anotherSubFolder = Folder.builder()
                .userId(1L)
                .parentId(savedRoot.getId())
                .name("Personal")
                .path("/Documents/Personal")
                .build();
        folderRepository.save(anotherSubFolder);
        
        // When
        List<Folder> subFolders = folderRepository.findByParentId(savedRoot.getId());
        
        // Then
        assertThat(subFolders).hasSize(2);
        assertThat(subFolders).extracting("name").containsExactlyInAnyOrder("Work", "Personal");
    }
    
    @Test
    void shouldFindFolderByUserIdAndPath() {
        // Given
        folderRepository.save(rootFolder);
        
        // When
        Optional<Folder> found = folderRepository.findByUserIdAndPath(1L, "/Documents");
        
        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Documents");
    }
    
    @Test
    void shouldDeleteFolder() {
        // Given
        Folder saved = folderRepository.save(rootFolder);
        
        // When
        folderRepository.deleteById(saved.getId());
        
        // Then
        Optional<Folder> found = folderRepository.findById(saved.getId());
        assertThat(found).isEmpty();
    }
    
    @Test
    void shouldEnforceUniqueFolderNamePerParent() {
        // Given
        Folder savedRoot = folderRepository.save(rootFolder);
        subFolder.setParentId(savedRoot.getId());
        folderRepository.save(subFolder);
        
        Folder duplicateFolder = Folder.builder()
                .userId(1L)
                .parentId(savedRoot.getId())
                .name("Work")
                .path("/Documents/Work")
                .build();
        
        // When & Then
        boolean exceptionThrown = false;
        try {
            folderRepository.saveAndFlush(duplicateFolder);
        } catch (Exception e) {
            // Expected - unique constraint violation
            exceptionThrown = true;
            assertThat(e).isNotNull();
        }
        assertThat(exceptionThrown).as("Should have thrown exception for duplicate folder name").isTrue();
    }
    
    @Test
    void shouldAllowSameFolderNameInDifferentParents() {
        // Given
        Folder savedRoot = folderRepository.save(rootFolder);
        
        Folder workFolder1 = Folder.builder()
                .userId(1L)
                .parentId(savedRoot.getId())
                .name("Projects")
                .path("/Documents/Projects")
                .build();
        Folder savedWork1 = folderRepository.save(workFolder1);
        
        Folder workFolder2 = Folder.builder()
                .userId(1L)
                .parentId(null)
                .name("Projects")
                .path("/Projects")
                .build();
        
        // When
        Folder savedWork2 = folderRepository.save(workFolder2);
        
        // Then
        assertThat(savedWork2.getId()).isNotNull();
        assertThat(savedWork2.getName()).isEqualTo("Projects");
    }
    
    @Test
    void shouldSupportHierarchicalStructure() {
        // Given
        Folder level1 = folderRepository.save(rootFolder);
        
        Folder level2 = Folder.builder()
                .userId(1L)
                .parentId(level1.getId())
                .name("Work")
                .path("/Documents/Work")
                .build();
        Folder savedLevel2 = folderRepository.save(level2);
        
        Folder level3 = Folder.builder()
                .userId(1L)
                .parentId(savedLevel2.getId())
                .name("Projects")
                .path("/Documents/Work/Projects")
                .build();
        Folder savedLevel3 = folderRepository.save(level3);
        
        // When
        List<Folder> level2Folders = folderRepository.findByParentId(level1.getId());
        List<Folder> level3Folders = folderRepository.findByParentId(savedLevel2.getId());
        
        // Then
        assertThat(level2Folders).hasSize(1);
        assertThat(level3Folders).hasSize(1);
        assertThat(level3Folders.get(0).getName()).isEqualTo("Projects");
    }
}
