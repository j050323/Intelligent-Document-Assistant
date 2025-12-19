<template>
  <div class="profile-page">
    <div class="profile-container">
      <h1 class="page-title">个人中心</h1>
      
      <!-- 用户信息卡片 -->
      <div class="profile-card">
        <div class="profile-header">
          <div class="avatar-section">
            <img 
              :src="userInfo?.avatarUrl || defaultAvatar" 
              alt="用户头像" 
              class="avatar"
            />
            <button @click="showAvatarUpload = true" class="change-avatar-btn">
              更换头像
            </button>
          </div>
          
          <div class="user-info">
            <h2>{{ userInfo?.username }}</h2>
            <p class="email">{{ userInfo?.email }}</p>
            <span 
              :class="['status-badge', userInfo?.isEmailVerified ? 'verified' : 'unverified']"
            >
              {{ userInfo?.isEmailVerified ? '已验证' : '未验证' }}
            </span>
            <p class="role">角色: {{ roleText }}</p>
          </div>
        </div>
        
        <!-- 编辑信息表单 -->
        <div v-if="isEditing" class="edit-form">
          <h3>编辑个人信息</h3>
          <form @submit.prevent="handleUpdateInfo">
            <div class="form-group">
              <label for="username">用户名</label>
              <input
                id="username"
                v-model="editForm.username"
                type="text"
                placeholder="请输入用户名"
                required
                minlength="3"
                maxlength="50"
              />
            </div>
            
            <div class="form-actions">
              <button type="submit" class="btn btn-primary" :disabled="loading">
                {{ loading ? '保存中...' : '保存' }}
              </button>
              <button type="button" class="btn btn-secondary" @click="cancelEdit">
                取消
              </button>
            </div>
          </form>
        </div>
        
        <div v-else class="info-actions">
          <button @click="goToDocuments" class="btn btn-primary">
            📁 文档管理
          </button>
          <button @click="startEdit" class="btn btn-secondary">
            编辑信息
          </button>
          <button @click="showEmailChange = true" class="btn btn-secondary">
            修改邮箱
          </button>
          <button @click="showPasswordChange = true" class="btn btn-secondary">
            修改密码
          </button>
          <button @click="handleLogout" class="btn btn-danger">
            退出登录
          </button>
        </div>
      </div>
      
      <!-- 账户信息 -->
      <div class="account-info">
        <h3>账户信息</h3>
        <div class="info-item">
          <span class="label">用户ID:</span>
          <span class="value">{{ userInfo?.id }}</span>
        </div>
        <div class="info-item">
          <span class="label">注册时间:</span>
          <span class="value">{{ formatDate(userInfo?.createdAt) }}</span>
        </div>
      </div>
    </div>
    
    <!-- 头像上传弹窗 -->
    <AvatarUpload 
      v-if="showAvatarUpload"
      @close="showAvatarUpload = false"
      @uploaded="handleAvatarUploaded"
    />
    
    <!-- 邮箱修改弹窗 -->
    <EmailChangeForm
      v-if="showEmailChange"
      @close="showEmailChange = false"
      @updated="handleEmailUpdated"
    />
    
    <!-- 密码修改弹窗 -->
    <PasswordChangeForm
      v-if="showPasswordChange"
      @close="showPasswordChange = false"
      @updated="handlePasswordUpdated"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { userApi } from '@/api/user'
import type { User } from '@/types'
import AvatarUpload from '@/components/AvatarUpload.vue'
import EmailChangeForm from '@/components/EmailChangeForm.vue'
import PasswordChangeForm from '@/components/PasswordChangeForm.vue'

const router = useRouter()
const userStore = useUserStore()

const userInfo = ref<User | null>(null)
const isEditing = ref(false)
const loading = ref(false)
const showAvatarUpload = ref(false)
const showEmailChange = ref(false)
const showPasswordChange = ref(false)

const defaultAvatar = 'https://via.placeholder.com/150'

const editForm = ref({
  username: ''
})

const roleText = computed(() => {
  if (!userInfo.value) return ''
  return userInfo.value.role === 'ADMINISTRATOR' ? '管理员' : '普通用户'
})

// 加载用户信息
const loadUserInfo = async () => {
  try {
    loading.value = true
    const response = await userApi.getCurrentUser()
    userInfo.value = response.data
    userStore.setUser(response.data)
  } catch (error: any) {
    console.error('加载用户信息失败:', error)
    // 401错误已经在axios拦截器中处理，这里不需要重复处理
    // 其他错误也会在axios拦截器中显示消息
  } finally {
    loading.value = false
  }
}

// 开始编辑
const startEdit = () => {
  if (userInfo.value) {
    editForm.value.username = userInfo.value.username
  }
  isEditing.value = true
}

