package com.docassistant.document.service;

import com.docassistant.document.dto.CreateFolderRequest;
import com.docassistant.document.dto.FolderDTO;
import com.docassistant.document.dto.UpdateFolderRequest;
import com.docassistant.document.entity.Document;
import com.docassistant.document.entity.Folder;
import com.docassistant.document.repository.DocumentRepository;
import com.docassistant.document.repository.FolderRepository;
import com.docassistant.document.service.impl.FolderServiceImpl;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.LongRange;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * FolderService Property-Based Tests
 * Using jqwik for property-based testing
 */
class FolderServicePropertyTest {
    
    /**
     * Feature: document-management, Property 14: Folder hierarchy structure correctness
     * Validates: Requirements 5.2
     * 
     * For any subfolder creation operation, should correctly establish parent-child relationship
     * and support multi-level structure
     */
    @Property(tries = 100)
    void folderHierarchyStructureCorrectness(
            @ForAll @LongRange(min = 1, max = 10000) Long userId,
            @ForAll("folderName") String parentFolderName,
            @ForAll("folderName") String childFolderName,
            @ForAll @LongRange(min = 1, max = 1000) Long parentFolderId) {
        
        // Ensure parent and child folder names are different
        Assume.that(!parentFolderName.equals(childFolderName));
        
        // Create mock objects
        FolderRepository folderRepository = mock(FolderRepository.class);
        DocumentRepository documentRepository = mock(DocumentRepository.class);
        
        // Create parent folder
        Folder parentFolder = Folder.builder()
                .id(parentFolderId)
                .userId(userId)
                .parentId(null)
                .name(parentFolderName)
                .path("/" + parentFolderName)
                .createdAt(LocalDateTime.now())
                .build();
        
        // Setup mock behavior for parent folder
        when(folderRepository.findByIdAndUserId(eq(parentFolderId), eq(userId)))
                .thenReturn(Optional.of(parentFolder));
        
        // Setup mock behavior for name validation
        when(folderRepository.existsByUserIdAndParentIdAndName(eq(userId), eq(parentFolderId), eq(childFolderName)))
                .thenReturn(false);
        
        // Setup mock behavior for save
        when(folderRepository.save(any(Folder.class))).thenAnswer(invocation -> {
            Folder folder = invocation.getArgument(0);
            if (folder.getId() == null) {
                folder.setId(System.nanoTime());
            }
            return folder;
        });
        
        // Create service instance
        FolderService folderService = new FolderServiceImpl(folderRepository, documentRepository);
        
        // Create child folder
        CreateFolderRequest request = CreateFolderRequest.builder()
                .name(childFolderName)
                .parentId(parentFolderId)
                .build();
        
        FolderDTO result = folderService.createFolder(userId, request);
        
        // Verify child folder was created with correct parent-child relationship
        if (result == null) {
            throw new AssertionError("Child folder creation result should not be null");
        }
        
        if (!result.getParentId().equals(parentFolderId)) {
            throw new AssertionError(
                    String.format("Parent-child relationship incorrect, expected parent ID: %d, actual: %d",
                            parentFolderId, result.getParentId())
            );
        }
        
        // Verify path is correctly calculated
        String expectedPath = "/" + parentFolderName + "/" + childFolderName;
        if (!result.getPath().equals(expectedPath)) {
            throw new AssertionError(
                    String.format("Folder path incorrect, expected: %s, actual: %s",
                            expectedPath, result.getPath())
            );
        }
        
        // Verify folder name is correct
        if (!result.getName().equals(childFolderName)) {
            throw new AssertionError(
                    String.format("Folder name incorrect, expected: %s, actual: %s",
                            childFolderName, result.getName())
            );
        }
        
        // Verify save was called
        verify(folderRepository, times(1)).save(any(Folder.class));
    }
    
