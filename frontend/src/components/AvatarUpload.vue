<template>
  <div class="modal-overlay" @click.self="handleClose">
    <div class="modal-content">
      <div class="modal-header">
        <h2>上传头像</h2>
        <button class="close-btn" @click="handleClose">&times;</button>
      </div>
      
      <div class="modal-body">
        <!-- 文件选择区域 -->
        <div 
          class="upload-area"
          :class="{ 'drag-over': isDragOver }"
          @drop.prevent="handleDrop"
          @dragover.prevent="isDragOver = true"
          @dragleave.prevent="isDragOver = false"
          @click="triggerFileInput"
        >
          <input
            ref="fileInput"
            type="file"
            accept="image/jpeg,image/png,image/gif"
            @change="handleFileSelect"
            style="display: none"
          />
          
          <div v-if="!previewUrl" class="upload-placeholder">
            <svg class="upload-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
            </svg>
            <p class="upload-text">点击或拖拽图片到此处上传</p>
            <p class="upload-hint">支持 JPG、PNG、GIF 格式，最大 5MB</p>
          </div>
          
          <div v-else class="preview-container">
            <img :src="previewUrl" alt="预览" class="preview-image" />
            <button class="remove-btn" @click.stop="removeImage">
              <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                  d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
        </div>
        
        <!-- 错误提示 -->
        <div v-if="errorMessage" class="error-message">
          {{ errorMessage }}
        </div>
        
        <!-- 上传进度 -->
        <div v-if="uploading" class="upload-progress">
          <div class="progress-bar">
            <div class="progress-fill" :style="{ width: uploadProgress + '%' }"></div>
          </div>
          <p class="progress-text">上传中... {{ uploadProgress }}%</p>
        </div>
      </div>
      
      <div class="modal-footer">
        <button 
          class="btn btn-secondary" 
          @click="handleClose"
          :disabled="uploading"
        >
          取消
        </button>
        <button 
          class="btn btn-primary" 
          @click="handleUpload"
          :disabled="!selectedFile || uploading"
        >
          {{ uploading ? '上传中...' : '上传' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { userApi } from '@/api/user'

const emit = defineEmits<{
  close: []
  uploaded: [avatarUrl: string]
}>()

const fileInput = ref<HTMLInputElement | null>(null)
const selectedFile = ref<File | null>(null)
const previewUrl = ref<string>('')
const isDragOver = ref(false)
const uploading = ref(false)
const uploadProgress = ref(0)
const errorMessage = ref('')

const MAX_FILE_SIZE = 5 * 1024 * 1024 // 5MB
const ALLOWED_TYPES = ['image/jpeg', 'image/png', 'image/gif']

// 验证文件
const validateFile = (file: File): boolean => {
  errorMessage.value = ''
  
  // 检查文件类型
  if (!ALLOWED_TYPES.includes(file.type)) {
    errorMessage.value = '只支持 JPG、PNG、GIF 格式的图片'
    return false
  }
  
  // 检查文件大小
  if (file.size > MAX_FILE_SIZE) {
    errorMessage.value = '文件大小不能超过 5MB'
    return false
  }
  
  return true
}

// 触发文件选择
const triggerFileInput = () => {
  fileInput.value?.click()
}

// 处理文件选择
const handleFileSelect = (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  
  if (file && validateFile(file)) {
    selectedFile.value = file
    previewUrl.value = URL.createObjectURL(file)
  }
}

// 处理拖拽上传
const handleDrop = (event: DragEvent) => {
  isDragOver.value = false
  const file = event.dataTransfer?.files[0]
  
  if (file && validateFile(file)) {
    selectedFile.value = file
    previewUrl.value = URL.createObjectURL(file)
  }
}

// 移除图片
const removeImage = () => {
  selectedFile.value = null
  previewUrl.value = ''
  errorMessage.value = ''
  if (fileInput.value) {
    fileInput.value.value = ''
  }
}

// 上传头像
const handleUpload = async () => {
  if (!selectedFile.value) return
  
  try {
    uploading.value = true
    uploadProgress.value = 0
    errorMessage.value = ''
    
    // 模拟上传进度
    const progressInterval = setInterval(() => {
      if (uploadProgress.value < 90) {
        uploadProgress.value += 10
      }
    }, 200)
    
    const response = await userApi.uploadAvatar(selectedFile.value)
    
    clearInterval(progressInterval)
    uploadProgress.value = 100
    
    // 通知父组件上传成功
    emit('uploaded', response.data.avatarUrl)
  } catch (error: any) {
    console.error('上传失败:', error)
    errorMessage.value = error.response?.data?.message || '上传失败，请重试'
  } finally {
    uploading.value = false
    uploadProgress.value = 0
  }
}

// 关闭弹窗
const handleClose = () => {
  if (!uploading.value) {
    emit('close')
  }
}
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 1rem;
}

.modal-content {
  background: white;
  border-radius: 12px;
  width: 100%;
  max-width: 500px;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.5rem;
  border-bottom: 1px solid #e5e7eb;
}

.modal-header h2 {
  font-size: 1.5rem;
  color: #1f2937;
  margin: 0;
}

.close-btn {
  background: none;
  border: none;
  font-size: 2rem;
  color: #6b7280;
  cursor: pointer;
  line-height: 1;
  padding: 0;
  width: 2rem;
  height: 2rem;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: color 0.3s;
}

.close-btn:hover {
  color: #1f2937;
}

.modal-body {
  padding: 1.5rem;
}

.upload-area {
  border: 2px dashed #d1d5db;
  border-radius: 8px;
  padding: 2rem;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
  min-height: 250px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.upload-area:hover {
  border-color: #667eea;
  background: #f9fafb;
}

.upload-area.drag-over {
  border-color: #667eea;
  background: #eef2ff;
}

.upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
}

.upload-icon {
  width: 64px;
  height: 64px;
  color: #9ca3af;
}

.upload-text {
  font-size: 1rem;
  color: #374151;
  margin: 0;
}

.upload-hint {
  font-size: 0.875rem;
  color: #6b7280;
  margin: 0;
}

.preview-container {
  position: relative;
  width: 100%;
  max-width: 300px;
  margin: 0 auto;
}

.preview-image {
  width: 100%;
  height: auto;
  border-radius: 8px;
  display: block;
}

.remove-btn {
  position: absolute;
  top: -10px;
  right: -10px;
  width: 32px;
  height: 32px;
  background: #ef4444;
  border: none;
  border-radius: 50%;
  color: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.3s;
}

.remove-btn:hover {
  background: #dc2626;
}

.remove-btn svg {
  width: 20px;
  height: 20px;
}

.error-message {
  margin-top: 1rem;
  padding: 0.75rem;
  background: #fee2e2;
  color: #991b1b;
  border-radius: 6px;
  font-size: 0.875rem;
}

.upload-progress {
  margin-top: 1rem;
}

.progress-bar {
  width: 100%;
  height: 8px;
  background: #e5e7eb;
  border-radius: 4px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: #667eea;
  transition: width 0.3s;
}

.progress-text {
  text-align: center;
  margin-top: 0.5rem;
  color: #6b7280;
  font-size: 0.875rem;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 1rem;
  padding: 1.5rem;
  border-top: 1px solid #e5e7eb;
}

.btn {
  padding: 0.75rem 1.5rem;
  border: none;
  border-radius: 6px;
  font-size: 1rem;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-primary {
  background: #667eea;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: #5568d3;
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-secondary {
  background: #e5e7eb;
  color: #374151;
}

.btn-secondary:hover:not(:disabled) {
  background: #d1d5db;
}

.btn-secondary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
