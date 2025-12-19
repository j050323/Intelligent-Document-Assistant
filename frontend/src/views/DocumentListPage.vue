<template>
  <div class="document-list-page">
    <el-container>
      <!-- 侧边栏 - 文件夹树 -->
      <el-aside width="250px" class="folder-sidebar">
        <FolderTree 
          :selected-folder="currentFolderId"
          @folder-select="handleFolderSelect" 
        />
      </el-aside>

      <!-- 主内容区 -->
      <el-main>
        <!-- 面包屑导航 -->
        <FolderBreadcrumb @navigate="handleFolderSelect" />

        <!-- 顶部工具栏 -->
        <div class="toolbar">
          <div class="toolbar-left">
            <el-button @click="goToProfile" type="primary" plain>
              👤 个人中心
            </el-button>
            <el-input
              v-model="searchKeyword"
              placeholder="搜索文档..."
              :prefix-icon="Search"
              clearable
              style="width: 300px; margin-left: 10px"
              @input="handleSearch"
            />
            <el-select
              v-model="fileTypeFilter"
              placeholder="文件类型"
              clearable
              style="width: 150px; margin-left: 10px"
              @change="handleFilterChange"
            >
              <el-option label="PDF" value="pdf" />
              <el-option label="Word" value="docx" />
              <el-option label="文本" value="txt" />
            </el-select>
          </div>
          <div class="toolbar-right">
            <DocumentUpload @upload-success="handleUploadSuccess" />
            <el-button
              v-if="hasSelectedDocuments"
              type="danger"
              :icon="Delete"
              @click="handleBatchDelete"
            >
              删除选中 ({{ selectedCount }})
            </el-button>
          </div>
        </div>

        <!-- 存储空间信息 -->
        <StorageQuota />

        <!-- 文档列表 -->
        <div v-loading="isLoading" class="document-list">
          <div v-if="documents.length === 0" class="empty-state">
            <el-empty description="暂无文档" />
          </div>
          <div v-else class="document-grid">
            <el-checkbox
              v-model="selectAll"
              class="select-all"
              @change="handleSelectAll"
            >
              全选
            </el-checkbox>
            <DocumentCard
              v-for="doc in documents"
              :key="doc.id"
              :document="doc"
              :selected="selectedDocuments.includes(doc.id)"
              @select="handleDocumentSelect"
              @preview="handlePreview"
              @download="handleDownload"
              @rename="handleRename"
              @move="handleMove"
              @delete="handleDelete"
            />
          </div>
        </div>

        <!-- 分页 -->
        <div v-if="totalPages > 1" class="pagination">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :total="totalElements"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            @current-change="handlePageChange"
            @size-change="handleSizeChange"
          />
        </div>
      </el-main>
    </el-container>

    <!-- 文档预览对话框 -->
    <DocumentPreview
      v-model:visible="previewVisible"
      :document-id="previewDocumentId"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Delete } from '@element-plus/icons-vue'
import { useDocumentStore } from '@/stores/document'
import { useFolderStore } from '@/stores/folder'
import { documentApi } from '@/api/document'
import { folderApi } from '@/api/folder'
import DocumentCard from '@/components/DocumentCard.vue'
import DocumentUpload from '@/components/DocumentUpload.vue'
import DocumentPreview from '@/components/DocumentPreview.vue'
import FolderTree from '@/components/FolderTree.vue'
import FolderBreadcrumb from '@/components/FolderBreadcrumb.vue'
import StorageQuota from '@/components/StorageQuota.vue'

const router = useRouter()
const documentStore = useDocumentStore()
const folderStore = useFolderStore()

// State
const searchKeyword = ref('')
const fileTypeFilter = ref<string | undefined>(undefined)
const previewVisible = ref(false)
const previewDocumentId = ref<number | null>(null)
const currentFolderId = ref<number | undefined>(undefined)

// Computed
const documents = computed(() => documentStore.documents)
const isLoading = computed(() => documentStore.isLoading)
const selectedDocuments = computed(() => documentStore.selectedDocuments)
const hasSelectedDocuments = computed(() => documentStore.hasSelectedDocuments)
const selectedCount = computed(() => documentStore.selectedCount)
const currentPage = computed({
  get: () => documentStore.currentPage + 1, // Element Plus uses 1-based indexing
  set: (val) => documentStore.currentPage = val - 1
})
const pageSize = computed({
  get: () => documentStore.pageSize,
  set: (val) => documentStore.pageSize = val
})
const totalPages = computed(() => documentStore.totalPages)
const totalElements = computed(() => documentStore.totalElements)
const selectAll = computed({
  get: () => documentStore.isAllSelected,
  set: () => {} // Handled by handleSelectAll
})

// Methods
const goToProfile = () => {
  router.push('/profile')
}

