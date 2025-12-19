# API 文档

## 概述

智能文档助手提供完整的RESTful API，支持用户认证、文档管理和文件夹管理功能。所有API都遵循REST规范，使用JSON格式进行数据交换。

**基础URL**: `http://localhost:8080`

**API文档**: `http://localhost:8080/swagger-ui.html`

## 认证

### JWT令牌认证

大部分API需要JWT令牌认证。请先调用登录接口获取令牌，然后在后续请求的Header中添加：

```
Authorization: Bearer <your-jwt-token>
```

### 获取令牌

**登录接口**: `POST /api/auth/login`

**请求示例**:
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123"
  }'
```

**响应示例**:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

## 文档管理 API

### 1. 上传单个文档

上传单个文档到系统。

**接口**: `POST /api/documents/upload`

**认证**: 需要JWT令牌

**请求参数**:
- `file` (multipart/form-data, 必需): 上传的文件
- `folderId` (query, 可选): 文件夹ID

**支持格式**: PDF (.pdf), Word (.doc, .docx), TXT (.txt)

**文件大小限制**: 最大100MB

**请求示例**:
```bash
curl -X POST http://localhost:8080/api/documents/upload \
  -H "Authorization: Bearer <your-jwt-token>" \
  -F "file=@/path/to/document.pdf" \
  -F "folderId=1"
```

**响应示例** (201 Created):
```json
{
  "id": 1,
  "filename": "document_20231216_123456.pdf",
  "originalFilename": "document.pdf",
  "fileType": "pdf",
  "fileSize": 1048576,
  "mimeType": "application/pdf",
  "folderId": 1,
  "folderName": "我的文档",
  "createdAt": "2023-12-16T12:34:56",
  "updatedAt": "2023-12-16T12:34:56"
}
```

**错误响应**:
- `400 Bad Request`: 文件格式不支持或文件大小超限
- `507 Insufficient Storage`: 存储空间不足

---

### 2. 批量上传文档

批量上传多个文档到系统。

**接口**: `POST /api/documents/batch-upload`

**认证**: 需要JWT令牌

**请求参数**:
- `files` (multipart/form-data, 必需): 上传的文件列表
- `folderId` (query, 可选): 文件夹ID

**请求示例**:
```bash
curl -X POST http://localhost:8080/api/documents/batch-upload \
  -H "Authorization: Bearer <your-jwt-token>" \
  -F "files=@/path/to/document1.pdf" \
  -F "files=@/path/to/document2.docx" \
  -F "folderId=1"
```

**响应示例** (200 OK):
```json
{
  "successCount": 2,
  "failureCount": 0,
  "successIds": [1, 2],
  "errors": []
}
```

**部分失败响应**:
```json
{
  "successCount": 1,
  "failureCount": 1,
  "successIds": [1],
  "errors": [
    {
      "filename": "document2.docx",
      "errorMessage": "文件格式不支持"
    }
  ]
}
```

---

### 3. 获取文档列表

获取当前用户的文档列表，支持分页、搜索、筛选和排序。

**接口**: `GET /api/documents`

**认证**: 需要JWT令牌

**查询参数**:
- `page` (int, 可选, 默认: 0): 页码（从0开始）
- `size` (int, 可选, 默认: 20): 每页数量
- `keyword` (string, 可选): 搜索关键词（文件名）
- `fileType` (string, 可选): 文件类型筛选 (pdf/docx/txt)
- `folderId` (long, 可选): 文件夹ID筛选
- `sortBy` (string, 可选, 默认: createdAt): 排序字段
- `sortDirection` (string, 可选, 默认: DESC): 排序方向 (ASC/DESC)

**请求示例**:
```bash
# 获取第一页文档
curl -X GET "http://localhost:8080/api/documents?page=0&size=20" \
  -H "Authorization: Bearer <your-jwt-token>"

# 搜索文件名包含"报告"的PDF文档
curl -X GET "http://localhost:8080/api/documents?keyword=报告&fileType=pdf" \
  -H "Authorization: Bearer <your-jwt-token>"

# 获取指定文件夹中的文档
curl -X GET "http://localhost:8080/api/documents?folderId=1" \
  -H "Authorization: Bearer <your-jwt-token>"
