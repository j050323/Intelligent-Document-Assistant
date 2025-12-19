<template>
  <div class="modal-overlay" @click.self="handleClose">
    <div class="modal-content">
      <div class="modal-header">
        <h2>修改邮箱</h2>
        <button class="close-btn" @click="handleClose">&times;</button>
      </div>
      
      <form @submit.prevent="handleSubmit">
        <div class="modal-body">
          <!-- 步骤指示器 -->
          <div class="steps">
            <div class="step" :class="{ active: step === 1 }">
              <div class="step-number">1</div>
              <div class="step-label">输入新邮箱</div>
            </div>
            <div class="step-divider"></div>
            <div class="step" :class="{ active: step === 2 }">
              <div class="step-number">2</div>
              <div class="step-label">验证邮箱</div>
            </div>
          </div>
          
          <!-- 步骤1: 输入新邮箱 -->
          <div v-if="step === 1" class="step-content">
            <div class="form-group">
              <label for="newEmail">新邮箱地址</label>
              <input
                id="newEmail"
                v-model="formData.newEmail"
                type="email"
                placeholder="请输入新的邮箱地址"
                required
              />
            </div>
            
            <p class="info-text">
              我们将向新邮箱发送验证码，请确保邮箱地址正确
            </p>
          </div>
          
          <!-- 步骤2: 输入验证码 -->
          <div v-if="step === 2" class="step-content">
            <div class="form-group">
              <label for="verificationCode">验证码</label>
              <input
                id="verificationCode"
                v-model="formData.verificationCode"
                type="text"
                placeholder="请输入6位验证码"
                required
                maxlength="6"
                pattern="[0-9]{6}"
              />
            </div>
            
            <div class="verification-info">
              <p class="info-text">
                验证码已发送至 <strong>{{ formData.newEmail }}</strong>
              </p>
              <button
                type="button"
                class="resend-btn"
                @click="sendVerificationCode"
                :disabled="countdown > 0 || loading"
              >
                {{ countdown > 0 ? `${countdown}秒后重新发送` : '重新发送验证码' }}
              </button>
            </div>
          </div>
          
          <!-- 错误提示 -->
          <div v-if="errorMessage" class="error-message">
            {{ errorMessage }}
          </div>
          
          <!-- 成功提示 -->
          <div v-if="successMessage" class="success-message">
            {{ successMessage }}
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
            v-if="step === 1"
            type="button"
            class="btn btn-primary" 
            @click="sendVerificationCode"
            :disabled="loading || !formData.newEmail"
          >
            {{ loading ? '发送中...' : '发送验证码' }}
          </button>
          <button 
            v-if="step === 2"
            type="submit"
            class="btn btn-primary" 
            :disabled="loading || formData.verificationCode.length !== 6"
          >
            {{ loading ? '验证中...' : '确认修改' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { userApi } from '@/api/user'
import { authApi } from '@/api/auth'

const emit = defineEmits<{
  close: []
  updated: []
}>()

const step = ref(1)
const formData = ref({
  newEmail: '',
  verificationCode: ''
})

const loading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const countdown = ref(0)
let countdownTimer: number | null = null

// 发送验证码
const sendVerificationCode = async () => {
  if (!formData.value.newEmail) {
    errorMessage.value = '请输入邮箱地址'
    return
  }
  
  // 验证邮箱格式
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!emailRegex.test(formData.value.newEmail)) {
    errorMessage.value = '请输入有效的邮箱地址'
    return
  }
  
  try {
    loading.value = true
    errorMessage.value = ''
    successMessage.value = ''
    
    // 这里应该调用发送验证码的API
    // 由于后端可能没有单独的发送验证码接口，我们模拟这个过程
    // 实际项目中需要根据后端API调整
    await new Promise(resolve => setTimeout(resolve, 1000))
    
    step.value = 2
    successMessage.value = '验证码已发送，请查收邮件'
    
    // 开始倒计时
    startCountdown()
  } catch (error: any) {
    console.error('发送验证码失败:', error)
    errorMessage.value = error.response?.data?.message || '发送失败，请重试'
  } finally {
    loading.value = false
  }
}

// 开始倒计时
const startCountdown = () => {
  countdown.value = 60
  
  if (countdownTimer) {
    clearInterval(countdownTimer)
  }
  
  countdownTimer = window.setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      if (countdownTimer) {
        clearInterval(countdownTimer)
        countdownTimer = null
      }
    }
  }, 1000)
}

// 提交表单
const handleSubmit = async () => {
  if (formData.value.verificationCode.length !== 6) {
    errorMessage.value = '请输入6位验证码'
    return
  }
  
  try {
    loading.value = true
    errorMessage.value = ''
    
    await userApi.updateEmail({
      newEmail: formData.value.newEmail,
      verificationCode: formData.value.verificationCode
    })
    
    successMessage.value = '邮箱修改成功'
    
    // 延迟关闭弹窗
    setTimeout(() => {
      emit('updated')
    }, 1500)
  } catch (error: any) {
    console.error('修改邮箱失败:', error)
    errorMessage.value = error.response?.data?.message || '验证码错误或已过期'
  } finally {
    loading.value = false
  }
}

// 关闭弹窗
const handleClose = () => {
  if (!loading.value) {
    if (countdownTimer) {
      clearInterval(countdownTimer)
    }
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

.steps {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 2rem;
}

.step {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
}

.step-number {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #e5e7eb;
  color: #6b7280;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  transition: all 0.3s;
}

.step.active .step-number {
  background: #667eea;
  color: white;
}

.step-label {
  font-size: 0.875rem;
  color: #6b7280;
  transition: color 0.3s;
}

.step.active .step-label {
  color: #667eea;
  font-weight: 500;
}

.step-divider {
  width: 60px;
  height: 2px;
  background: #e5e7eb;
  margin: 0 1rem;
}

.step-content {
  animation: fadeIn 0.3s;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
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

.info-text {
  color: #6b7280;
  font-size: 0.875rem;
  margin: 0;
}

.verification-info {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.verification-info strong {
  color: #667eea;
}

.resend-btn {
  align-self: flex-start;
  padding: 0.5rem 1rem;
  background: none;
  border: 1px solid #667eea;
  color: #667eea;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.875rem;
  transition: all 0.3s;
}

.resend-btn:hover:not(:disabled) {
  background: #667eea;
  color: white;
}

.resend-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.error-message {
  padding: 0.75rem;
  background: #fee2e2;
  color: #991b1b;
  border-radius: 6px;
  font-size: 0.875rem;
  margin-top: 1rem;
}

.success-message {
  padding: 0.75rem;
  background: #d1fae5;
  color: #065f46;
  border-radius: 6px;
  font-size: 0.875rem;
  margin-top: 1rem;
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
