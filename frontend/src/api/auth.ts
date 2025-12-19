import apiClient from './axios'

export interface RegisterRequest {
  username: string
  email: string
  password: string
}

export interface LoginRequest {
  usernameOrEmail: string
  password: string
  rememberMe?: boolean
}

export interface LoginResponse {
  accessToken: string
  refreshToken: string
  user: {
    id: number
    username: string
    email: string
    role: string
    avatarUrl?: string
    isEmailVerified: boolean
  }
  expiresIn: number
}

export const authApi = {
  register: (data: RegisterRequest) => apiClient.post('/auth/register', data),
  
  verifyEmail: (email: string, code: string) =>
    apiClient.post('/auth/verify-email', { email, code }),
  
  login: (data: LoginRequest) => apiClient.post<LoginResponse>('/auth/login', data),
  
  logout: () => apiClient.post('/auth/logout'),
  
  refreshToken: () => apiClient.post('/auth/refresh-token'),
  
  forgotPassword: (email: string) => apiClient.post('/auth/forgot-password', { email }),
  
  resetPassword: (token: string, password: string) =>
    apiClient.post('/auth/reset-password', { token, password })
}