```

**响应示例** (200 OK):
```json
{
  "content": [
    {
      "id": 1,
      "filename": "document_20231216_123456.pdf",
      "originalFilename": "年度报告.pdf",
      "fileType": "pdf",
      "fileSize": 1048576,
      "mimeType": "application/pdf",
      "folderId": 1,
      "folderName": "我的文档",
      "createdAt": "2023-12-16T12:34:56",
      "updatedAt": "2023-12-16T12:34:56"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20
  },
  "totalElements": 1,
  "totalPages": 1,
  "last": true,
  "first": true
}
```

---

### 4. 获取文档详情

根据ID获取文档的详细信息。

**接口**: `GET /api/documents/{id}`

**认证**: 需要JWT令牌

**路径参数**:
- `id` (long, 必需): 文档ID

**请求示例**:
```bash
curl -X GET http://localhost:8080/api/documents/1 \
  -H "Authorization: Bearer <your-jwt-token>"
```

**响应示例** (200 OK):
```json
{
  "id": 1,
  "filename": "document_20231216_123456.pdf",
  "originalFilename": "年度报告.pdf",
  "fileType": "pdf",
  "fileSize": 1048576,
  "mimeType": "application/pdf",
  "folderId": 1,
  "folderName": "我的文档",
  "createdAt": "2023-12-16T12:34:56",
  "updatedAt": "2023-12-16T12:34:56"
}
```

**错误响应**:
- `404 Not Found`: 文档不存在
- `403 Forbidden`: 无权访问该文档

---

### 5. 预览文档

在线预览文档内容。

**接口**: `GET /api/documents/{id}/preview`

**认证**: 需要JWT令牌

**路径参数**:
- `id` (long, 必需): 文档ID

**请求示例**:
```bash
curl -X GET http://localhost:8080/api/documents/1/preview \
  -H "Authorization: Bearer <your-jwt-token>"
```

**响应示例 - PDF** (200 OK):
```json
{
  "type": "pdf",
  "url": "http://localhost:8080/uploads/user_1/2023/12/document_20231216_123456.pdf",
  "content": null
}
```

**响应示例 - TXT** (200 OK):
```json
{
  "type": "text",
  "url": null,
  "content": "这是文本文件的内容..."
}
```

**错误响应**:
- `404 Not Found`: 文档不存在
- `403 Forbidden`: 无权访问该文档

---

### 6. 下载文档

下载文档文件到本地。

**接口**: `GET /api/documents/{id}/download`

**认证**: 需要JWT令牌

**路径参数**:
- `id` (long, 必需): 文档ID

**请求示例**:
```bash
curl -X GET http://localhost:8080/api/documents/1/download \
  -H "Authorization: Bearer <your-jwt-token>" \
  -o downloaded_file.pdf
```

**响应**: 文件二进制流

**响应头**:
```
Content-Type: application/octet-stream
Content-Disposition: attachment; filename="年度报告.pdf"
Content-Length: 1048576
```

---

### 7. 更新文档信息

更新文档信息，支持重命名和移动到其他文件夹。

**接口**: `PUT /api/documents/{id}`

**认证**: 需要JWT令牌

**路径参数**:
- `id` (long, 必需): 文档ID

**请求体**:
```json
{
  "filename": "新文件名.pdf",
  "folderId": 2
}
```

**请求示例**:
```bash
# 重命名文档
curl -X PUT http://localhost:8080/api/documents/1 \
  -H "Authorization: Bearer <your-jwt-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "filename": "年度报告_2023.pdf"
  }'

# 移动文档到其他文件夹
curl -X PUT http://localhost:8080/api/documents/1 \
  -H "Authorization: Bearer <your-jwt-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "folderId": 2
  }'
```

**响应示例** (200 OK):
```json
{
  "id": 1,
  "filename": "年度报告_2023.pdf",
  "originalFilename": "年度报告_2023.pdf",
  "fileType": "pdf",
  "fileSize": 1048576,
  "mimeType": "application/pdf",
  "folderId": 2,
  "folderName": "工作文档",
  "createdAt": "2023-12-16T12:34:56",
  "updatedAt": "2023-12-16T13:00:00"
}
```

---

### 8. 删除文档

删除指定的文档，同时删除数据库记录和文件系统中的文件。

**接口**: `DELETE /api/documents/{id}`

**认证**: 需要JWT令牌

**路径参数**:
- `id` (long, 必需): 文档ID

**请求示例**:
```bash
curl -X DELETE http://localhost:8080/api/documents/1 \
  -H "Authorization: Bearer <your-jwt-token>"
```

**响应**: 204 No Content

---

### 9. 批量删除文档

批量删除多个文档。

**接口**: `DELETE /api/documents/batch`

**认证**: 需要JWT令牌

**请求体**:
```json
[1, 2, 3]
```

**请求示例**:
```bash
curl -X DELETE http://localhost:8080/api/documents/batch \
  -H "Authorization: Bearer <your-jwt-token>" \
  -H "Content-Type: application/json" \
  -d '[1, 2, 3]'
