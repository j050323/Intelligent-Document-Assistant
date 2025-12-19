import axios from 'axios'
import type { AxiosInstance, AxiosError, InternalAxiosRequestConfig, AxiosResponse } from 'axios'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import router from '@/router'

const apiClient: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 用于跟踪是否正在刷新令牌
let isRefreshing = false
// 存储等待令牌刷新的请求队列
let failedQueue: Array<{
  resolve: (value?: any) => void
  reject: (reason?: any) => void
}> = []

// 处理队列中的请求
const processQueue = (error: AxiosError | null, token: string | null = null) => {
  failedQueue.forEach(promise => {
    if (error) {
      promise.reject(error)
    } else {
      promise.resolve(token)
    }
  })
  failedQueue = []
}

// Request interceptor - 添加JWT令牌
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const userStore = useUserStore()
    
    // 为所有请求添加JWT令牌（除了刷新令牌请求本身）
    if (userStore.accessToken && !config.url?.includes('/auth/refresh-token')) {
      config.headers.Authorization = `Bearer ${userStore.accessToken}`
    }
    
    return config
  },
  (error: AxiosError) => {
    return Promise.reject(error)
  }
)

// Response interceptor - 处理错误和令牌刷新
apiClient.interceptors.response.use(
  (response: AxiosResponse) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean }
    
    // 处理401未授权错误
    if (error.response?.status === 401 && originalRequest && !originalRequest._retry) {
      // 如果是刷新令牌请求失败，直接登出
      if (originalRequest.url?.includes('/auth/refresh-token')) {
        const userStore = useUserStore()
        userStore.clearAuth()
        
        // 只有当前不在登录页时才跳转
        if (router.currentRoute.value.name !== 'Login') {
          router.replace({ name: 'Login', query: { redirect: router.currentRoute.value.fullPath } })
          ElMessage.error('登录已过期，请重新登录')
        }
        
        return Promise.reject(error)
      }

      // 如果正在刷新令牌，将请求加入队列
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject })
        })
          .then(token => {
            if (originalRequest.headers) {
              originalRequest.headers.Authorization = `Bearer ${token}`
            }
            return apiClient(originalRequest)
          })
          .catch(err => {
            return Promise.reject(err)
          })
      }

      // 标记正在刷新令牌
      originalRequest._retry = true
      isRefreshing = true

      const userStore = useUserStore()
      
      // 尝试刷新令牌
      try {
        // 使用refresh token刷新access token
        // 注意：后端期望 refreshToken 作为请求参数，不是在 header 中
        const response = await axios.post(
          `/api/auth/refresh-token?refreshToken=${userStore.refreshToken}`
        )

        const { accessToken, refreshToken } = response.data
        
        // 更新令牌
        userStore.setTokens(accessToken, refreshToken)
        
        // 更新原始请求的令牌
        if (originalRequest.headers) {
          originalRequest.headers.Authorization = `Bearer ${accessToken}`
        }
        
        // 处理队列中的请求
        processQueue(null, accessToken)
        
        // 重试原始请求
        return apiClient(originalRequest)
      } catch (refreshError) {
        // 刷新令牌失败，清除认证信息并跳转到登录页
        processQueue(refreshError as AxiosError, null)
        userStore.clearAuth()
        
        // 使用 replace 而不是 push，避免在历史记录中留下无效页面
        // 只有当前不在登录页时才跳转
        if (router.currentRoute.value.name !== 'Login') {
          router.replace({ name: 'Login', query: { redirect: router.currentRoute.value.fullPath } })
          ElMessage.error('登录已过期，请重新登录')
        }
        
        return Promise.reject(refreshError)
      } finally {
        isRefreshing = false
      }
    }

    // 处理403权限不足错误
    if (error.response?.status === 403) {
      ElMessage.error('您没有权限执行此操作')
    }
    
    // 其他错误不在这里显示消息，让各个页面自己处理
    // 这样可以避免重复显示错误消息
    
    return Promise.reject(error)
  }
)

export default apiClient
