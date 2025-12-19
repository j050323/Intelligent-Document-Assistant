<template>
  <div class="reset-password-container">
    <el-card class="reset-password-card">
      <template #header>
        <div class="card-header">
          <h2>重置密码</h2>
        </div>
      </template>

      <div class="reset-password-content">
        <div v-if="!resetSuccess" class="form-section">
          <div class="info-section">
            <el-icon class="info-icon" :size="48"><Lock /></el-icon>
            <p class="info-text">请输入您的新密码</p>
          </div>

          <el-form
            ref="resetPasswordFormRef"
            :model="resetPasswordForm"
            :rules="resetPasswordRules"
            label-position="top"
            @submit.prevent="handleSubmit"
          >
            <el-form-item label="新密码" prop="password">
              <el-input
                v-model="resetPasswordForm.password"
                type="password"
                placeholder="请输入新密码（至少8个字符，包含字母和数字）"
                size="large"
                show-password
                clearable
                @input="checkPasswordStrength"
              >
                <template #prefix>
                  <el-icon><Lock /></el-icon>
                </template>
              </el-input>
              <div v-if="resetPasswordForm.password" class="password-strength">
                <div class="strength-bar">
                  <div
                    class="strength-fill"
                    :class="passwordStrength.class"
                    :style="{ width: passwordStrength.width }"
                  ></div>
                </div>
                <span class="strength-text" :class="passwordStrength.class">
                  {{ passwordStrength.text }}
                </span>
              </div>
            </el-form-item>

            <el-form-item label="确认新密码" prop="confirmPassword">
              <el-input
                v-model="resetPasswordForm.confirmPassword"
                type="password"
                placeholder="请再次输入新密码"
                size="large"
                show-password
                clearable
              >
                <template #prefix>
                  <el-icon><Lock /></el-icon>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item>
              <el-button
                type="primary"
                size="large"
                :loading="loading"
                native-type="submit"
                class="submit-button"
              >
                重置密码
              </el-button>
            </el-form-item>
          </el-form>
        </div>

        <div v-else class="success-section">
          <el-result
            icon="success"
            title="密码重置成功"
            sub-title="您的密码已成功重置，请使用新密码登录"
          >
            <template #extra>
              <el-button type="primary" @click="goToLogin">立即登录</el-button>
            </template>
          </el-result>
        </div>

        <div class="back-link">
          <el-link type="primary" @click="goToLogin">
            <el-icon><ArrowLeft /></el-icon>
            返回登录
          </el-link>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Lock, ArrowLeft } from '@element-plus/icons-vue'
import { authApi } from '@/api/auth'

const router = useRouter()
const route = useRoute()

const resetPasswordFormRef = ref<FormInstance>()
const loading = ref(false)
const resetSuccess = ref(false)

const resetToken = ref(route.query.token as string || '')

const resetPasswordForm = reactive({
  password: '',
  confirmPassword: ''
})

// 密码强度计算
const passwordStrength = ref({
  width: '0%',
  text: '',
  class: ''
})

const checkPasswordStrength = () => {
  const password = resetPasswordForm.password
  let strength = 0
  
  if (password.length >= 8) strength++
  if (password.length >= 12) strength++
  if (/[a-z]/.test(password)) strength++
  if (/[A-Z]/.test(password)) strength++
  if (/\d/.test(password)) strength++
  if (/[^a-zA-Z0-9]/.test(password)) strength++

  if (strength <= 2) {
    passwordStrength.value = { width: '33%', text: '弱', class: 'weak' }
  } else if (strength <= 4) {
    passwordStrength.value = { width: '66%', text: '中等', class: 'medium' }
  } else {
    passwordStrength.value = { width: '100%', text: '强', class: 'strong' }
  }
}

// 验证密码复杂度
const validatePassword = (rule: any, value: string, callback: any) => {
  if (!value) {
    callback(new Error('请输入新密码'))
  } else if (value.length < 8) {
    callback(new Error('密码长度至少为8个字符'))
  } else if (!/[a-zA-Z]/.test(value)) {
    callback(new Error('密码必须包含字母'))
  } else if (!/\d/.test(value)) {
    callback(new Error('密码必须包含数字'))
  } else {
    callback()
  }
}

// 验证确认密码
const validateConfirmPassword = (rule: any, value: string, callback: any) => {
  if (!value) {
    callback(new Error('请再次输入新密码'))
  } else if (value !== resetPasswordForm.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const resetPasswordRules: FormRules = {
  password: [
    { required: true, validator: validatePassword, trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const handleSubmit = async () => {
  if (!resetPasswordFormRef.value) return

  try {
    const valid = await resetPasswordFormRef.value.validate()
    if (!valid) return

    if (!resetToken.value) {
      ElMessage.error('重置令牌无效，请重新申请密码重置')
      router.push('/forgot-password')
      return
    }

    loading.value = true

    await authApi.resetPassword(resetToken.value, resetPasswordForm.password)

    resetSuccess.value = true
    ElMessage.success('密码重置成功')
  } catch (error: any) {
    const message = error.response?.data?.message || '重置失败，请检查链接是否有效或已过期'
    ElMessage.error(message)
    
    // 如果令牌无效或过期，提示用户重新申请
    if (error.response?.status === 400 || error.response?.status === 404) {
      setTimeout(() => {
        router.push('/forgot-password')
      }, 2000)
    }
  } finally {
    loading.value = false
  }
}

const goToLogin = () => {
  router.push('/login')
}

// 组件挂载时验证令牌
onMounted(() => {
  if (!resetToken.value) {
    ElMessage.warning('缺少重置令牌，请通过邮件链接访问')
    router.push('/forgot-password')
  }
})
</script>

<style scoped>
.reset-password-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.reset-password-card {
  width: 100%;
  max-width: 480px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

.card-header {
  text-align: center;
}

.card-header h2 {
  margin: 0;
  color: #303133;
  font-size: 24px;
  font-weight: 600;
}

.reset-password-content {
  padding: 10px 0;
}

.form-section {
  margin-bottom: 20px;
}

.info-section {
  text-align: center;
  margin-bottom: 30px;
}

.info-icon {
  color: #409eff;
  margin-bottom: 16px;
}

.info-text {
  font-size: 14px;
  color: #606266;
  margin: 0;
}

.password-strength {
  margin-top: 8px;
}

.strength-bar {
  height: 4px;
  background-color: #e4e7ed;
  border-radius: 2px;
  overflow: hidden;
  margin-bottom: 4px;
}

.strength-fill {
  height: 100%;
  transition: width 0.3s ease, background-color 0.3s ease;
}

.strength-fill.weak {
  background-color: #f56c6c;
}

.strength-fill.medium {
  background-color: #e6a23c;
}

.strength-fill.strong {
  background-color: #67c23a;
}

.strength-text {
  font-size: 12px;
  font-weight: 500;
}

.strength-text.weak {
  color: #f56c6c;
}

.strength-text.medium {
  color: #e6a23c;
}

.strength-text.strong {
  color: #67c23a;
}

.submit-button {
  width: 100%;
}

.success-section {
  margin-bottom: 20px;
}

.back-link {
  text-align: center;
  margin-top: 16px;
}

.back-link .el-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
</style>
