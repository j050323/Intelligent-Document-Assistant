import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export interface User {
  id: number
  username: string
  email: string
  role: string
  avatarUrl?: string
  isEmailVerified: boolean
  createdAt?: string
}

export const useUserStore = defineStore('user', () => {
  // State
  const user = ref<User | null>(null)
  const accessToken = ref<string | null>(localStorage.getItem('access_token'))
  const refreshToken = ref<string | null>(localStorage.getItem('refresh_token'))
  const isLoading = ref<boolean>(false)

  // Getters
  const isAuthenticated = computed(() => !!accessToken.value)
  const isAdmin = computed(() => user.value?.role === 'ADMINISTRATOR')
  const userDisplayName = computed(() => user.value?.username || user.value?.email || 'User')

  // Actions
  function setUser(userData: User) {
    user.value = userData
    // 缓存用户信息到 sessionStorage 以便页面刷新后恢复
    sessionStorage.setItem('user_info', JSON.stringify(userData))
  }

  function setTokens(access: string, refresh: string) {
    accessToken.value = access
    refreshToken.value = refresh
    localStorage.setItem('access_token', access)
    localStorage.setItem('refresh_token', refresh)
  }

  function clearAuth() {
    user.value = null
    accessToken.value = null
    refreshToken.value = null
    localStorage.removeItem('access_token')
    localStorage.removeItem('refresh_token')
    sessionStorage.removeItem('user_info')
  }

  function setLoading(loading: boolean) {
    isLoading.value = loading
  }

  // 从缓存恢复用户信息
  function restoreUserFromCache() {
    const cachedUser = sessionStorage.getItem('user_info')
    if (cachedUser && accessToken.value) {
      try {
        user.value = JSON.parse(cachedUser)
      } catch (error) {
        console.error('Failed to restore user from cache:', error)
        sessionStorage.removeItem('user_info')
      }
    }
  }

  // 更新用户信息（部分更新）
  function updateUser(updates: Partial<User>) {
    if (user.value) {
      user.value = { ...user.value, ...updates }
      sessionStorage.setItem('user_info', JSON.stringify(user.value))
    }
  }

  // 登录状态管理
  function login(access: string, refresh: string, userData: User) {
    setTokens(access, refresh)
    setUser(userData)
  }

  function logout() {
    clearAuth()
  }

  // 初始化时恢复用户信息
  restoreUserFromCache()

  return {
    // State
    user,
    accessToken,
    refreshToken,
    isLoading,
    // Getters
    isAuthenticated,
    isAdmin,
    userDisplayName,
    // Actions
    setUser,
    setTokens,
    clearAuth,
    setLoading,
    restoreUserFromCache,
    updateUser,
    login,
    logout
  }
})