// 取消编辑
const cancelEdit = () => {
  isEditing.value = false
  editForm.value.username = ''
}

// 更新个人信息
const handleUpdateInfo = async () => {
  try {
    loading.value = true
    const response = await userApi.updateUserInfo({
      username: editForm.value.username
    })
    userInfo.value = response.data
    userStore.setUser(response.data)
    isEditing.value = false
    alert('信息更新成功')
  } catch (error: any) {
    console.error('更新信息失败:', error)
    alert(error.response?.data?.message || '更新失败')
  } finally {
    loading.value = false
  }
}

// 头像上传成功
const handleAvatarUploaded = (avatarUrl: string) => {
  if (userInfo.value) {
    userInfo.value.avatarUrl = avatarUrl
  }
  showAvatarUpload.value = false
}

// 邮箱更新成功
const handleEmailUpdated = () => {
  showEmailChange.value = false
  loadUserInfo()
}

// 密码更新成功
const handlePasswordUpdated = () => {
  showPasswordChange.value = false
  alert('密码修改成功，请重新登录')
  userStore.logout()
  router.push('/login')
}

// 跳转到文档管理
const goToDocuments = () => {
  router.push('/documents')
}

// 退出登录
const handleLogout = () => {
  if (confirm('确定要退出登录吗？')) {
    userStore.logout()
    router.push('/login')
  }
}

// 格式化日期
const formatDate = (dateString?: string) => {
  if (!dateString) return '-'
  const date = new Date(dateString)
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

onMounted(() => {
  loadUserInfo()
})
</script>

<style scoped>
.profile-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 2rem;
}

.profile-container {
  max-width: 800px;
  margin: 0 auto;
}

.page-title {
  color: white;
  font-size: 2rem;
  margin-bottom: 2rem;
  text-align: center;
}

.profile-card {
  background: white;
  border-radius: 12px;
  padding: 2rem;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  margin-bottom: 2rem;
}

.profile-header {
  display: flex;
  gap: 2rem;
  margin-bottom: 2rem;
  padding-bottom: 2rem;
  border-bottom: 1px solid #e5e7eb;
}

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
}

.avatar {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  object-fit: cover;
  border: 3px solid #667eea;
}

.change-avatar-btn {
  padding: 0.5rem 1rem;
  background: #667eea;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.875rem;
  transition: background 0.3s;
}

.change-avatar-btn:hover {
  background: #5568d3;
}

.user-info {
  flex: 1;
}

.user-info h2 {
  font-size: 1.5rem;
  margin-bottom: 0.5rem;
  color: #1f2937;
}

.email {
  color: #6b7280;
  margin-bottom: 0.5rem;
}

.status-badge {
  display: inline-block;
  padding: 0.25rem 0.75rem;
  border-radius: 12px;
  font-size: 0.875rem;
  margin-bottom: 0.5rem;
}

.status-badge.verified {
  background: #d1fae5;
  color: #065f46;
}

.status-badge.unverified {
  background: #fee2e2;
  color: #991b1b;
}

.role {
  color: #6b7280;
  font-size: 0.875rem;
}

.edit-form {
  margin-top: 1.5rem;
}

.edit-form h3 {
  font-size: 1.25rem;
  margin-bottom: 1rem;
  color: #1f2937;
}

.form-group {
  margin-bottom: 1.5rem;
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  color: #374151;
  font-weight: 500;
}

.form-group input {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 1rem;
  transition: border-color 0.3s;
}

.form-group input:focus {
  outline: none;
  border-color: #667eea;
}

.form-actions {
  display: flex;
  gap: 1rem;
}

.info-actions {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
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

.btn-secondary:hover {
  background: #d1d5db;
}

.btn-danger {
  background: #ef4444;
  color: white;
}

.btn-danger:hover {
  background: #dc2626;
}

.account-info {
  background: white;
  border-radius: 12px;
  padding: 2rem;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}

.account-info h3 {
  font-size: 1.25rem;
  margin-bottom: 1rem;
  color: #1f2937;
}

.info-item {
  display: flex;
  justify-content: space-between;
  padding: 0.75rem 0;
  border-bottom: 1px solid #e5e7eb;
}

.info-item:last-child {
  border-bottom: none;
}

.info-item .label {
  color: #6b7280;
  font-weight: 500;
}

.info-item .value {
  color: #1f2937;
}

@media (max-width: 640px) {
  .profile-header {
    flex-direction: column;
    align-items: center;
    text-align: center;
  }
  
  .info-actions {
    flex-direction: column;
  }
  
  .btn {
    width: 100%;
  }
}
</style>
