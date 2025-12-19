# 部署指南

## 生产环境部署

### 1. 安装 Docker

**Linux (Ubuntu/Debian)**:
```bash
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker $USER
```

**Windows/macOS**: 下载并安装 [Docker Desktop](https://www.docker.com/products/docker-desktop)

### 2. 配置 Docker 镜像加速器（中国大陆必须）

**Docker Desktop (Windows/macOS)**:
1. 打开 Docker Desktop → Settings → Docker Engine
2. 添加以下配置：
   ```json
   {
     "registry-mirrors": [
       "https://docker.m.daocloud.io",
       "https://docker.1panel.live",
       "https://hub.rat.dev"
     ]
   }
   ```
3. 点击 "Apply & Restart"

**Linux**:
```bash
sudo mkdir -p /etc/docker
sudo tee /etc/docker/daemon.json <<-'EOF'
{
  "registry-mirrors": [
    "https://docker.m.daocloud.io",
    "https://docker.1panel.live",
    "https://hub.rat.dev"
  ]
}
EOF
sudo systemctl daemon-reload
sudo systemctl restart docker
```

### 3. 配置环境变量

```bash
cp .env.example .env
```

编辑 `.env` 文件，**必须修改**以下配置：

```env
# 数据库密码（必须修改）
POSTGRES_PASSWORD=your_strong_password

# JWT 密钥（必须修改，至少 256 位）
# 生成命令: openssl rand -base64 64
JWT_SECRET=your_generated_secret_key

# 邮件配置（必须配置）
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_app_password

# 文件存储配置（可选，默认使用本地存储）
FILE_STORAGE_TYPE=local
FILE_STORAGE_PATH=/app/uploads

# MinIO 配置（如果使用 MinIO 对象存储）
# FILE_STORAGE_TYPE=minio
# MINIO_ENDPOINT=http://minio:9000
# MINIO_ACCESS_KEY=minioadmin
# MINIO_SECRET_KEY=minioadmin
# MINIO_BUCKET_NAME=documents

# 文件上传限制
MAX_FILE_SIZE=100MB
MAX_REQUEST_SIZE=100MB

# 存储配额（每个用户的默认存储配额，单位：字节）
DEFAULT_STORAGE_QUOTA=1073741824
```

### 4. 启动服务

```bash
docker compose up -d
```

### 5. 验证部署

```bash
# 查看服务状态
docker compose ps

# 查看日志
docker compose logs -f

# 测试后端
curl http://localhost:8080/actuator/health

# 访问前端
open http://localhost
```

## 邮件配置

### Gmail

1. 启用两步验证：https://myaccount.google.com/security
2. 生成应用专用密码：https://myaccount.google.com/apppasswords
3. 使用应用专用密码（16位，去掉空格）

```env
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=abcdefghijklmnop
```

### QQ 邮箱

1. 登录 QQ 邮箱 → 设置 → 账户
2. 开启 SMTP 服务并生成授权码

```env
MAIL_HOST=smtp.qq.com
MAIL_PORT=587
MAIL_USERNAME=your-qq@qq.com
MAIL_PASSWORD=your-authorization-code
```

### 163 邮箱

```env
MAIL_HOST=smtp.163.com
MAIL_PORT=465
MAIL_USERNAME=your-email@163.com
MAIL_PASSWORD=your-authorization-code
```

## 文件存储配置

系统支持两种文件存储方式：本地文件系统和 MinIO 对象存储。

### 本地文件系统（默认）

本地文件系统适合小规模部署和开发环境。

**配置**:
```env
FILE_STORAGE_TYPE=local
FILE_STORAGE_PATH=/app/uploads
```

**特点**:
- ✅ 配置简单，无需额外服务
- ✅ 适合单机部署
- ❌ 不支持分布式部署
- ❌ 备份需要手动处理

**存储路径结构**:
```
/app/uploads/
├── user_1/
│   ├── 2023/
│   │   ├── 12/
│   │   │   ├── document_20231216_123456.pdf
│   │   │   └── document_20231216_234567.docx
│   └── avatars/
│       └── avatar_20231216_123456.jpg
└── user_2/
    └── ...
```

**备份建议**:
```bash
# 定期备份上传目录
tar -czf uploads_backup_$(date +%Y%m%d).tar.gz /app/uploads/

# 或使用 rsync 同步到备份服务器
rsync -avz /app/uploads/ backup-server:/backup/uploads/
```

### MinIO 对象存储（推荐生产环境）

MinIO 是高性能的对象存储服务，适合生产环境和分布式部署。

**优势**:
- ✅ 支持分布式部署
- ✅ 高可用性和可扩展性
- ✅ 兼容 AWS S3 API
- ✅ 内置数据冗余和备份

#### 1. 部署 MinIO

**使用 Docker Compose**:

在 `docker-compose.yml` 中添加 MinIO 服务：

```yaml
services:
  minio:
    image: minio/minio:latest
    container_name: docassistant-minio
    ports:
      - "9000:9000"
      - "9001:9001"
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: minioadmin123
    volumes:
      - minio_data:/data
    command: server /data --console-address ":9001"
    networks:
      - docassistant-network
    restart: unless-stopped

volumes:
  minio_data:
    driver: local
```

**启动 MinIO**:
```bash
docker compose up -d minio
```

**访问 MinIO 控制台**:
- URL: http://localhost:9001
- 用户名: minioadmin
- 密码: minioadmin123

#### 2. 创建存储桶

**方法 1: 使用 MinIO 控制台**
1. 访问 http://localhost:9001
2. 登录后点击 "Buckets" → "Create Bucket"
3. 输入桶名称: `documents`
4. 点击 "Create Bucket"

**方法 2: 使用 MinIO 客户端**
```bash
# 安装 mc 客户端
docker run --rm -it --entrypoint=/bin/sh minio/mc

# 配置别名
mc alias set myminio http://minio:9000 minioadmin minioadmin123

# 创建存储桶
mc mb myminio/documents

# 设置公共读取策略（可选）
mc anonymous set download myminio/documents
```

#### 3. 配置应用

在 `.env` 文件中配置 MinIO：

```env
FILE_STORAGE_TYPE=minio
MINIO_ENDPOINT=http://minio:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin123
MINIO_BUCKET_NAME=documents
```

**生产环境建议**:
```env
FILE_STORAGE_TYPE=minio
MINIO_ENDPOINT=https://minio.your-domain.com
MINIO_ACCESS_KEY=your_access_key
MINIO_SECRET_KEY=your_secret_key
MINIO_BUCKET_NAME=documents
```

#### 4. 安全配置

**修改默认密码**:
```bash
# 在 docker-compose.yml 中修改
environment:
  MINIO_ROOT_USER: your_admin_user
  MINIO_ROOT_PASSWORD: your_strong_password
```

**创建专用访问密钥**:
1. 登录 MinIO 控制台
2. 点击 "Identity" → "Service Accounts"
3. 点击 "Create Service Account"
4. 保存生成的 Access Key 和 Secret Key
5. 在应用配置中使用这些密钥

**启用 HTTPS**:
```bash
# 生成证书
mkdir -p ~/.minio/certs
openssl req -new -x509 -days 365 -nodes \
  -out ~/.minio/certs/public.crt \
  -keyout ~/.minio/certs/private.key

# 挂载证书到容器
volumes:
  - ~/.minio/certs:/root/.minio/certs
```

### 文件上传限制配置

**配置文件大小限制**:
```env
# 单个文件最大大小
MAX_FILE_SIZE=100MB

# 单次请求最大大小（批量上传时）
MAX_REQUEST_SIZE=100MB
```

**在 application.yml 中配置**:
```yaml
spring:
  servlet:
    multipart:
      max-file-size: ${MAX_FILE_SIZE:100MB}
      max-request-size: ${MAX_REQUEST_SIZE:100MB}
```

### 存储配额配置

**设置默认存储配额**:
```env
# 每个用户的默认存储配额（单位：字节）
# 1GB = 1073741824 字节
DEFAULT_STORAGE_QUOTA=1073741824
```

**常用配额值**:
- 100MB: `104857600`
- 500MB: `524288000`
- 1GB: `1073741824`
- 5GB: `5368709120`
- 10GB: `10737418240`

**修改用户配额**:

管理员可以通过 API 修改用户的存储配额：

```bash
curl -X PUT http://localhost:8080/api/admin/users/{userId}/quota \
  -H "Authorization: Bearer <admin-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "storageQuota": 5368709120
  }'
```

### 支持的文件格式

系统支持以下文件格式：

- **PDF**: `.pdf`
- **Word**: `.doc`, `.docx`
- **文本**: `.txt`

**修改支持的格式**:

在 `application.yml` 中配置：

```yaml
app:
  document:
    allowed-extensions:
      - pdf
      - doc
      - docx
      - txt
    allowed-mime-types:
      - application/pdf
      - application/msword
      - application/vnd.openxmlformats-officedocument.wordprocessingml.document
      - text/plain
```

## SSL 配置（推荐）

### 使用 Let's Encrypt

```bash
# 安装 Certbot
sudo apt install certbot python3-certbot-nginx

# 获取证书
sudo certbot --nginx -d your-domain.com

# 自动续期
sudo certbot renew --dry-run
```

## 防火墙配置

```bash
# 允许 HTTP 和 HTTPS
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw enable
```

## 数据备份

### 自动备份脚本

创建 `backup.sh`:

```bash
#!/bin/bash
BACKUP_DIR="/backup/postgresql"
DATE=$(date +%Y%m%d_%H%M%S)

mkdir -p $BACKUP_DIR

# 备份数据库
docker compose exec -T postgres pg_dump -U postgres docassistant > $BACKUP_DIR/backup_$DATE.sql

# 保留最近 7 天的备份
find $BACKUP_DIR -name "backup_*.sql" -mtime +7 -delete

echo "Backup completed: backup_$DATE.sql"
```

### 设置定时任务

```bash
# 编辑 crontab
crontab -e

# 每天凌晨 2 点执行备份
0 2 * * * /path/to/backup.sh >> /var/log/backup.log 2>&1
```

### 恢复数据库

```bash
docker compose exec -T postgres psql -U postgres docassistant < /backup/postgresql/backup_20251217_020000.sql
```

## 更新应用

```bash
# 拉取最新代码
git pull origin main

# 重新构建并启动
docker compose up -d --build
```

## 监控

### 查看日志

```bash
# 所有服务
docker compose logs -f

# 特定服务
docker compose logs -f backend
docker compose logs -f frontend
docker compose logs -f postgres
```

### 健康检查

```bash
# 后端健康检查
curl http://localhost:8080/actuator/health

# 数据库健康检查
docker compose exec postgres pg_isready

# Redis 健康检查
docker compose exec redis redis-cli ping
```

### 资源监控

```bash
# 查看容器资源使用
docker stats
```

## 故障排查

### Docker Desktop 未启动

**错误信息**: `open //./pipe/dockerDesktopLinuxEngine: The system cannot find the file specified`

**解决方案**:
1. 启动 Docker Desktop 应用程序
2. 等待 Docker Desktop 完全启动（图标变为绿色）
3. 重新运行命令

### 服务无法启动

```bash
# 查看日志
docker compose logs backend

# 重新构建
docker compose build --no-cache
docker compose up -d
```

### 数据库连接失败

```bash
# 检查数据库状态
docker compose ps postgres

# 查看数据库日志
docker compose logs postgres

# 测试连接
docker compose exec postgres psql -U postgres -d docassistant -c "SELECT 1"
```

### 邮件发送失败

1. 检查邮件配置是否正确
2. 确认使用应用专用密码（不是登录密码）
3. 检查防火墙是否阻止 SMTP 端口
4. 查看后端日志中的详细错误

## 安全建议

1. ✅ 修改默认数据库密码
2. ✅ 使用强随机 JWT 密钥（至少 256 位）
3. ✅ 配置真实邮件服务器
4. ✅ 启用 HTTPS
5. ✅ 配置防火墙
6. ✅ 定期备份数据库
7. ✅ 定期更新依赖
