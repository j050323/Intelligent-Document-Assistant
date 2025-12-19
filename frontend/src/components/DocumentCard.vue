<template>
  <el-card
    class="document-card"
    :class="{ selected: selected }"
    shadow="hover"
    @click="handleCardClick"
  >
    <template #header>
      <div class="card-header">
        <el-checkbox
          :model-value="selected"
          @click.stop
          @change="handleSelect"
        />
        <el-dropdown trigger="click" @click.native.stop>
          <el-button :icon="MoreFilled" circle size="small" @click.stop />
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item :icon="View" @click="handlePreview">
                预览
              </el-dropdown-item>
              <el-dropdown-item :icon="Download" @click="handleDownload">
                下载
              </el-dropdown-item>
              <el-dropdown-item :icon="Edit" @click="showRenameDialog">
                重命名
              </el-dropdown-item>
              <el-dropdown-item :icon="FolderOpened" @click="showMoveDialog">
                移动
              </el-dropdown-item>
              <el-dropdown-item :icon="Delete" @click="handleDelete" divided>
                删除
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </template>

    <div class="card-body">
      <!-- File Icon -->
      <div class="file-icon">
        <el-icon :size="56" :color="getFileIconColor()">
          <component :is="getFileIcon()" />
        </el-icon>
      </div>

      <!-- File Info -->
      <div class="file-info">
        <el-tooltip :content="document.originalFilename" placement="top">
          <div class="file-name">{{ document.originalFilename }}</div>
        </el-tooltip>
        
        <div class="file-type-badge">
          <el-tag :type="getFileTypeTagType()" size="small">
            {{ document.fileType.toUpperCase() }}
          </el-tag>
        </div>

        <div class="file-meta-grid">
          <div class="meta-item">
            <span class="meta-label">大小</span>
            <span class="meta-value">{{ formatFileSize(document.fileSize) }}</span>
          </div>
          <div class="meta-item">
            <span class="meta-label">上传</span>
            <span class="meta-value">{{ formatDate(document.createdAt) }}</span>
          </div>
        </div>

        <div v-if="document.folderName" class="file-folder">
          <el-icon><Folder /></el-icon>
          <span>{{ document.folderName }}</span>
        </div>

        <!-- Quick Actions -->
        <div class="quick-actions">
          <el-button
            type="primary"
            size="small"
            :icon="View"
            @click.stop="handlePreview"
          >
            预览
          </el-button>
          <el-button
            size="small"
            :icon="Download"
            @click.stop="handleDownload"
          >
            下载
          </el-button>
        </div>
      </div>
    </div>

    <!-- Rename Dialog -->
    <el-dialog
      v-model="renameDialogVisible"
      title="重命名文档"
      width="400px"
      @click.stop
    >
      <el-form :model="renameForm" label-width="80px">
        <el-form-item label="文件名">
          <el-input
            v-model="renameForm.filename"
            placeholder="请输入新文件名"
            @keyup.enter="handleRename"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="renameDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleRename">确定</el-button>
      </template>
    </el-dialog>

    <!-- Move Dialog -->
    <el-dialog
      v-model="moveDialogVisible"
      title="移动文档"
      width="400px"
      @click.stop
    >
      <el-form :model="moveForm" label-width="80px">
        <el-form-item label="目标文件夹">
          <el-select
            v-model="moveForm.folderId"
            placeholder="选择文件夹"
            clearable
            style="width: 100%"
          >
            <el-option label="根目录" :value="undefined" />
            <el-option
              v-for="folder in folders"
              :key="folder.id"
              :label="folder.name"
              :value="folder.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="moveDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleMove">确定</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import {
  MoreFilled,
  View,
  Download,
  Edit,
  Delete,
  FolderOpened,
  Folder,
  Document as DocumentIcon,
  Reading,
  Tickets
} from '@element-plus/icons-vue'
import type { DocumentDTO } from '@/api/document'
import { useFolderStore } from '@/stores/folder'

interface Props {
  document: DocumentDTO
  selected: boolean
}

