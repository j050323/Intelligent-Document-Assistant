<template>
  <div class="modal-overlay" @click.self="handleClose">
    <div class="modal-content">
      <div class="modal-header">
        <h2>修改密码</h2>
        <button class="close-btn" @click="handleClose">&times;</button>
      </div>
      
      <form @submit.prevent="handleSubmit">
        <div class="modal-body">
          <!-- 原密码 -->
          <div class="form-group">
            <label for="oldPassword">原密码</label>
            <div class="password-input">
              <input
                id="oldPassword"
                v-model="formData.oldPassword"
                :type="showOldPassword ? 'text' : 'password'"
                placeholder="请输入原密码"
                required
              />
              <button
                type="button"
                class="toggle-password"
                @click="showOldPassword = !showOldPassword"
              >
                <svg v-if="showOldPassword" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                    d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                    d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                </svg>
                <svg v-else fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                    d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21" />
                </svg>
              </button>
            </div>
          </div>
          
          <!-- 新密码 -->
          <div class="form-group">
            <label for="newPassword">新密码</label>
            <div class="password-input">
              <input
                id="newPassword"
                v-model="formData.newPassword"
                :type="showNewPassword ? 'text' : 'password'"
                placeholder="请输入新密码（至少8位，包含字母和数字）"
                required
                minlength="8"
                @input="validatePassword"
              />
              <button
                type="button"
                class="toggle-password"
                @click="showNewPassword = !showNewPassword"
              >
                <svg v-if="showNewPassword" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                    d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                    d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                </svg>
                <svg v-else fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                    d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21" />
                </svg>
              </button>
            </div>
            
            <!-- 密码强度指示器 -->
            <div v-if="formData.newPassword" class="password-strength">
              <div class="strength-bar">
                <div 
                  class="strength-fill" 
                  :class="passwordStrength.class"
                  :style="{ width: passwordStrength.width }"
                ></div>
              </div>
              <p class="strength-text" :class="passwordStrength.class">
                {{ passwordStrength.text }}
              </p>
            </div>
          </div>
          
          <!-- 确认新密码 -->
          <div class="form-group">
            <label for="confirmPassword">确认新密码</label>
            <div class="password-input">
              <input
                id="confirmPassword"
                v-model="formData.confirmPassword"
                :type="showConfirmPassword ? 'text' : 'password'"
                placeholder="请再次输入新密码"
                required
                @input="validateConfirmPassword"
              />
              <button
                type="button"
                class="toggle-password"
                @click="showConfirmPassword = !showConfirmPassword"
              >
                <svg v-if="showConfirmPassword" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                    d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                    d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                </svg>
                <svg v-else fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                    d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21" />
                </svg>
              </button>
            </div>
            <p v-if="confirmPasswordError" class="error-text">{{ confirmPasswordError }}</p>
          </div>
          
          <!-- 错误提示 -->
          <div v-if="errorMessage" class="error-message">
            {{ errorMessage }}
          </div>
        </div>
        
        <div class="modal-footer">
          <button 
            type="button"
            class="btn btn-secondary" 
            @click="handleClose"
            :disabled="loading"
          >
            取消
          </button>
          <button 
            type="submit"
            class="btn btn-primary" 
            :disabled="loading || !isFormValid"
          >
            {{ loading ? '提交中...' : '确认修改' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { userApi } from '@/api/user'

const emit = defineEmits<{
  close: []
  updated: []
}>()

const formData = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const showOldPassword = ref(false)
const showNewPassword = ref(false)
const showConfirmPassword = ref(false)
const loading = ref(false)
const errorMessage = ref('')
const confirmPasswordError = ref('')

// 密码强度计算
const passwordStrength = computed(() => {
  const password = formData.value.newPassword
  if (!password) return { width: '0%', text: '', class: '' }
  
  let strength = 0
  
  // 长度检查
  if (password.length >= 8) strength++
  if (password.length >= 12) strength++
  
  // 包含字母
  if (/[a-zA-Z]/.test(password)) strength++
  
  // 包含数字
  if (/\d/.test(password)) strength++
  
  // 包含特殊字符
  if (/[!@#$%^&*(),.?":{}|<>]/.test(password)) strength++
  
  if (strength <= 2) {
    return { width: '33%', text: '弱', class: 'weak' }
  } else if (strength <= 4) {
    return { width: '66%', text: '中', class: 'medium' }
  } else {
    return { width: '100%', text: '强', class: 'strong' }
  }
})

// 验证密码
const validatePassword = () => {
  const password = formData.value.newPassword
  
  if (password.length < 8) {
    return false
  }
  
  if (!/[a-zA-Z]/.test(password) || !/\d/.test(password)) {
    return false
  }
  
  return true
}

// 验证确认密码
const validateConfirmPassword = () => {
  if (formData.value.confirmPassword && 
      formData.value.newPassword !== formData.value.confirmPassword) {
    confirmPasswordError.value = '两次输入的密码不一致'
  } else {
    confirmPasswordError.value = ''
  }
}

// 表单是否有效
const isFormValid = computed(() => {
  return formData.value.oldPassword &&
         formData.value.newPassword &&
         formData.value.confirmPassword &&
         formData.value.newPassword === formData.value.confirmPassword &&
         validatePassword() &&
         !confirmPasswordError.value
})

// 提交表单
const handleSubmit = async () => {
  if (!isFormValid.value) return
  
  try {
    loading.value = true
    errorMessage.value = ''
    
    await userApi.updatePassword({
      oldPassword: formData.value.oldPassword,
      newPassword: formData.value.newPassword
    })
    
    emit('updated')
  } catch (error: any) {
    console.error('修改密码失败:', error)
    errorMessage.value = error.response?.data?.message || '修改失败，请检查原密码是否正确'
  } finally {
    loading.value = false
  }
}

// 关闭弹窗
const handleClose = () => {
  if (!loading.value) {
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

.form-group {
  margin-bottom: 1.5rem;
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  color: #374151;
  font-weight: 500;
}

.password-input {
  position: relative;
}

.password-input input {
  width: 100%;
  padding: 0.75rem;
  padding-right: 3rem;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 1rem;
  transition: border-color 0.3s;
}

.password-input input:focus {
  outline: none;
  border-color: #667eea;
}

.toggle-password {
  position: absolute;
  right: 0.75rem;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  color: #6b7280;
  cursor: pointer;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.toggle-password svg {
  width: 20px;
  height: 20px;
}

.toggle-password:hover {
  color: #374151;
}

.password-strength {
  margin-top: 0.5rem;
}

.strength-bar {
  width: 100%;
  height: 4px;
  background: #e5e7eb;
  border-radius: 2px;
  overflow: hidden;
}

.strength-fill {
  height: 100%;
  transition: width 0.3s, background 0.3s;
}

.strength-fill.weak {
  background: #ef4444;
}

.strength-fill.medium {
  background: #f59e0b;
}

.strength-fill.strong {
  background: #10b981;
}

.strength-text {
  margin-top: 0.25rem;
  font-size: 0.875rem;
  font-weight: 500;
}

.strength-text.weak {
  color: #ef4444;
}

.strength-text.medium {
  color: #f59e0b;
}

.strength-text.strong {
  color: #10b981;
}

.error-text {
  margin-top: 0.25rem;
  color: #ef4444;
  font-size: 0.875rem;
}

.error-message {
  padding: 0.75rem;
  background: #fee2e2;
  color: #991b1b;
  border-radius: 6px;
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
