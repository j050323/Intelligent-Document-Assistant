<template>
  <div class="document-upload">
    <el-button type="primary" :icon="Upload" @click="dialogVisible = true">
      上传文档
    </el-button>

    <el-dialog
      v-model="dialogVisible"
      title="上传文档"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-upload
        ref="uploadRef"
        class="upload-area"
        drag
        multiple
        :auto-upload="false"
        :on-change="handleFileChange"
        :on-remove="handleFileRemove"
        :before-upload="beforeUpload"
        :file-list="fileList"
        accept=".pdf,.doc,.docx,.txt"
      >
        <el-icon class="el-icon--upload"><upload-filled /></el-icon>
        <div class="el-upload__text">
          拖拽文件到此处或 <em>点击上传</em>
        </div>
        <template #tip>
          <div class="el-upload__tip">
            支持 PDF、Word、TXT 格式，单个文件不超过 100MB
          </div>
        </template>
      </el-upload>

      <!-- 上传进度 -->
      <div v-if="uploading" class="upload-progress">
        <div v-for="(progress, fileId) in uploadProgress" :key="fileId" class="progress-item">
          <span class="file-name">{{ getFileName(fileId) }}</span>
          <el-progress :percentage="progress" :status="progress === 100 ? 'success' : undefined" />
        </div>
      </div>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="handleCancel">取消</el-button>
          <el-button
            type="primary"
            :loading="uploading"
            :disabled="fileList.length === 0"
            @click="handleUpload"
          >
            {{ uploading ? '上传中...' : '开始上传' }}
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Upload, UploadFilled } from '@element-plus/icons-vue'
import type { UploadFile, UploadFiles, UploadInstance } from 'element-plus'
import { documentApi } from '@/api/document'
import { useDocumentStore } from '@/stores/document'
import { useFolderStore } from '@/stores/folder'

const emit = defineEmits<{
  uploadSuccess: []
}>()

const documentStore = useDocumentStore()
const folderStore = useFolderStore()

// State
const dialogVisible = ref(false)
const uploadRef = ref<UploadInstance>()
const fileList = ref<UploadFiles>([])
const uploading = ref(false)
const uploadProgress = ref<Map<string, number>>(new Map())

// Computed
const currentFolderId = computed(() => folderStore.currentFolder?.id)

// File size limit: 100MB
const MAX_FILE_SIZE = 100 * 1024 * 1024

// Supported file types
const SUPPORTED_TYPES = [
  'application/pdf',
  'application/msword',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  'text/plain'
]

const SUPPORTED_EXTENSIONS = ['.pdf', '.doc', '.docx', '.txt']

// Methods
const validateFile = (file: File): boolean => {
  // Check file size
  if (file.size > MAX_FILE_SIZE) {
    ElMessage.error(`文件 ${file.name} 大小超过 100MB`)
    return false
  }

  // Check file type
  const extension = '.' + file.name.split('.').pop()?.toLowerCase()
  if (!SUPPORTED_EXTENSIONS.includes(extension)) {
    ElMessage.error(`文件 ${file.name} 格式不支持，仅支持 PDF、Word、TXT 格式`)
    return false
  }

  return true
}

const beforeUpload = (file: File): boolean => {
  return validateFile(file)
}

const handleFileChange = (file: UploadFile, files: UploadFiles) => {
  // Validate the file
  if (file.raw && !validateFile(file.raw)) {
    // Remove invalid file
    const index = files.findIndex(f => f.uid === file.uid)
    if (index !== -1) {
      files.splice(index, 1)
    }
  }
  fileList.value = files
}

const handleFileRemove = (file: UploadFile, files: UploadFiles) => {
  fileList.value = files
}

const getFileName = (fileId: string): string => {
  const file = fileList.value.find(f => f.uid.toString() === fileId)
  return file?.name || fileId
}

const uploadSingleFile = async (file: UploadFile): Promise<boolean> => {
  if (!file.raw) return false

  const fileId = file.uid.toString()
  uploadProgress.value.set(fileId, 0)

  try {
    // Simulate progress (since we don't have real progress tracking)
    const progressInterval = setInterval(() => {
      const current = uploadProgress.value.get(fileId) || 0
      if (current < 90) {
        uploadProgress.value.set(fileId, current + 10)
      }
    }, 200)

    await documentApi.uploadDocument(file.raw, currentFolderId.value)

    clearInterval(progressInterval)
    uploadProgress.value.set(fileId, 100)

    return true
  } catch (error: any) {
    uploadProgress.value.delete(fileId)
    ElMessage.error(`上传 ${file.name} 失败: ${error.response?.data?.message || '未知错误'}`)
    return false
  }
}

const handleUpload = async () => {
  if (fileList.value.length === 0) {
    ElMessage.warning('请选择要上传的文件')
    return
  }

  uploading.value = true
  uploadProgress.value.clear()

  try {
    let successCount = 0
    let failureCount = 0

    // Upload files one by one
    for (const file of fileList.value) {
      const success = await uploadSingleFile(file)
      if (success) {
        successCount++
      } else {
        failureCount++
      }
    }

    // Show result message
    if (failureCount === 0) {
      ElMessage.success(`成功上传 ${successCount} 个文件`)
    } else if (successCount === 0) {
      ElMessage.error('所有文件上传失败')
    } else {
      ElMessage.warning(`成功上传 ${successCount} 个文件，${failureCount} 个失败`)
    }

    // Emit success event if at least one file was uploaded
    if (successCount > 0) {
      emit('uploadSuccess')
    }

    // Close dialog and reset
    if (failureCount === 0) {
      handleCancel()
    } else {
      // Keep dialog open but clear successful uploads
      fileList.value = fileList.value.filter(file => {
        const fileId = file.uid.toString()
        const progress = uploadProgress.value.get(fileId)
        return progress !== 100
      })
    }
  } finally {
    uploading.value = false
    setTimeout(() => {
      uploadProgress.value.clear()
    }, 2000)
  }
}

const handleCancel = () => {
  if (uploading.value) {
    ElMessage.warning('文件正在上传中，请稍候')
    return
  }

  dialogVisible.value = false
  fileList.value = []
  uploadProgress.value.clear()
  uploadRef.value?.clearFiles()
}
</script>

<style scoped>
.document-upload {
  display: inline-block;
}

.upload-area {
  margin-bottom: 20px;
}

.upload-progress {
  margin-top: 20px;
  max-height: 300px;
  overflow-y: auto;
}

.progress-item {
  margin-bottom: 15px;
}

.file-name {
  display: block;
  margin-bottom: 5px;
  font-size: 14px;
  color: #606266;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

:deep(.el-upload-dragger) {
  padding: 40px;
}

:deep(.el-icon--upload) {
  font-size: 67px;
  color: #409eff;
  margin-bottom: 16px;
}

:deep(.el-upload__text) {
  font-size: 14px;
  color: #606266;
}

:deep(.el-upload__text em) {
  color: #409eff;
  font-style: normal;
}

:deep(.el-upload__tip) {
  margin-top: 7px;
  font-size: 12px;
  color: #909399;
}
</style>