    /**
     * Test multi-level folder hierarchy (3 levels)
     */
    @Property(tries = 100)
    void multiLevelFolderHierarchy(
            @ForAll @LongRange(min = 1, max = 10000) Long userId,
            @ForAll("folderName") String level1Name,
            @ForAll("folderName") String level2Name,
            @ForAll("folderName") String level3Name) {
        
        // Ensure all folder names are different
        Assume.that(!level1Name.equals(level2Name));
        Assume.that(!level1Name.equals(level3Name));
        Assume.that(!level2Name.equals(level3Name));
        
        // Create mock objects
        FolderRepository folderRepository = mock(FolderRepository.class);
        DocumentRepository documentRepository = mock(DocumentRepository.class);
        
        // Create level 1 folder (root)
        Folder level1Folder = Folder.builder()
                .id(1L)
                .userId(userId)
                .parentId(null)
                .name(level1Name)
                .path("/" + level1Name)
                .createdAt(LocalDateTime.now())
                .build();
        
        // Create level 2 folder
        Folder level2Folder = Folder.builder()
                .id(2L)
                .userId(userId)
                .parentId(1L)
                .name(level2Name)
                .path("/" + level1Name + "/" + level2Name)
                .createdAt(LocalDateTime.now())
                .build();
        
        // Setup mock behavior
        when(folderRepository.findByIdAndUserId(eq(1L), eq(userId)))
                .thenReturn(Optional.of(level1Folder));
        when(folderRepository.findByIdAndUserId(eq(2L), eq(userId)))
                .thenReturn(Optional.of(level2Folder));
        
        when(folderRepository.existsByUserIdAndParentIdAndName(eq(userId), eq(1L), eq(level2Name)))
                .thenReturn(false);
        when(folderRepository.existsByUserIdAndParentIdAndName(eq(userId), eq(2L), eq(level3Name)))
                .thenReturn(false);
        
        when(folderRepository.save(any(Folder.class))).thenAnswer(invocation -> {
            Folder folder = invocation.getArgument(0);
            if (folder.getId() == null) {
                folder.setId(System.nanoTime());
            }
            return folder;
        });
        
        // Create service instance
        FolderService folderService = new FolderServiceImpl(folderRepository, documentRepository);
        
        // Create level 2 folder
        CreateFolderRequest level2Request = CreateFolderRequest.builder()
                .name(level2Name)
                .parentId(1L)
                .build();
        
        FolderDTO level2Result = folderService.createFolder(userId, level2Request);
        
        // Verify level 2 folder path
        String expectedLevel2Path = "/" + level1Name + "/" + level2Name;
        if (!level2Result.getPath().equals(expectedLevel2Path)) {
            throw new AssertionError(
                    String.format("Level 2 folder path incorrect, expected: %s, actual: %s",
                            expectedLevel2Path, level2Result.getPath())
            );
        }
        
        // Create level 3 folder
        CreateFolderRequest level3Request = CreateFolderRequest.builder()
                .name(level3Name)
                .parentId(2L)
                .build();
        
        FolderDTO level3Result = folderService.createFolder(userId, level3Request);
        
        // Verify level 3 folder path
        String expectedLevel3Path = "/" + level1Name + "/" + level2Name + "/" + level3Name;
        if (!level3Result.getPath().equals(expectedLevel3Path)) {
            throw new AssertionError(
                    String.format("Level 3 folder path incorrect, expected: %s, actual: %s",
                            expectedLevel3Path, level3Result.getPath())
            );
        }
        
        // Verify parent-child relationships
        if (!level3Result.getParentId().equals(2L)) {
            throw new AssertionError(
                    String.format("Level 3 folder parent ID incorrect, expected: 2, actual: %d",
                            level3Result.getParentId())
            );
        }
    }
    