```

**响应示例** (200 OK):
```json
{
  "successCount": 2,
  "failureCount": 1,
  "successIds": [1, 2],
  "errors": [
    {
      "documentId": 3,
      "errorMessage": "文档不存在"
    }
  ]
}
```

---

### 10. 获取存储空间信息

获取当前用户的存储空间使用情况。

**接口**: `GET /api/documents/storage-info`

**认证**: 需要JWT令牌

**请求示例**:
```bash
curl -X GET http://localhost:8080/api/documents/storage-info \
  -H "Authorization: Bearer <your-jwt-token>"
```

**响应示例** (200 OK):
```json
{
  "usedSpace": 10485760,
  "totalQuota": 1073741824,
  "remainingSpace": 1063256064,
  "usagePercentage": 0.98,
  "nearLimit": false
}
```

**字段说明**:
- `usedSpace`: 已使用空间（字节）
- `totalQuota`: 总配额（字节）
- `remainingSpace`: 剩余空间（字节）
- `usagePercentage`: 使用百分比（0-100）
- `nearLimit`: 是否接近配额限制（90%以上）

---

## 文件夹管理 API

### 1. 创建文件夹

创建新文件夹，支持在根目录或指定父文件夹下创建。

**接口**: `POST /api/folders`

**认证**: 需要JWT令牌

**请求体**:
```json
{
  "name": "我的文档",
  "parentId": null
}
```

**请求示例**:
```bash
# 在根目录创建文件夹
curl -X POST http://localhost:8080/api/folders \
  -H "Authorization: Bearer <your-jwt-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "我的文档"
  }'

# 在指定父文件夹下创建子文件夹
curl -X POST http://localhost:8080/api/folders \
  -H "Authorization: Bearer <your-jwt-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "子文件夹",
    "parentId": 1
  }'
```

**响应示例** (201 Created):
```json
{
  "id": 1,
  "name": "我的文档",
  "parentId": null,
  "path": "/我的文档",
  "createdAt": "2023-12-16T12:34:56",
  "updatedAt": "2023-12-16T12:34:56"
}
```

**错误响应**:
- `400 Bad Request`: 文件夹名称重复
- `404 Not Found`: 父文件夹不存在

---

### 2. 获取文件夹列表

获取当前用户的文件夹列表。

**接口**: `GET /api/folders`

**认证**: 需要JWT令牌

**查询参数**:
- `parentId` (long, 可选): 父文件夹ID（不指定则获取根文件夹）

**请求示例**:
```bash
# 获取根文件夹
curl -X GET http://localhost:8080/api/folders \
  -H "Authorization: Bearer <your-jwt-token>"

# 获取指定文件夹的子文件夹
curl -X GET "http://localhost:8080/api/folders?parentId=1" \
  -H "Authorization: Bearer <your-jwt-token>"
```

**响应示例** (200 OK):
```json
[
  {
    "id": 1,
    "name": "我的文档",
    "parentId": null,
    "path": "/我的文档",
    "createdAt": "2023-12-16T12:34:56",
    "updatedAt": "2023-12-16T12:34:56"
  },
  {
    "id": 2,
    "name": "工作文档",
    "parentId": null,
    "path": "/工作文档",
    "createdAt": "2023-12-16T12:35:00",
    "updatedAt": "2023-12-16T12:35:00"
  }
]
```

---

### 3. 获取文件夹详情

根据ID获取文件夹的详细信息。

**接口**: `GET /api/folders/{id}`

**认证**: 需要JWT令牌

**路径参数**:
- `id` (long, 必需): 文件夹ID

**请求示例**:
```bash
curl -X GET http://localhost:8080/api/folders/1 \
  -H "Authorization: Bearer <your-jwt-token>"
```

**响应示例** (200 OK):
```json
{
  "id": 1,
  "name": "我的文档",
  "parentId": null,
  "path": "/我的文档",
  "createdAt": "2023-12-16T12:34:56",
  "updatedAt": "2023-12-16T12:34:56"
}
```

---

### 4. 更新文件夹

更新文件夹信息（重命名）。

**接口**: `PUT /api/folders/{id}`

**认证**: 需要JWT令牌

**路径参数**:
- `id` (long, 必需): 文件夹ID

**请求体**:
```json
{
  "name": "新文件夹名称"
}
```

**请求示例**:
```bash
curl -X PUT http://localhost:8080/api/folders/1 \
  -H "Authorization: Bearer <your-jwt-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "重要文档"
  }'
