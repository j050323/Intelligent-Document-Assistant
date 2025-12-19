package com.docassistant.document.service;

import com.docassistant.document.dto.CreateFolderRequest;
import com.docassistant.document.dto.FolderDTO;
import com.docassistant.document.dto.UpdateFolderRequest;
import com.docassistant.document.entity.Document;
import com.docassistant.document.entity.Folder;
import com.docassistant.document.exception.DuplicateFolderNameException;
import com.docassistant.document.exception.FolderNotEmptyException;
import com.docassistant.document.exception.FolderNotFoundException;
import com.docassistant.document.repository.DocumentRepository;
import com.docassistant.document.repository.FolderRepository;
import com.docassistant.document.service.impl.FolderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * FolderService Unit Tests
 */
@ExtendWith(MockitoExtension.class)
class FolderServiceTest {
    
    @Mock
    private FolderRepository folderRepository;
    
    @Mock
    private DocumentRepository documentRepository;
    
    @InjectMocks
    private FolderServiceImpl folderService;
    
    private Long userId;
    private Folder testFolder;
    
    @BeforeEach
    void setUp() {
        userId = 1L;
        testFolder = Folder.builder()
                .id(1L)
                .userId(userId)
                .parentId(null)
                .name("Test Folder")
                .path("/Test Folder")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
    
    @Test
    void createFolder_Success() {
        // Arrange
        CreateFolderRequest request = CreateFolderRequest.builder()
                .name("New Folder")
                .parentId(null)
                .build();
        
        when(folderRepository.existsByUserIdAndNullParentIdAndName(userId, "New Folder"))
                .thenReturn(false);
        when(folderRepository.save(any(Folder.class))).thenAnswer(invocation -> {
            Folder folder = invocation.getArgument(0);
            folder.setId(2L);
            folder.setCreatedAt(LocalDateTime.now());
            folder.setUpdatedAt(LocalDateTime.now());
            return folder;
        });
        
        // Act
        FolderDTO result = folderService.createFolder(userId, request);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("New Folder");
        assertThat(result.getPath()).isEqualTo("/New Folder");
        assertThat(result.getParentId()).isNull();
        
        verify(folderRepository, times(1)).save(any(Folder.class));
    }
    
    @Test
    void createFolder_WithParent_Success() {
        // Arrange
        Folder parentFolder = Folder.builder()
                .id(1L)
                .userId(userId)
                .parentId(null)
                .name("Parent")
                .path("/Parent")
                .build();
        
        CreateFolderRequest request = CreateFolderRequest.builder()
                .name("Child")
                .parentId(1L)
                .build();
        
        when(folderRepository.findByIdAndUserId(1L, userId))
                .thenReturn(Optional.of(parentFolder));
        when(folderRepository.existsByUserIdAndParentIdAndName(userId, 1L, "Child"))
                .thenReturn(false);
        when(folderRepository.save(any(Folder.class))).thenAnswer(invocation -> {
            Folder folder = invocation.getArgument(0);
            folder.setId(2L);
            folder.setCreatedAt(LocalDateTime.now());
            folder.setUpdatedAt(LocalDateTime.now());
            return folder;
        });
        
        // Act
        FolderDTO result = folderService.createFolder(userId, request);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Child");
        assertThat(result.getPath()).isEqualTo("/Parent/Child");
        assertThat(result.getParentId()).isEqualTo(1L);
    }
    
    @Test
    void createFolder_DuplicateName_ThrowsException() {
        // Arrange
        CreateFolderRequest request = CreateFolderRequest.builder()
                .name("Duplicate")
                .parentId(null)
                .build();
        
        when(folderRepository.existsByUserIdAndNullParentIdAndName(userId, "Duplicate"))
                .thenReturn(true);
        
        // Act & Assert
        assertThatThrownBy(() -> folderService.createFolder(userId, request))
                .isInstanceOf(DuplicateFolderNameException.class)
                .hasMessageContaining("Duplicate");
        
        verify(folderRepository, never()).save(any(Folder.class));
    }
    
    @Test
    void getFolders_RootLevel_Success() {
        // Arrange
        List<Folder> folders = Arrays.asList(
                Folder.builder().id(1L).userId(userId).name("Folder1").path("/Folder1").build(),
                Folder.builder().id(2L).userId(userId).name("Folder2").path("/Folder2").build()
        );
        
        when(folderRepository.findByUserIdAndParentIdIsNull(userId))
                .thenReturn(folders);
        
        // Act
        List<FolderDTO> result = folderService.getFolders(userId, null);
        
        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Folder1");
        assertThat(result.get(1).getName()).isEqualTo("Folder2");
    }
    
    @Test
    void getFolders_WithParent_Success() {
        // Arrange
        Long parentId = 1L;
        List<Folder> folders = Arrays.asList(
                Folder.builder().id(2L).userId(userId).parentId(parentId).name("Child1").build(),
                Folder.builder().id(3L).userId(userId).parentId(parentId).name("Child2").build()
        );
        
        when(folderRepository.findByUserIdAndParentId(userId, parentId))
                .thenReturn(folders);
        
        // Act
        List<FolderDTO> result = folderService.getFolders(userId, parentId);
        
        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Child1");
        assertThat(result.get(1).getName()).isEqualTo("Child2");
    }
    
    @Test
    void getFolderById_Success() {
        // Arrange
        when(folderRepository.findByIdAndUserId(1L, userId))
                .thenReturn(Optional.of(testFolder));
        
        // Act
        FolderDTO result = folderService.getFolderById(userId, 1L);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Test Folder");
    }
    
    @Test
    void getFolderById_NotFound_ThrowsException() {
        // Arrange
        when(folderRepository.findByIdAndUserId(1L, userId))
                .thenReturn(Optional.empty());
        
        // Act & Assert
        assertThatThrownBy(() -> folderService.getFolderById(userId, 1L))
                .isInstanceOf(FolderNotFoundException.class);
    }
    
    @Test
    void updateFolder_Success() {
        // Arrange
        UpdateFolderRequest request = UpdateFolderRequest.builder()
                .name("Updated Folder")
                .build();
        
        when(folderRepository.findByIdAndUserId(1L, userId))
                .thenReturn(Optional.of(testFolder));
        when(folderRepository.existsByUserIdAndNullParentIdAndName(userId, "Updated Folder"))
                .thenReturn(false);
        when(folderRepository.findByParentId(1L))
                .thenReturn(new ArrayList<>());
        when(folderRepository.save(any(Folder.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        FolderDTO result = folderService.updateFolder(userId, 1L, request);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Updated Folder");
        assertThat(result.getPath()).isEqualTo("/Updated Folder");
    }
    
    @Test
    void updateFolder_SameName_NoUpdate() {
        // Arrange
        UpdateFolderRequest request = UpdateFolderRequest.builder()
                .name("Test Folder")
                .build();
        
        when(folderRepository.findByIdAndUserId(1L, userId))
                .thenReturn(Optional.of(testFolder));
        
        // Act
        FolderDTO result = folderService.updateFolder(userId, 1L, request);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Test Folder");
        
        verify(folderRepository, never()).save(any(Folder.class));
    }
    
    @Test
    void deleteFolder_Empty_Success() {
        // Arrange
        when(folderRepository.findByIdAndUserId(1L, userId))
                .thenReturn(Optional.of(testFolder));
        when(documentRepository.countByUserIdAndFolderId(userId, 1L))
                .thenReturn(0L);
        when(folderRepository.findByParentId(1L))
                .thenReturn(new ArrayList<>());
        
        // Act
        folderService.deleteFolder(userId, 1L);
        
        // Assert
        verify(folderRepository, times(1)).delete(testFolder);
    }
    
    @Test
    void deleteFolder_WithDocuments_ThrowsException() {
        // Arrange
        when(folderRepository.findByIdAndUserId(1L, userId))
                .thenReturn(Optional.of(testFolder));
        when(documentRepository.countByUserIdAndFolderId(userId, 1L))
                .thenReturn(5L);
        
        // Act & Assert
        assertThatThrownBy(() -> folderService.deleteFolder(userId, 1L))
                .isInstanceOf(FolderNotEmptyException.class)
                .hasMessageContaining("5");
        
        verify(folderRepository, never()).delete(any(Folder.class));
    }
    
    @Test
    void deleteFolder_WithSubfolders_ThrowsException() {
        // Arrange
        List<Folder> subfolders = Arrays.asList(
                Folder.builder().id(2L).parentId(1L).name("Child1").build(),
                Folder.builder().id(3L).parentId(1L).name("Child2").build()
        );
        
        when(folderRepository.findByIdAndUserId(1L, userId))
                .thenReturn(Optional.of(testFolder));
        when(documentRepository.countByUserIdAndFolderId(userId, 1L))
                .thenReturn(0L);
        when(folderRepository.findByParentId(1L))
                .thenReturn(subfolders);
        
        // Act & Assert
        assertThatThrownBy(() -> folderService.deleteFolder(userId, 1L))
                .isInstanceOf(FolderNotEmptyException.class)
                .hasMessageContaining("2");
        
        verify(folderRepository, never()).delete(any(Folder.class));
    }
    
    @Test
    void getDocumentsInFolder_Success() {
        // Arrange
        List<Document> documents = Arrays.asList(
                Document.builder().id(1L).userId(userId).folderId(1L).filename("doc1.pdf").build(),
                Document.builder().id(2L).userId(userId).folderId(1L).filename("doc2.pdf").build()
        );
        
        when(folderRepository.findByIdAndUserId(1L, userId))
                .thenReturn(Optional.of(testFolder));
        when(documentRepository.findByUserIdAndFolderId(userId, 1L))
                .thenReturn(documents);
        
        // Act
        var result = folderService.getDocumentsInFolder(userId, 1L);
        
        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getFilename()).isEqualTo("doc1.pdf");
        assertThat(result.get(1).getFilename()).isEqualTo("doc2.pdf");
    }
    
    @Test
    void getDocumentsInFolder_FolderNotFound_ThrowsException() {
        // Arrange
        when(folderRepository.findByIdAndUserId(1L, userId))
                .thenReturn(Optional.empty());
        
        // Act & Assert
        assertThatThrownBy(() -> folderService.getDocumentsInFolder(userId, 1L))
                .isInstanceOf(FolderNotFoundException.class);
    }
}
