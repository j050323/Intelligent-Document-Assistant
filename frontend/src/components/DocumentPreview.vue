<template>
  <el-dialog
    v-model="dialogVisible"
    width="90%"
    :fullscreen="fullscreen"
    :close-on-click-modal="false"
    custom-class="preview-dialog-custom"
    append-to-body
    destroy-on-close
  >
    <template #header>
      <div class="preview-header">
        <span class="title">{{ documentTitle }}</span>
        <div class="actions">
          <el-button
            :icon="fullscreen ? FullScreen : FullScreen"
            circle
            @click="toggleFullscreen"
          />
          <el-button :icon="Download" circle @click="handleDownload" />
        </div>
      </div>
    </template>

    <div v-loading="loading" class="preview-content">
      <!-- PDF Preview -->
      <div v-if="previewType === 'pdf'" class="pdf-preview">
        <iframe
          v-if="previewUrl"
          :src="previewUrl"
          width="100%"
          height="100%"
          frameborder="0"
        />
      </div>

      <!-- Text Preview -->
      <div v-else-if="previewType === 'text'" class="text-preview">
        <pre>{{ previewContent }}</pre>
      </div>

      <!-- Word Preview (Download prompt) -->
      <div v-else-if="previewType === 'word'" class="word-preview">
        <el-result
          icon="info"
          title="Word 文档预览"
          sub-title="Word 文档需要下载后查看"
        >
          <template #extra>
            <el-button type="primary" :icon="Download" @click="handleDownload">
              下载文档
            </el-button>
          </template>
        </el-result>
      </div>

      <!-- Error State -->
      <div v-else-if="error" class="error-state">
        <el-result icon="error" title="预览失败" :sub-title="errorMessage">
          <template #extra>
            <el-button type="primary" @click="loadPreview">重试</el-button>
          </template>
        </el-result>
      </div>

      <!-- Loading State -->
      <div v-else class="loading-state">
        <el-empty description="加载中..." />
      </div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { FullScreen, Download } from '@element-plus/icons-vue'
import { documentApi } from '@/api/document'
import type { DocumentDTO } from '@/api/document'

interface Props {
  visible: boolean
  documentId: number | null
}

const props = defineProps<Props>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
}>()

// State
const loading = ref(false)
const error = ref(false)
const errorMessage = ref('')
const fullscreen = ref(false)
const previewType = ref<'pdf' | 'text' | 'word' | null>(null)
const previewUrl = ref<string | null>(null)
const previewContent = ref<string | null>(null)
const document = ref<DocumentDTO | null>(null)

// Computed
const dialogVisible = computed({
  get: () => props.visible,
  set: (value) => emit('update:visible', value)
})

const documentTitle = computed(() => {
  return document.value?.originalFilename || '文档预览'
})

const dialogStyle = computed(() => {
  if (fullscreen.value) {
    return {
      height: '100vh',
      maxHeight: '100vh',
      margin: '0'
    }
  }
  return {
    height: '85vh',
    maxHeight: '85vh',
    margin: '5vh auto'
  }
})

// Methods
const loadPreview = async () => {
  if (!props.documentId) return

  loading.value = true
  error.value = false
  errorMessage.value = ''
  previewType.value = null
  previewUrl.value = null
  previewContent.value = null

  try {
    // Load document info
    const docResponse = await documentApi.getDocument(props.documentId)
    document.value = docResponse.data

    // Determine preview type based on file type
    const fileType = document.value.fileType.toLowerCase()
    
    if (fileType === 'pdf') {
      previewType.value = 'pdf'
      // For PDF, download the file and create a blob URL for preview
      const downloadResponse = await documentApi.downloadDocument(props.documentId)
      const blob = new Blob([downloadResponse.data], { type: 'application/pdf' })
      previewUrl.value = URL.createObjectURL(blob)
    } else if (fileType === 'txt') {
      previewType.value = 'text'
      const previewResponse = await documentApi.previewDocument(props.documentId)
      previewContent.value = previewResponse.data.content
    } else if (fileType === 'doc' || fileType === 'docx') {
      previewType.value = 'word'
      // Word documents typically need to be downloaded
    } else {
      throw new Error('不支持的文件类型')
    }
  } catch (err: any) {
    error.value = true
    errorMessage.value = err.response?.data?.message || err.message || '预览失败'
    ElMessage.error(errorMessage.value)
  } finally {
    loading.value = false
  }
}

