import apiClient from './axios'
import type { User } from '@/types'

export interface UpdateUserRequest {
  username?: string
}

export interface UpdateEmailRequest {
  newEmail: string
  verificationCode: string
}

export interface UpdatePasswordRequest {
  oldPassword: string
  newPassword: string
}

export const userApi = {
  // 获取当前用户信息
  getCurrentUser: () => apiClient.get<User>('/users/me'),
  
  // 更新个人信息
  updateUserInfo: (data: UpdateUserRequest) => 
    apiClient.put<User>('/users/me', data),
  
  // 上传头像
  uploadAvatar: (file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return apiClient.post<{ avatarUrl: string }>('/users/me/avatar', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  },
  
  // 修改邮箱
  updateEmail: (data: UpdateEmailRequest) => 
    apiClient.put('/users/me/email', data),
  
  // 修改密码
  updatePassword: (data: UpdatePasswordRequest) => 
    apiClient.put('/users/me/password', data)
}
