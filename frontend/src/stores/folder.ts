import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { FolderDTO } from '@/api/folder'

export const useFolderStore = defineStore('folder', () => {
  // State
  const folders = ref<FolderDTO[]>([])
  const currentFolder = ref<FolderDTO | null>(null)
  const isLoading = ref(false)

  // Getters
  const rootFolders = computed(() => 
    folders.value.filter(f => !f.parentId)
  )

  const getFoldersByParent = computed(() => (parentId: number | undefined) => {
    if (parentId === undefined) {
      return rootFolders.value
    }
    return folders.value.filter(f => f.parentId === parentId)
  })

  const getFolderPath = computed(() => (folderId: number): FolderDTO[] => {
    const path: FolderDTO[] = []
    let currentId: number | undefined = folderId
    
    while (currentId !== undefined) {
      const folder = folders.value.find(f => f.id === currentId)
      if (!folder) break
      path.unshift(folder)
      currentId = folder.parentId
    }
    
    return path
  })

  // Actions
  function setFolders(folderList: FolderDTO[]) {
    folders.value = folderList
  }

  function addFolder(folder: FolderDTO) {
    folders.value.push(folder)
  }

  function updateFolder(id: number, updates: Partial<FolderDTO>) {
    const index = folders.value.findIndex(f => f.id === id)
    if (index !== -1) {
      folders.value[index] = { ...folders.value[index], ...updates }
    }
  }

  function removeFolder(id: number) {
    folders.value = folders.value.filter(f => f.id !== id)
  }

  function setCurrentFolder(folder: FolderDTO | null) {
    currentFolder.value = folder
  }

  function setLoading(loading: boolean) {
    isLoading.value = loading
  }

  return {
    // State
    folders,
    currentFolder,
    isLoading,
    // Getters
    rootFolders,
    getFoldersByParent,
    getFolderPath,
    // Actions
    setFolders,
    addFolder,
    updateFolder,
    removeFolder,
    setCurrentFolder,
    setLoading
  }
})