const handleDownload = async () => {
  if (!props.documentId || !document.value) return

  try {
    const response = await documentApi.downloadDocument(props.documentId)
    const url = window.URL.createObjectURL(new Blob([response.data]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', document.value.originalFilename)
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    
    ElMessage.success('下载成功')
  } catch (err: any) {
    ElMessage.error(err.response?.data?.message || '下载失败')
  }
}

const toggleFullscreen = () => {
  fullscreen.value = !fullscreen.value
}

const cleanup = () => {
  if (previewUrl.value && previewUrl.value.startsWith('blob:')) {
    URL.revokeObjectURL(previewUrl.value)
  }
  previewUrl.value = null
  previewContent.value = null
  previewType.value = null
  document.value = null
  error.value = false
  errorMessage.value = ''
}

// Force dialog height after it opens
const forceDialogHeight = () => {
  // Try multiple times to ensure DOM is ready
  const attempts = [0, 50, 100]
  attempts.forEach(delay => {
    setTimeout(() => {
      const dialogs = document.querySelectorAll('.el-dialog.preview-dialog')
      dialogs.forEach((dialog: Element) => {
        const htmlDialog = dialog as HTMLElement
        if (fullscreen.value) {
          htmlDialog.style.setProperty('--el-dialog-margin-top', '0', 'important')
          htmlDialog.style.setProperty('height', '100vh', 'important')
          htmlDialog.style.setProperty('max-height', '100vh', 'important')
          htmlDialog.style.setProperty('margin', '0', 'important')
        } else {
          htmlDialog.style.setProperty('--el-dialog-margin-top', '5vh', 'important')
          htmlDialog.style.setProperty('height', '85vh', 'important')
          htmlDialog.style.setProperty('max-height', '85vh', 'important')
          htmlDialog.style.setProperty('margin-bottom', '5vh', 'important')
        }
      })
    }, delay)
  })
}

// Watch for dialog visibility and document ID changes
watch(() => props.visible, (visible) => {
  if (visible && props.documentId) {
    loadPreview()
    forceDialogHeight()
  } else if (!visible) {
    cleanup()
  }
})

watch(() => props.documentId, (newId) => {
  if (newId && props.visible) {
    loadPreview()
  }
})

watch(() => fullscreen.value, () => {
  forceDialogHeight()
})
</script>

<style>
/* Global styles with maximum specificity to override Element Plus */
.el-dialog.preview-dialog-custom {
  height: 85vh !important;
  max-height: 85vh !important;
  display: flex !important;
  flex-direction: column !important;
  margin: 5vh auto !important;
}

.el-dialog.preview-dialog-custom.is-fullscreen {
  height: 100vh !important;
  max-height: 100vh !important;
  width: 100vw !important;
  margin: 0 !important;
}

.el-dialog.preview-dialog-custom .el-dialog__header {
  flex-shrink: 0 !important;
  padding: 20px !important;
}

.el-dialog.preview-dialog-custom .el-dialog__body {
  flex: 1 !important;
  padding: 0 !important;
  overflow: hidden !important;
  display: flex !important;
  flex-direction: column !important;
  min-height: 0 !important;
}
</style>

<style scoped>
.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.preview-header .title {
  font-size: 18px;
  font-weight: 500;
  flex: 1;
}

.preview-header .actions {
  display: flex;
  gap: 10px;
}

.preview-content {
  flex: 1;
  height: 100%;
  overflow: auto;
  background-color: #f5f7fa;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.pdf-preview {
  flex: 1;
  width: 100%;
  height: 100%;
  background-color: #525659;
  display: flex;
  overflow: hidden;
  min-height: 0;
}

.pdf-preview iframe {
  width: 100%;
  height: 100%;
  border: none;
}

.text-preview {
  height: 100%;
  padding: 20px;
  background-color: white;
  overflow: auto;
}

.text-preview pre {
  margin: 0;
  font-family: 'Courier New', Courier, monospace;
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-wrap: break-word;
}

.word-preview,
.error-state,
.loading-state {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  background-color: white;
}
</style>
