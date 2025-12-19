<template>
  <div v-if="storageInfo" class="storage-quota">
    <el-card shadow="never">
      <div class="storage-header">
        <div class="storage-title">
          <el-icon><Folder /></el-icon>
          <span>存储空间</span>
        </div>
        <div class="storage-stats">
          <span class="used">{{ formatSize(storageInfo.usedSpace) }}</span>
          <span class="separator">/</span>
          <span class="total">{{ formatSize(storageInfo.totalQuota) }}</span>
        </div>
      </div>

      <div class="storage-progress">
        <el-progress
          :percentage="storageInfo.usagePercentage"
          :status="getProgressStatus()"
          :stroke-width="12"
        />
      </div>

      <div v-if="storageInfo.nearLimit" class="storage-warning">
        <el-alert
          type="warning"
          :closable="false"
          show-icon
        >
          <template #title>
            存储空间即将用尽，剩余 {{ formatSize(storageInfo.remainingSpace) }}
          </template>
        </el-alert>
      </div>

      <div class="storage-details">
        <div class="detail-item">
          <span class="label">已使用:</span>
          <span class="value">{{ formatSize(storageInfo.usedSpace) }}</span>
        </div>
        <div class="detail-item">
          <span class="label">剩余:</span>
          <span class="value">{{ formatSize(storageInfo.remainingSpace) }}</span>
        </div>
        <div class="detail-item">
          <span class="label">使用率:</span>
          <span class="value">{{ storageInfo.usagePercentage.toFixed(1) }}%</span>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Folder } from '@element-plus/icons-vue'
import { useDocumentStore } from '@/stores/document'

const documentStore = useDocumentStore()

// Computed
const storageInfo = computed(() => documentStore.storageInfo)

// Methods
const formatSize = (bytes: number): string => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return (bytes / Math.pow(k, i)).toFixed(2) + ' ' + sizes[i]
}

const getProgressStatus = () => {
  if (!storageInfo.value) return undefined
  
  const percentage = storageInfo.value.usagePercentage
  if (percentage >= 90) {
    return 'exception'
  } else if (percentage >= 70) {
    return 'warning'
  }
  return undefined
}
</script>

<style scoped>
.storage-quota {
  margin-bottom: 20px;
}

.storage-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.storage-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 500;
  color: #303133;
}

.storage-stats {
  font-size: 14px;
  color: #606266;
}

.storage-stats .used {
  font-weight: 600;
  color: #409eff;
}

.storage-stats .separator {
  margin: 0 5px;
  color: #909399;
}

.storage-stats .total {
  color: #909399;
}

.storage-progress {
  margin-bottom: 15px;
}

.storage-warning {
  margin-bottom: 15px;
}

.storage-details {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 15px;
  padding-top: 15px;
  border-top: 1px solid #ebeef5;
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.detail-item .label {
  font-size: 12px;
  color: #909399;
}

.detail-item .value {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

:deep(.el-card__body) {
  padding: 20px;
}

:deep(.el-alert) {
  padding: 8px 12px;
}

:deep(.el-alert__title) {
  font-size: 13px;
}
</style>