```

**响应示例** (200 OK):
```json
{
  "id": 1,
  "name": "重要文档",
  "parentId": null,
  "path": "/重要文档",
  "createdAt": "2023-12-16T12:34:56",
  "updatedAt": "2023-12-16T13:00:00"
}
```

---

### 5. 删除文件夹

删除指定的文件夹。只能删除空文件夹。

**接口**: `DELETE /api/folders/{id}`

**认证**: 需要JWT令牌

**路径参数**:
- `id` (long, 必需): 文件夹ID

**请求示例**:
```bash
curl -X DELETE http://localhost:8080/api/folders/1 \
  -H "Authorization: Bearer <your-jwt-token>"
```

**响应**: 204 No Content

**错误响应**:
- `400 Bad Request`: 文件夹不为空
- `404 Not Found`: 文件夹不存在

---

### 6. 获取文件夹中的文档

获取指定文件夹中的所有文档。

**接口**: `GET /api/folders/{id}/documents`

**认证**: 需要JWT令牌

**路径参数**:
- `id` (long, 必需): 文件夹ID

**请求示例**:
```bash
curl -X GET http://localhost:8080/api/folders/1/documents \
  -H "Authorization: Bearer <your-jwt-token>"
```

**响应示例** (200 OK):
```json
[
  {
    "id": 1,
    "filename": "document_20231216_123456.pdf",
    "originalFilename": "年度报告.pdf",
    "fileType": "pdf",
    "fileSize": 1048576,
    "mimeType": "application/pdf",
    "folderId": 1,
    "folderName": "我的文档",
    "createdAt": "2023-12-16T12:34:56",
    "updatedAt": "2023-12-16T12:34:56"
  }
]
```

---

## 错误响应格式

所有API错误响应遵循统一格式：

```json
{
  "timestamp": "2023-12-16T12:34:56Z",
  "status": 400,
  "error": "Bad Request",
  "message": "文件格式不支持",
  "path": "/api/documents/upload",
  "errorCode": "UNSUPPORTED_FILE_FORMAT"
}
```

### 常见错误代码

#### 文档操作相关
- `UNSUPPORTED_FILE_FORMAT`: 不支持的文件格式
- `FILE_SIZE_EXCEEDED`: 文件大小超限
- `STORAGE_QUOTA_EXCEEDED`: 存储空间不足
- `DOCUMENT_NOT_FOUND`: 文档不存在
- `FILE_UPLOAD_FAILED`: 文件上传失败
- `FILE_DOWNLOAD_FAILED`: 文件下载失败
- `FILE_PREVIEW_FAILED`: 文件预览失败

#### 文件夹操作相关
- `FOLDER_NOT_FOUND`: 文件夹不存在
- `FOLDER_NOT_EMPTY`: 文件夹不为空
- `DUPLICATE_FOLDER_NAME`: 文件夹名称重复

#### 权限相关
- `DOCUMENT_ACCESS_DENIED`: 无权访问文档
- `FOLDER_ACCESS_DENIED`: 无权访问文件夹

#### 系统错误
- `STORAGE_SERVICE_ERROR`: 存储服务错误
- `FILE_CONVERSION_ERROR`: 文件转换错误

---

## 使用示例

### 完整工作流程示例

```bash
# 1. 登录获取令牌
TOKEN=$(curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password123"}' \
  | jq -r '.accessToken')

# 2. 创建文件夹
FOLDER_ID=$(curl -X POST http://localhost:8080/api/folders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"我的文档"}' \
  | jq -r '.id')

# 3. 上传文档到文件夹
DOC_ID=$(curl -X POST http://localhost:8080/api/documents/upload \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@document.pdf" \
  -F "folderId=$FOLDER_ID" \
  | jq -r '.id')

# 4. 获取文档列表
curl -X GET "http://localhost:8080/api/documents?folderId=$FOLDER_ID" \
  -H "Authorization: Bearer $TOKEN"

# 5. 预览文档
curl -X GET "http://localhost:8080/api/documents/$DOC_ID/preview" \
  -H "Authorization: Bearer $TOKEN"

# 6. 下载文档
curl -X GET "http://localhost:8080/api/documents/$DOC_ID/download" \
  -H "Authorization: Bearer $TOKEN" \
  -o downloaded.pdf

# 7. 查看存储空间
curl -X GET http://localhost:8080/api/documents/storage-info \
  -H "Authorization: Bearer $TOKEN"
```

---

## 在线API文档

访问 Swagger UI 获取交互式API文档：

**开发环境**: http://localhost:8080/swagger-ui.html

**生产环境**: https://your-domain.com/swagger-ui.html

Swagger UI 提供：
- 完整的API接口列表
- 交互式API测试
- 请求/响应示例
- 数据模型定义
- 在线调试功能
