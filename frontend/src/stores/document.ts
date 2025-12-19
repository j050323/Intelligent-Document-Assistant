import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { DocumentDTO, DocumentQueryParams, StorageInfo } from '@/api/document'

export const useDocumentStore = defineStore('document', () => {
  // State
  const documents = ref<DocumentDTO[]>([])
  const selectedDocuments = ref<number[]>([])
  const currentPage = ref(0)
  const totalPages = ref(0)
  const totalElements = ref(0)
  const pageSize = ref(20)
  const isLoading = ref(false)
  const uploadProgress = ref<Map<string, number>>(new Map())
  const storageInfo = ref<StorageInfo | null>(null)
  
  // Query filters
  const searchKeyword = ref('')
  const fileTypeFilter = ref<string | undefined>(undefined)
  const folderFilter = ref<number | undefined>(undefined)
  const sortBy = ref('createdAt')
  const sortDirection = ref('DESC')

  // Getters
  const hasSelectedDocuments = computed(() => selectedDocuments.value.length > 0)
  const selectedCount = computed(() => selectedDocuments.value.length)
  const isAllSelected = computed(() => 
    documents.value.length > 0 && selectedDocuments.value.length === documents.value.length
  )

  // Actions
  function setDocuments(docs: DocumentDTO[]) {
    documents.value = docs
  }

  function addDocument(doc: DocumentDTO) {
    documents.value.unshift(doc)
  }

  function updateDocument(id: number, updates: Partial<DocumentDTO>) {
    const index = documents.value.findIndex(d => d.id === id)
    if (index !== -1) {
      documents.value[index] = { ...documents.value[index], ...updates }
    }
  }

  function removeDocument(id: number) {
    documents.value = documents.value.filter(d => d.id !== id)
  }

  function setPagination(page: number, total: number, totalElems: number) {
    currentPage.value = page
    totalPages.value = total
    totalElements.value = totalElems
  }

  function setLoading(loading: boolean) {
    isLoading.value = loading
  }

  function toggleDocumentSelection(id: number) {
    const index = selectedDocuments.value.indexOf(id)
    if (index === -1) {
      selectedDocuments.value.push(id)
    } else {
      selectedDocuments.value.splice(index, 1)
    }
  }

  function selectAllDocuments() {
    selectedDocuments.value = documents.value.map(d => d.id)
  }

  function clearSelection() {
    selectedDocuments.value = []
  }

  function setUploadProgress(fileId: string, progress: number) {
    uploadProgress.value.set(fileId, progress)
  }

  function removeUploadProgress(fileId: string) {
    uploadProgress.value.delete(fileId)
  }

  function clearUploadProgress() {
    uploadProgress.value.clear()
  }

  function setStorageInfo(info: StorageInfo) {
    storageInfo.value = info
  }

  function setSearchKeyword(keyword: string) {
    searchKeyword.value = keyword
  }

  function setFileTypeFilter(fileType: string | undefined) {
    fileTypeFilter.value = fileType
  }

  function setFolderFilter(folderId: number | undefined) {
    folderFilter.value = folderId
  }

  function setSorting(field: string, direction: string) {
    sortBy.value = field
    sortDirection.value = direction
  }

  function getQueryParams(): DocumentQueryParams {
    return {
      page: currentPage.value,
      size: pageSize.value,
      keyword: searchKeyword.value || undefined,
      fileType: fileTypeFilter.value,
      folderId: folderFilter.value,
      sortBy: sortBy.value,
      sortDirection: sortDirection.value
    }
  }

  function resetFilters() {
    searchKeyword.value = ''
    fileTypeFilter.value = undefined
    folderFilter.value = undefined
    currentPage.value = 0
  }

  return {
    // State
    documents,
    selectedDocuments,
    currentPage,
    totalPages,
    totalElements,
    pageSize,
    isLoading,
    uploadProgress,
    storageInfo,
    searchKeyword,
    fileTypeFilter,
    folderFilter,
    sortBy,
    sortDirection,
    // Getters
    hasSelectedDocuments,
    selectedCount,
    isAllSelected,
    // Actions
    setDocuments,
    addDocument,
    updateDocument,
    removeDocument,
    setPagination,
    setLoading,
    toggleDocumentSelection,
    selectAllDocuments,
    clearSelection,
    setUploadProgress,
    removeUploadProgress,
    clearUploadProgress,
    setStorageInfo,
    setSearchKeyword,
    setFileTypeFilter,
    setFolderFilter,
    setSorting,
    getQueryParams,
    resetFilters
  }
})