const loadDocuments = async () => {
  try {
    documentStore.setLoading(true)
    const params = documentStore.getQueryParams()
    const response = await documentApi.getDocuments(params)
    documentStore.setDocuments(response.data.content)
    documentStore.setPagination(
      response.data.number,
      response.data.totalPages,
      response.data.totalElements
    )
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '加载文档列表失败')
  } finally {
    documentStore.setLoading(false)
  }
}

const loadFolders = async () => {
  try {
    folderStore.setLoading(true)
    const response = await folderApi.getFolders()
    folderStore.setFolders(response.data)
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '加载文件夹列表失败')
  } finally {
    folderStore.setLoading(false)
  }
}

const loadStorageInfo = async () => {
  try {
    const response = await documentApi.getStorageInfo()
    documentStore.setStorageInfo(response.data)
  } catch (error: any) {
    console.error('Failed to load storage info:', error)
  }
}

const handleSearch = () => {
  documentStore.setSearchKeyword(searchKeyword.value)
  documentStore.currentPage = 0
  loadDocuments()
}

const handleFilterChange = () => {
  documentStore.setFileTypeFilter(fileTypeFilter.value)
  documentStore.currentPage = 0
  loadDocuments()
}

const handleFolderSelect = (folderId: number | undefined) => {
  currentFolderId.value = folderId
  documentStore.setFolderFilter(folderId)
  documentStore.currentPage = 0
  
  // Update current folder in store
  if (folderId) {
    const folder = folderStore.folders.find(f => f.id === folderId)
    folderStore.setCurrentFolder(folder || null)
  } else {
    folderStore.setCurrentFolder(null)
  }
  
  loadDocuments()
}

const handlePageChange = () => {
  loadDocuments()
}

const handleSizeChange = () => {
  documentStore.currentPage = 0
  loadDocuments()
}

const handleSelectAll = () => {
  if (documentStore.isAllSelected) {
    documentStore.clearSelection()
  } else {
    documentStore.selectAllDocuments()
  }
}

const handleDocumentSelect = (id: number) => {
  documentStore.toggleDocumentSelection(id)
}

const handleUploadSuccess = () => {
  loadDocuments()
  loadStorageInfo()
}

const handlePreview = (id: number) => {
  previewDocumentId.value = id
  previewVisible.value = true
}

const handleDownload = async (id: number) => {
  try {
    const doc = documents.value.find(d => d.id === id)
    if (!doc) return

    const response = await documentApi.downloadDocument(id)
    const url = window.URL.createObjectURL(new Blob([response.data]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', doc.originalFilename)
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    
    ElMessage.success('下载成功')
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '下载失败')
  }
}

const handleRename = async (id: number, newName: string) => {
  try {
    await documentApi.updateDocument(id, { filename: newName })
    documentStore.updateDocument(id, { filename: newName })
    ElMessage.success('重命名成功')
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '重命名失败')
  }
}

const handleMove = async (id: number, folderId: number | undefined) => {
  try {
    await documentApi.updateDocument(id, { folderId })
    documentStore.updateDocument(id, { folderId })
    ElMessage.success('移动成功')
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '移动失败')
  }
}

const handleDelete = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定要删除这个文档吗？', '确认删除', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await documentApi.deleteDocument(id)
    documentStore.removeDocument(id)
    loadStorageInfo()
    ElMessage.success('删除成功')
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || '删除失败')
    }
  }
}

const handleBatchDelete = async () => {
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedCount.value} 个文档吗？`,
      '确认批量删除',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    const response = await documentApi.batchDelete(selectedDocuments.value)
    
    // Remove successfully deleted documents
    response.data.successIds.forEach(id => {
      documentStore.removeDocument(id)
    })
    
    documentStore.clearSelection()
    loadStorageInfo()
    
    if (response.data.failureCount > 0) {
      ElMessage.warning(
        `成功删除 ${response.data.successCount} 个文档，${response.data.failureCount} 个失败`
      )
    } else {
      ElMessage.success(`成功删除 ${response.data.successCount} 个文档`)
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || '批量删除失败')
    }
  }
}

// Lifecycle
onMounted(() => {
  loadDocuments()
  loadFolders()
  loadStorageInfo()
})

// Watch for filter changes
watch([searchKeyword, fileTypeFilter], () => {
  // Debounce search
  const timer = setTimeout(() => {
    handleSearch()
  }, 300)
  return () => clearTimeout(timer)
})
</script>

<style scoped>
.document-list-page {
  height: 100vh;
  background-color: #f5f7fa;
}

.folder-sidebar {
  background-color: white;
  border-right: 1px solid #e4e7ed;
  overflow-y: auto;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 15px;
  background-color: white;
  border-radius: 4px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.toolbar-left {
  display: flex;
  align-items: center;
}

.toolbar-right {
  display: flex;
  gap: 10px;
}

.document-list {
  min-height: 400px;
}

.empty-state {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
}

.document-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 20px;
  padding: 20px;
}

.select-all {
  grid-column: 1 / -1;
  margin-bottom: 10px;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 20px;
  padding: 20px;
  background-color: white;
  border-radius: 4px;
}
</style>