const props = defineProps<Props>()

const emit = defineEmits<{
  select: [id: number]
  preview: [id: number]
  download: [id: number]
  rename: [id: number, newName: string]
  move: [id: number, folderId: number | undefined]
  delete: [id: number]
}>()

const folderStore = useFolderStore()

// State
const renameDialogVisible = ref(false)
const moveDialogVisible = ref(false)
const renameForm = ref({
  filename: ''
})
const moveForm = ref<{
  folderId: number | undefined
}>({
  folderId: undefined
})

// Computed
const folders = computed(() => folderStore.folders)

// Methods
const handleCardClick = () => {
  handlePreview()
}

const handleSelect = () => {
  emit('select', props.document.id)
}

const handlePreview = () => {
  emit('preview', props.document.id)
}

const handleDownload = () => {
  emit('download', props.document.id)
}

const showRenameDialog = () => {
  renameForm.value.filename = props.document.filename
  renameDialogVisible.value = true
}

const handleRename = () => {
  if (!renameForm.value.filename.trim()) {
    return
  }
  emit('rename', props.document.id, renameForm.value.filename)
  renameDialogVisible.value = false
}

const showMoveDialog = () => {
  moveForm.value.folderId = props.document.folderId
  moveDialogVisible.value = true
}

const handleMove = () => {
  emit('move', props.document.id, moveForm.value.folderId)
  moveDialogVisible.value = false
}

const handleDelete = () => {
  emit('delete', props.document.id)
}

const getFileIcon = () => {
  const fileType = props.document.fileType.toLowerCase()
  switch (fileType) {
    case 'pdf':
      return Reading
    case 'doc':
    case 'docx':
      return Tickets
    case 'txt':
      return DocumentIcon
    default:
      return DocumentIcon
  }
}

const getFileIconColor = () => {
  const fileType = props.document.fileType.toLowerCase()
  switch (fileType) {
    case 'pdf':
      return '#f56c6c'
    case 'doc':
    case 'docx':
      return '#409eff'
    case 'txt':
      return '#67c23a'
    default:
      return '#909399'
  }
}

const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
}

const formatDate = (dateString: string): string => {
  const date = new Date(dateString)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))

  if (days === 0) {
    return '今天'
  } else if (days === 1) {
    return '昨天'
  } else if (days < 7) {
    return `${days} 天前`
  } else {
    return date.toLocaleDateString('zh-CN')
  }
}

const getFileTypeTagType = () => {
  const fileType = props.document.fileType.toLowerCase()
  switch (fileType) {
    case 'pdf':
      return 'danger'
    case 'doc':
    case 'docx':
      return 'primary'
    case 'txt':
      return 'success'
    default:
      return 'info'
  }
}
</script>

<style scoped>
.document-card {
  cursor: pointer;
  transition: all 0.3s;
  border: 2px solid transparent;
}

.document-card:hover {
  transform: translateY(-2px);
}

.document-card.selected {
  border-color: #409eff;
  background-color: #ecf5ff;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0;
}

.card-body {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 10px 0;
}

.file-icon {
  margin-bottom: 15px;
}

.file-info {
  width: 100%;
  text-align: center;
}

.file-name {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 10px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  padding: 0 5px;
}

.file-type-badge {
  margin-bottom: 12px;
}

.file-meta-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-bottom: 10px;
  padding: 10px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.meta-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.meta-label {
  font-size: 11px;
  color: #909399;
  font-weight: 500;
}

.meta-value {
  font-size: 13px;
  color: #606266;
  font-weight: 600;
}

.file-folder {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  font-size: 12px;
  color: #606266;
  margin-bottom: 12px;
  padding: 4px 8px;
  background-color: #e6f7ff;
  border-radius: 4px;
  display: inline-flex;
}

.quick-actions {
  display: flex;
  gap: 8px;
  justify-content: center;
  margin-top: 10px;
}

:deep(.el-card__header) {
  padding: 10px 15px;
}

:deep(.el-card__body) {
  padding: 15px;
}
</style>
