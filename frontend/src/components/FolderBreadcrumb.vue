<template>
  <div class="folder-breadcrumb">
    <div class="breadcrumb-container">
      <div class="breadcrumb-wrapper">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item @click="handleNavigate(undefined)">
            <el-icon><HomeFilled /></el-icon>
            <span>全部文档</span>
          </el-breadcrumb-item>
          <el-breadcrumb-item
            v-for="folder in breadcrumbPath"
            :key="folder.id"
            @click="handleNavigate(folder.id)"
          >
            <span class="breadcrumb-link">{{ folder.name }}</span>
          </el-breadcrumb-item>
        </el-breadcrumb>
        
        <!-- Quick jump dropdown -->
        <el-dropdown v-if="siblingFolders.length > 0" trigger="click" @command="handleQuickJump">
          <el-button size="small" :icon="More" circle />
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item 
                v-for="folder in siblingFolders" 
                :key="folder.id"
                :command="folder.id"
              >
                <el-icon><Folder /></el-icon>
                {{ folder.name }}
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
      
      <!-- Path display -->
      <div v-if="fullPath" class="path-display">
        <el-icon><FolderOpened /></el-icon>
        <span class="path-text">{{ fullPath }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { HomeFilled, FolderOpened, More, Folder } from '@element-plus/icons-vue'
import { useFolderStore } from '@/stores/folder'

const emit = defineEmits<{
  navigate: [folderId: number | undefined]
}>()

const folderStore = useFolderStore()

// Computed
const breadcrumbPath = computed(() => {
  if (!folderStore.currentFolder) {
    return []
  }
  return folderStore.getFolderPath(folderStore.currentFolder.id)
})

const fullPath = computed(() => {
  if (breadcrumbPath.value.length === 0) {
    return ''
  }
  return '/' + breadcrumbPath.value.map(f => f.name).join('/')
})

// Get sibling folders (folders at the same level as current folder)
const siblingFolders = computed(() => {
  if (!folderStore.currentFolder) {
    return folderStore.rootFolders.slice(0, 10) // Show up to 10 root folders
  }
  
  const siblings = folderStore.getFoldersByParent(folderStore.currentFolder.parentId)
  return siblings.filter(f => f.id !== folderStore.currentFolder?.id).slice(0, 10)
})

// Methods
const handleNavigate = (folderId: number | undefined) => {
  emit('navigate', folderId)
}

const handleQuickJump = (folderId: number) => {
  emit('navigate', folderId)
}
</script>

<style scoped>
.folder-breadcrumb {
  padding: 15px 20px;
  background-color: white;
  border-radius: 4px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  margin-bottom: 15px;
}

.breadcrumb-container {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}

.breadcrumb-wrapper {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
}

.el-breadcrumb-item {
  cursor: pointer;
}

.el-breadcrumb-item:hover {
  color: #409eff;
}

.breadcrumb-link {
  cursor: pointer;
  transition: color 0.3s;
}

.breadcrumb-link:hover {
  color: #409eff;
}

.path-display {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #909399;
  font-size: 13px;
  padding: 5px 12px;
  background-color: #f5f7fa;
  border-radius: 4px;
  flex-shrink: 0;
}

.path-text {
  max-width: 400px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

:deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) {
  color: #303133;
  font-weight: 500;
}

:deep(.el-breadcrumb__item .el-breadcrumb__inner) {
  display: flex;
  align-items: center;
  gap: 5px;
  cursor: pointer;
}

:deep(.el-breadcrumb__item .el-breadcrumb__inner:hover) {
  color: #409eff;
}

:deep(.el-dropdown-menu__item) {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
