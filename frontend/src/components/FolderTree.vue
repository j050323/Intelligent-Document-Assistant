<template>
  <div class="folder-tree">
    <div class="tree-header">
      <h3>文件夹</h3>
      <el-button
        type="primary"
        :icon="Plus"
        size="small"
        circle
        @click="showCreateDialog"
      />
    </div>

    <div class="tree-content">
      <!-- Root folder -->
      <div
        class="folder-item root"
        :class="{ active: selectedFolderId === undefined }"
        @click="handleFolderClick(undefined)"
      >
        <el-icon><Folder /></el-icon>
        <span>全部文档</span>
      </div>

      <!-- Folder tree -->
      <el-tree
        :data="treeData"
        :props="treeProps"
        node-key="id"
        :default-expand-all="false"
        :highlight-current="true"
        @node-click="handleNodeClick"
      >
        <template #default="{ node, data }">
          <div class="custom-tree-node">
            <div class="node-label">
              <el-icon><Folder /></el-icon>
              <span>{{ node.label }}</span>
            </div>
            <div class="node-actions">
              <el-button
                :icon="Plus"
                size="small"
                circle
                @click.stop="showCreateDialog(data.id)"
              />
              <el-button
                :icon="Edit"
                size="small"
                circle
                @click.stop="showRenameDialog(data)"
              />
              <el-button
                :icon="Delete"
                size="small"
                circle
                @click.stop="handleDelete(data.id)"
              />
            </div>
          </div>
        </template>
      </el-tree>
    </div>

    <!-- Create Folder Dialog -->
    <el-dialog
      v-model="createDialogVisible"
      title="创建文件夹"
      width="400px"
    >
      <el-form :model="createForm" label-width="80px">
        <el-form-item label="文件夹名">
          <el-input
            v-model="createForm.name"
            placeholder="请输入文件夹名称"
            @keyup.enter="handleCreate"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>

    <!-- Rename Folder Dialog -->
    <el-dialog
      v-model="renameDialogVisible"
      title="重命名文件夹"
      width="400px"
    >
      <el-form :model="renameForm" label-width="80px">
        <el-form-item label="文件夹名">
          <el-input
            v-model="renameForm.name"
            placeholder="请输入新文件夹名称"
            @keyup.enter="handleRename"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="renameDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleRename">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Folder, Plus, Edit, Delete } from '@element-plus/icons-vue'
import { useFolderStore } from '@/stores/folder'
import { folderApi } from '@/api/folder'
import type { FolderDTO } from '@/api/folder'

const emit = defineEmits<{
  folderSelect: [folderId: number | undefined]
}>()

const props = defineProps<{
  selectedFolder?: number | undefined
}>()

const folderStore = useFolderStore()

// State
const selectedFolderId = ref<number | undefined>(undefined)
const createDialogVisible = ref(false)
const renameDialogVisible = ref(false)
const createForm = ref({
  name: '',
  parentId: undefined as number | undefined
})
const renameForm = ref({
  id: 0,
  name: ''
})

// Tree configuration
const treeProps = {
  children: 'children',
  label: 'name'
}

// Computed
const treeData = computed(() => {
  return buildTree(folderStore.folders)
})

// Methods
const buildTree = (folders: FolderDTO[]): any[] => {
  const map = new Map<number, any>()
  const roots: any[] = []

  // Create map of all folders
  folders.forEach(folder => {
    map.set(folder.id, {
      id: folder.id,
      name: folder.name,
      parentId: folder.parentId,
      children: []
    })
  })

  // Build tree structure
  folders.forEach(folder => {
    const node = map.get(folder.id)
    if (folder.parentId && map.has(folder.parentId)) {
      const parent = map.get(folder.parentId)
      parent.children.push(node)
    } else {
      roots.push(node)
    }
  })

  return roots
}

const handleFolderClick = (folderId: number | undefined) => {
  selectedFolderId.value = folderId
  folderStore.setCurrentFolder(
    folderId ? folderStore.folders.find(f => f.id === folderId) || null : null
  )
  emit('folderSelect', folderId)
}

const handleNodeClick = (data: any) => {
  handleFolderClick(data.id)
}

const showCreateDialog = (parentId?: number) => {
  createForm.value = {
    name: '',
    parentId
  }
  createDialogVisible.value = true
}

const handleCreate = async () => {
  if (!createForm.value.name.trim()) {
    ElMessage.warning('请输入文件夹名称')
    return
  }

  try {
    const response = await folderApi.createFolder({
      name: createForm.value.name,
      parentId: createForm.value.parentId
    })
    folderStore.addFolder(response.data)
    createDialogVisible.value = false
    ElMessage.success('创建成功')
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '创建失败')
  }
}

const showRenameDialog = (folder: any) => {
  renameForm.value = {
    id: folder.id,
    name: folder.name
  }
  renameDialogVisible.value = true
}

const handleRename = async () => {
  if (!renameForm.value.name.trim()) {
    ElMessage.warning('请输入文件夹名称')
    return
  }

  try {
    const response = await folderApi.updateFolder(renameForm.value.id, {
      name: renameForm.value.name
    })
    folderStore.updateFolder(renameForm.value.id, response.data)
    renameDialogVisible.value = false
    ElMessage.success('重命名成功')
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '重命名失败')
  }
}

const handleDelete = async (folderId: number) => {
  try {
    await ElMessageBox.confirm('确定要删除这个文件夹吗？', '确认删除', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await folderApi.deleteFolder(folderId)
    folderStore.removeFolder(folderId)
    
    // If deleted folder was selected, reset selection
    if (selectedFolderId.value === folderId) {
      handleFolderClick(undefined)
    }
    
    ElMessage.success('删除成功')
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || '删除失败')
    }
  }
}

// Watch for external folder selection changes (e.g., from breadcrumb)
watch(() => props.selectedFolder, (newValue) => {
  if (newValue !== selectedFolderId.value) {
    selectedFolderId.value = newValue
  }
}, { immediate: true })
</script>

<style scoped>
.folder-tree {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.tree-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px;
  border-bottom: 1px solid #e4e7ed;
}

.tree-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 500;
  color: #303133;
}

.tree-content {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
}

.folder-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 15px;
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.3s;
  margin-bottom: 5px;
}

.folder-item:hover {
  background-color: #f5f7fa;
}

.folder-item.active {
  background-color: #ecf5ff;
  color: #409eff;
}

.folder-item.root {
  font-weight: 500;
}

.custom-tree-node {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  padding-right: 10px;
}

.node-label {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}

.node-actions {
  display: none;
  gap: 5px;
}

.custom-tree-node:hover .node-actions {
  display: flex;
}

:deep(.el-tree-node__content) {
  height: 36px;
  padding: 0 10px;
}

:deep(.el-tree-node__content:hover) {
  background-color: #f5f7fa;
}

:deep(.el-tree-node.is-current > .el-tree-node__content) {
  background-color: #ecf5ff;
  color: #409eff;
}
</style>
