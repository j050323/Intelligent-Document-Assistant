<template>
  <div class="forgot-password-container">
    <el-card class="forgot-password-card">
      <template #header>
        <div class="card-header">
          <h2>忘记密码</h2>
        </div>
      </template>

      <div class="forgot-password-content">
        <div v-if="!emailSent" class="form-section">
          <div class="info-section">
            <el-icon class="info-icon" :size="48"><Lock /></el-icon>
            <p class="info-text">
              请输入您的注册邮箱，我们将发送密码重置链接到您的邮箱
            </p>
          </div>

          <el-form
            ref="forgotPasswordFormRef"
            :model="forgotPasswordForm"
            :rules="forgotPasswordRules"
            label-position="top"
            @submit.prevent="handleSubmit"
          >
            <el-form-item label="邮箱地址" prop="email">
              <el-input
                v-model="forgotPasswordForm.email"
                type="email"
                placeholder="请输入注册邮箱"
                size="large"
                clearable
              >
                <template #prefix>
                  <el-icon><Message /></el-icon>
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
                发送重置链接
              </el-button>
            </el-form-item>
          </el-form>
        </div>

        <div v-else class="success-section">
          <el-result
            icon="success"
            title="邮件已发送"
            sub-title="密码重置链接已发送到您的邮箱，请查收邮件并按照指引重置密码"
          >
            <template #extra>
              <div class="success-actions">
                <p class="hint-text">
                  重置链接将在 <strong>1小时</strong> 后失效
                </p>
                <el-button type="primary" @click="goToLogin">返回登录</el-button>
                <el-button @click="resetForm">重新发送</el-button>
              </div>
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
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Message, Lock, ArrowLeft } from '@element-plus/icons-vue'
import { authApi } from '@/api/auth'

const router = useRouter()

const forgotPasswordFormRef = ref<FormInstance>()
const loading = ref(false)
const emailSent = ref(false)

const forgotPasswordForm = reactive({
  email: ''
})

const forgotPasswordRules: FormRules = {
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' }
  ]
}

const handleSubmit = async () => {
  if (!forgotPasswordFormRef.value) return

  try {
    const valid = await forgotPasswordFormRef.value.validate()
    if (!valid) return

    loading.value = true

    await authApi.forgotPassword(forgotPasswordForm.email)

    emailSent.value = true
    ElMessage.success('密码重置邮件已发送')
  } catch (error: any) {
    const message = error.response?.data?.message || '发送失败，请稍后重试'
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}

const resetForm = () => {
  emailSent.value = false
  forgotPasswordForm.email = ''
  forgotPasswordFormRef.value?.resetFields()
}

const goToLogin = () => {
  router.push('/login')
}
</script>

<style scoped>
.forgot-password-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.forgot-password-card {
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

.forgot-password-content {
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
  line-height: 1.6;
  margin: 0;
}

.submit-button {
  width: 100%;
}

.success-section {
  margin-bottom: 20px;
}

.success-actions {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.hint-text {
  font-size: 14px;
  color: #606266;
  margin: 0 0 8px 0;
}

.hint-text strong {
  color: #303133;
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
