import apiClient from './axios'
import type { AxiosResponse } from 'axios'
import type { DocumentDTO } from './document'

export interface FolderDTO {
  id: number
  name: string
  parentId?: number
  path?: string
  createdAt: string
  updatedAt: string
}

export interface CreateFolderRequest {
  name: string
  parentId?: number
}

export interface UpdateFolderRequest {
  name: string
}

export const folderApi = {
  /**
   * 创建文件夹
   */
  createFolder: (data: CreateFolderRequest): Promise<AxiosResponse<FolderDTO>> => {
    return apiClient.post('/folders', data)
  },

  /**
   * 获取文件夹列表
   */
  getFolders: (parentId?: number): Promise<AxiosResponse<FolderDTO[]>> => {
    return apiClient.get('/folders', {
      params: parentId !== undefined ? { parentId } : {}
    })
  },

  /**
   * 获取文件夹详情
   */
  getFolder: (id: number): Promise<AxiosResponse<FolderDTO>> => {
    return apiClient.get(`/folders/${id}`)
  },

  /**
   * 更新文件夹
   */
  updateFolder: (id: number, data: UpdateFolderRequest): Promise<AxiosResponse<FolderDTO>> => {
    return apiClient.put(`/folders/${id}`, data)
  },

  /**
   * 删除文件夹
   */
  deleteFolder: (id: number): Promise<AxiosResponse<void>> => {
    return apiClient.delete(`/folders/${id}`)
  },

  /**
   * 获取文件夹中的文档
   */
  getDocumentsInFolder: (id: number): Promise<AxiosResponse<DocumentDTO[]>> => {
    return apiClient.get(`/folders/${id}/documents`)
  }
}
