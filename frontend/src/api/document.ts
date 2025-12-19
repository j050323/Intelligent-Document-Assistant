import apiClient from './axios'
import type { AxiosResponse } from 'axios'

export interface DocumentDTO {
  id: number
  filename: string
  originalFilename: string
  fileType: string
  fileSize: number
  mimeType?: string
  folderId?: number
  folderName?: string
  createdAt: string
  updatedAt: string
}

export interface DocumentQueryParams {
  page?: number
  size?: number
  keyword?: string
  fileType?: string
  folderId?: number
  sortBy?: string
  sortDirection?: string
}

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}

export interface StorageInfo {
  usedSpace: number
  totalQuota: number
  remainingSpace: number
  usagePercentage: number
  nearLimit: boolean
}

export interface UpdateDocumentRequest {
  filename?: string
  folderId?: number
}

export interface BatchOperationResult {
  successCount: number
  failureCount: number
  successIds: number[]
  errors: OperationError[]
}

export interface OperationError {
  id: number
  message: string
}

export interface PreviewResponse {
  type: 'url' | 'content'
  data: string
  mimeType?: string
}

export const documentApi = {
  /**
   * 上传单个文档
   */
  uploadDocument: (file: File, folderId?: number): Promise<AxiosResponse<DocumentDTO>> => {
    const formData = new FormData()
    formData.append('file', file)
    if (folderId !== undefined) {
      formData.append('folderId', folderId.toString())
    }
    return apiClient.post('/documents/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  },

  /**
   * 批量上传文档
   */
  batchUpload: (files: File[], folderId?: number): Promise<AxiosResponse<DocumentDTO[]>> => {
    const formData = new FormData()
    files.forEach(file => {
      formData.append('files', file)
    })
    if (folderId !== undefined) {
      formData.append('folderId', folderId.toString())
    }
    return apiClient.post('/documents/batch-upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  },

  /**
   * 获取文档列表
   */
  getDocuments: (params: DocumentQueryParams): Promise<AxiosResponse<PageResponse<DocumentDTO>>> => {
    return apiClient.get('/documents', { params })
  },

  /**
   * 获取文档详情
   */
  getDocument: (id: number): Promise<AxiosResponse<DocumentDTO>> => {
    return apiClient.get(`/documents/${id}`)
  },

  /**
   * 预览文档
   */
  previewDocument: (id: number): Promise<AxiosResponse<PreviewResponse>> => {
    return apiClient.get(`/documents/${id}/preview`)
  },

  /**
   * 下载文档
   */
  downloadDocument: (id: number): Promise<AxiosResponse<Blob>> => {
    return apiClient.get(`/documents/${id}/download`, {
      responseType: 'blob'
    })
  },

  /**
   * 更新文档信息
   */
  updateDocument: (id: number, data: UpdateDocumentRequest): Promise<AxiosResponse<DocumentDTO>> => {
    return apiClient.put(`/documents/${id}`, data)
  },

  /**
   * 删除文档
   */
  deleteDocument: (id: number): Promise<AxiosResponse<void>> => {
    return apiClient.delete(`/documents/${id}`)
  },

  /**
   * 批量删除文档
   */
  batchDelete: (ids: number[]): Promise<AxiosResponse<BatchOperationResult>> => {
    return apiClient.delete('/documents/batch', {
      data: { ids }
    })
  },

  /**
   * 获取存储空间信息
   */
  getStorageInfo: (): Promise<AxiosResponse<StorageInfo>> => {
    return apiClient.get('/documents/storage-info')
  }
}