    /**
     * Feature: document-management, Property 15: Folder rename does not affect documents
     * Validates: Requirements 5.3
     * 
     * For any folder rename operation, documents in the folder should remain unchanged
     */
    @Property(tries = 100)
    void folderRenameDoesNotAffectDocuments(
            @ForAll @LongRange(min = 1, max = 10000) Long userId,
            @ForAll @LongRange(min = 1, max = 1000) Long folderId,
            @ForAll("folderName") String oldName,
            @ForAll("folderName") String newName,
            @ForAll @IntRange(min = 1, max = 5) int documentCount) {
        
        // Ensure old and new names are different
        Assume.that(!oldName.equals(newName));
        
        // Create mock objects
        FolderRepository folderRepository = mock(FolderRepository.class);
        DocumentRepository documentRepository = mock(DocumentRepository.class);
        
        // Create test folder
        Folder folder = Folder.builder()
                .id(folderId)
                .userId(userId)
                .parentId(null)
                .name(oldName)
                .path("/" + oldName)
                .createdAt(LocalDateTime.now())
                .build();
        
        // Create test documents in the folder
        List<Document> documents = new ArrayList<>();
        for (int i = 0; i < documentCount; i++) {
            Document doc = Document.builder()
                    .id((long) i)
                    .userId(userId)
                    .folderId(folderId)
                    .filename("file" + i + ".pdf")
                    .originalFilename("file" + i + ".pdf")
                    .filePath("/path/file" + i + ".pdf")
                    .fileType("pdf")
                    .fileSize(1024L)
                    .createdAt(LocalDateTime.now())
                    .build();
            documents.add(doc);
        }
        
        // Setup mock behavior
        when(folderRepository.findByIdAndUserId(eq(folderId), eq(userId)))
                .thenReturn(Optional.of(folder));
        
        when(folderRepository.existsByUserIdAndNullParentIdAndName(eq(userId), eq(newName)))
                .thenReturn(false);
        
        when(folderRepository.findByParentId(eq(folderId)))
                .thenReturn(new ArrayList<>());
        
        when(folderRepository.save(any(Folder.class))).thenAnswer(invocation -> {
            Folder f = invocation.getArgument(0);
            return f;
        });
        
        when(documentRepository.findByUserIdAndFolderId(eq(userId), eq(folderId)))
                .thenReturn(documents);
        
        // Create service instance
        FolderService folderService = new FolderServiceImpl(folderRepository, documentRepository);
        
        // Get documents before rename
        List<com.docassistant.document.dto.DocumentDTO> documentsBefore = 
                folderService.getDocumentsInFolder(userId, folderId);
        
        int documentCountBefore = documentsBefore.size();
        List<Long> documentIdsBefore = documentsBefore.stream()
                .map(com.docassistant.document.dto.DocumentDTO::getId)
                .toList();
        
        // Rename folder
        UpdateFolderRequest request = UpdateFolderRequest.builder()
                .name(newName)
                .build();
        
        FolderDTO result = folderService.updateFolder(userId, folderId, request);
        
        // Verify folder was renamed
        if (!result.getName().equals(newName)) {
            throw new AssertionError(
                    String.format("Folder name should be updated, expected: %s, actual: %s",
                            newName, result.getName())
            );
        }
        
        // Get documents after rename
        List<com.docassistant.document.dto.DocumentDTO> documentsAfter = 
                folderService.getDocumentsInFolder(userId, folderId);
        
        // Verify document count remains the same
        if (documentsAfter.size() != documentCountBefore) {
            throw new AssertionError(
                    String.format("Document count should not change after folder rename, expected: %d, actual: %d",
                            documentCountBefore, documentsAfter.size())
            );
        }
        
        // Verify all document IDs remain the same
        List<Long> documentIdsAfter = documentsAfter.stream()
                .map(com.docassistant.document.dto.DocumentDTO::getId)
                .toList();
        
        if (!documentIdsBefore.equals(documentIdsAfter)) {
            throw new AssertionError(
                    "Document IDs should not change after folder rename"
            );
        }
        
        // Verify all documents still belong to the same folder
        for (com.docassistant.document.dto.DocumentDTO doc : documentsAfter) {
            if (!doc.getFolderId().equals(folderId)) {
                throw new AssertionError(
                        String.format("Document folder ID should not change, expected: %d, actual: %d",
                                folderId, doc.getFolderId())
                );
            }
        }
    }
    
    /**
     * Feature: document-management, Property 16: Non-empty folder deletion is rejected
     * Validates: Requirements 5.5
     * 
     * For any folder containing documents, deletion operation should be rejected
     */
    @Property(tries = 100)
    void nonEmptyFolderDeletionIsRejected(
            @ForAll @LongRange(min = 1, max = 10000) Long userId,
            @ForAll @LongRange(min = 1, max = 1000) Long folderId,
            @ForAll("folderName") String folderName,
            @ForAll @IntRange(min = 1, max = 10) int documentCount) {
        
        // Create mock objects
        FolderRepository folderRepository = mock(FolderRepository.class);
        DocumentRepository documentRepository = mock(DocumentRepository.class);
        
        // Create test folder
        Folder folder = Folder.builder()
                .id(folderId)
                .userId(userId)
                .parentId(null)
                .name(folderName)
                .path("/" + folderName)
                .createdAt(LocalDateTime.now())
                .build();
        
        // Setup mock behavior
        when(folderRepository.findByIdAndUserId(eq(folderId), eq(userId)))
                .thenReturn(Optional.of(folder));
        
        // Folder contains documents
        when(documentRepository.countByUserIdAndFolderId(eq(userId), eq(folderId)))
                .thenReturn((long) documentCount);
        
        when(folderRepository.findByParentId(eq(folderId)))
                .thenReturn(new ArrayList<>());
        
        // Create service instance
        FolderService folderService = new FolderServiceImpl(folderRepository, documentRepository);
        
        // Attempt to delete non-empty folder
        boolean exceptionThrown = false;
        String exceptionMessage = null;
        
        try {
            folderService.deleteFolder(userId, folderId);
        } catch (com.docassistant.document.exception.FolderNotEmptyException e) {
            exceptionThrown = true;
            exceptionMessage = e.getMessage();
        } catch (Exception e) {
            throw new AssertionError(
                    "Should throw FolderNotEmptyException, but threw " + e.getClass().getName()
            );
        }
        
        if (!exceptionThrown) {
            throw new AssertionError(
                    String.format("Deleting non-empty folder (with %d documents) should throw exception", documentCount)
            );
        }
        
        if (exceptionMessage == null || 
            (!exceptionMessage.contains("not empty") && !exceptionMessage.contains("不为空"))) {
            throw new AssertionError(
                    String.format("Exception message should indicate folder is not empty, actual: %s", exceptionMessage)
            );
        }
        
        // Verify folder was not deleted
        verify(folderRepository, never()).delete(any(Folder.class));
    }
    
