// Common type definitions

export interface ApiResponse<T = any> {
  data: T
  message?: string
  success: boolean
}

export interface ErrorResponse {
  timestamp: string
  status: number
  error: string
  message: string
  path: string
  errorCode: string
}

/**
 * 用户信息接口
 */
export interface User {
  id: number
  username: string
  email: string
  role: 'REGULAR_USER' | 'ADMINISTRATOR'
  avatarUrl?: string
  isEmailVerified: boolean
  createdAt: string
}

/**
 * 登录请求接口
 */
export interface LoginRequest {
  usernameOrEmail: string
  password: string
  rememberMe?: boolean
}

/**
 * 登录响应接口
 */
export interface LoginResponse {
  accessToken: string
  refreshToken: string
  user: User
  expiresIn: number
}

/**
 * 注册请求接口
 */
export interface RegisterRequest {
  username: string
  email: string
  password: string
}

/**
 * 注册响应接口
 */
export interface RegisterResponse {
  userId: number
  message: string
}

/**
 * 更新用户信息请求接口
 */
export interface UpdateUserRequest {
  username?: string
  email?: string
}
