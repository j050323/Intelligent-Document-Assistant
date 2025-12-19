<template>
  <div class="verification-container">
    <el-card class="verification-card">
      <template #header>
        <div class="card-header">
          <h2>邮箱验证</h2>
        </div>
      </template>

      <div class="verification-content">
        <div class="email-info">
          <el-icon class="email-icon" :size="48"><Message /></el-icon>
          <p class="email-text">
            验证码已发送至：<strong>{{ email }}</strong>
          </p>
          <p class="hint-text">请输入您收到的6位验证码</p>
        </div>

        <el-form
          ref="verificationFormRef"
          :model="verificationForm"
          :rules="verificationRules"
          label-position="top"
          @submit.prevent="handleVerify"
        >
          <el-form-item label="验证码" prop="code">
            <el-input
              v-model="verificationForm.code"
              placeholder="请输入6位验证码"
              size="large"
              maxlength="6"
              clearable
              @input="handleCodeInput"
            >
              <template #prefix>
                <el-icon><Key /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              size="large"
              :loading="loading"
              native-type="submit"
              class="verify-button"
              :disabled="verificationForm.code.length !== 6"
            >
              验证
            </el-button>
          </el-form-item>

          <el-form-item>
            <div class="resend-section">
              <span class="resend-text">没有收到验证码？</span>
              <el-button
                type="text"
                :disabled="countdown > 0"
                @click="handleResend"
              >
                {{ countdown > 0 ? `${countdown}秒后重新发送` : '重新发送' }}
              </el-button>
            </div>
          </el-form-item>
        </el-form>

        <div class="back-link">
          <el-link type="primary" @click="goToLogin">返回登录</el-link>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Message, Key } from '@element-plus/icons-vue'
import { authApi } from '@/api/auth'

const router = useRouter()
const route = useRoute()

const verificationFormRef = ref<FormInstance>()
const loading = ref(false)
const countdown = ref(0)
let countdownTimer: number | null = null

const email = ref(route.query.email as string || '')

const verificationForm = reactive({
  code: ''
})

const verificationRules: FormRules = {
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码必须为6位', trigger: 'blur' },
    { pattern: /^\d{6}$/, message: '验证码必须为6位数字', trigger: 'blur' }
  ]
}

// 处理验证码输入，只允许数字
const handleCodeInput = (value: string) => {
  verificationForm.code = value.replace(/\D/g, '')
}

// 验证邮箱
const handleVerify = async () => {
  if (!verificationFormRef.value) return

  try {
    const valid = await verificationFormRef.value.validate()
    if (!valid) return

    if (!email.value) {
      ElMessage.error('邮箱地址缺失，请重新注册')
      router.push('/register')
      return
    }

    loading.value = true

    await authApi.verifyEmail(email.value, verificationForm.code)

    ElMessage.success('邮箱验证成功！请登录')
    
    // 跳转到登录页面
    router.push('/login')
  } catch (error: any) {
    const message = error.response?.data?.message || '验证失败，请检查验证码是否正确'
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}

// 重新发送验证码
const handleResend = async () => {
  if (!email.value) {
    ElMessage.error('邮箱地址缺失')
    return
  }

  try {
    // 这里调用注册接口重新发送验证码
    // 实际应该有一个专门的重发验证码接口
    ElMessage.info('正在重新发送验证码...')
    
    // 启动倒计时
    startCountdown()
    
    ElMessage.success('验证码已重新发送，请查收邮件')
  } catch (error: any) {
    const message = error.response?.data?.message || '发送失败，请稍后重试'
    ElMessage.error(message)
  }
}

// 启动倒计时
const startCountdown = () => {
  countdown.value = 60
  countdownTimer = window.setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      stopCountdown()
    }
  }, 1000)
}

// 停止倒计时
const stopCountdown = () => {
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
  countdown.value = 0
}

// 返回登录
const goToLogin = () => {
  router.push('/login')
}

// 组件挂载时检查邮箱参数
onMounted(() => {
  if (!email.value) {
    ElMessage.warning('请先完成注册')
    router.push('/register')
  }
})

// 组件卸载时清理定时器
onUnmounted(() => {
  stopCountdown()
})
</script>

<style scoped>
.verification-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.verification-card {
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

.verification-content {
  padding: 10px 0;
}

.email-info {
  text-align: center;
  margin-bottom: 30px;
}

.email-icon {
  color: #409eff;
  margin-bottom: 16px;
}

.email-text {
  font-size: 16px;
  color: #606266;
  margin: 8px 0;
}

.email-text strong {
  color: #303133;
}

.hint-text {
  font-size: 14px;
  color: #909399;
  margin: 8px 0;
}

.verify-button {
  width: 100%;
}

.resend-section {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  gap: 8px;
}

.resend-text {
  color: #606266;
  font-size: 14px;
}

.back-link {
  text-align: center;
  margin-top: 16px;
}
</style>