    /**
     * Test that folder with subfolders cannot be deleted
     */
    @Property(tries = 100)
    void folderWithSubfoldersCannotBeDeleted(
            @ForAll @LongRange(min = 1, max = 10000) Long userId,
            @ForAll @LongRange(min = 1, max = 1000) Long folderId,
            @ForAll("folderName") String folderName,
            @ForAll @IntRange(min = 1, max = 5) int subfolderCount) {
        
        // Create mock objects
        FolderRepository folderRepository = mock(FolderRepository.class);
        DocumentRepository documentRepository = mock(DocumentRepository.class);
        
        // Create test folder
        Folder folder = Folder.builder()
                .id(folderId)
                .userId(userId)
                .parentId(null)
                .name(folderName)
                .path("/" + folderName)
                .createdAt(LocalDateTime.now())
                .build();
        
        // Create subfolders
        List<Folder> subfolders = new ArrayList<>();
        for (int i = 0; i < subfolderCount; i++) {
            Folder subfolder = Folder.builder()
                    .id(folderId + i + 1)
                    .userId(userId)
                    .parentId(folderId)
                    .name("subfolder" + i)
                    .path("/" + folderName + "/subfolder" + i)
                    .createdAt(LocalDateTime.now())
                    .build();
            subfolders.add(subfolder);
        }
        
        // Setup mock behavior
        when(folderRepository.findByIdAndUserId(eq(folderId), eq(userId)))
                .thenReturn(Optional.of(folder));
        
        // Folder has no documents
        when(documentRepository.countByUserIdAndFolderId(eq(userId), eq(folderId)))
                .thenReturn(0L);
        
        // Folder has subfolders
        when(folderRepository.findByParentId(eq(folderId)))
                .thenReturn(subfolders);
        
        // Create service instance
        FolderService folderService = new FolderServiceImpl(folderRepository, documentRepository);
        
        // Attempt to delete folder with subfolders
        boolean exceptionThrown = false;
        String exceptionMessage = null;
        
        try {
            folderService.deleteFolder(userId, folderId);
        } catch (com.docassistant.document.exception.FolderNotEmptyException e) {
            exceptionThrown = true;
            exceptionMessage = e.getMessage();
        } catch (Exception e) {
            throw new AssertionError(
                    "Should throw FolderNotEmptyException, but threw " + e.getClass().getName()
            );
        }
        
        if (!exceptionThrown) {
            throw new AssertionError(
                    String.format("Deleting folder with %d subfolders should throw exception", subfolderCount)
            );
        }
        
        if (exceptionMessage == null || 
            (!exceptionMessage.contains("not empty") && !exceptionMessage.contains("不为空"))) {
            throw new AssertionError(
                    String.format("Exception message should indicate folder is not empty, actual: %s", exceptionMessage)
            );
        }
        
        // Verify folder was not deleted
        verify(folderRepository, never()).delete(any(Folder.class));
    }
    
    /**
     * Test that empty folder can be deleted successfully
     */
    @Property(tries = 100)
    void emptyFolderCanBeDeleted(
            @ForAll @LongRange(min = 1, max = 10000) Long userId,
            @ForAll @LongRange(min = 1, max = 1000) Long folderId,
            @ForAll("folderName") String folderName) {
        
        // Create mock objects
        FolderRepository folderRepository = mock(FolderRepository.class);
        DocumentRepository documentRepository = mock(DocumentRepository.class);
        
        // Create test folder
        Folder folder = Folder.builder()
                .id(folderId)
                .userId(userId)
                .parentId(null)
                .name(folderName)
                .path("/" + folderName)
                .createdAt(LocalDateTime.now())
                .build();
        
        // Setup mock behavior
        when(folderRepository.findByIdAndUserId(eq(folderId), eq(userId)))
                .thenReturn(Optional.of(folder));
        
        // Folder is empty (no documents)
        when(documentRepository.countByUserIdAndFolderId(eq(userId), eq(folderId)))
                .thenReturn(0L);
        
        // Folder is empty (no subfolders)
        when(folderRepository.findByParentId(eq(folderId)))
                .thenReturn(new ArrayList<>());
        
        // Create service instance
        FolderService folderService = new FolderServiceImpl(folderRepository, documentRepository);
        
        // Delete empty folder - should succeed
        folderService.deleteFolder(userId, folderId);
        
        // Verify folder was deleted
        verify(folderRepository, times(1)).delete(eq(folder));
    }
    
    /**
     * Provide folder name
     */
    @Provide
    Arbitrary<String> folderName() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(20);
    }
}
