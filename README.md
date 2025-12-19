# 智能文档助手

基于 Spring Boot + Vue 3 的智能文档管理系统，提供用户认证、文档管理和文件夹管理功能。

## 功能特性

### 用户认证与授权
- 用户注册与邮箱验证
- 用户登录与 JWT 令牌认证
- 密码重置与修改
- 个人信息管理
- 头像上传
- 基于角色的访问控制（RBAC）

### 文档管理
- 文档上传（支持 PDF、Word、TXT 格式，最大 100MB）
- 批量上传文档
- 文档列表查询（支持分页、搜索、筛选、排序）
- 文档在线预览
- 文档下载（支持断点续传）
- 文档重命名和移动
- 文档删除和批量删除
- 存储空间管理和配额限制

### 文件夹管理
- 创建文件夹（支持多层级结构）
- 文件夹列表查询
- 文件夹重命名
- 删除空文件夹
- 查询文件夹中的文档

## 技术栈

**后端**: Spring Boot 3.2, Spring Security, PostgreSQL, Redis, JWT, MinIO (可选)  
**前端**: Vue 3, TypeScript, Vite, Element Plus

## 快速开始

### ⚠️ 重要：配置 Docker 镜像加速器

**中国大陆用户必须先配置镜像加速器，否则无法下载镜像！**

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
4. 等待 Docker 重启完成

### 开发环境

```bash
# 启动所有服务（包含热重载）
docker compose -f docker-compose.dev.yml up -d

# 访问应用
# 前端: http://localhost:5173
# 后端: http://localhost:8080
# API文档: http://localhost:8080/swagger-ui.html
```

### 生产环境

```bash
# 1. 配置环境变量
cp .env.example .env
# 编辑 .env 文件，设置数据库密码、JWT密钥、邮件配置

# 2. 启动服务
docker compose up -d

# 访问应用
# 前端: http://localhost
# 后端: http://localhost:8080
```

## 常用命令

```bash
# 查看日志
docker compose -f docker-compose.dev.yml logs -f

# 停止服务
docker compose -f docker-compose.dev.yml down

# 重新构建
docker compose -f docker-compose.dev.yml up -d --build

# 清除数据（重置数据库）
docker compose -f docker-compose.dev.yml down -v
```

## 开发说明

### 热重载

开发环境支持热重载，代码修改后自动生效：
- **后端**: Spring Boot DevTools 自动重新加载
- **前端**: Vite HMR 自动刷新浏览器

### 远程调试

后端支持远程调试，端口：5005

**IDEA 配置**:
1. Run → Edit Configurations → Remote JVM Debug
2. Host: localhost, Port: 5005
3. 设置断点并开始调试

### 数据库操作

```bash
# 连接数据库
docker compose -f docker-compose.dev.yml exec postgres psql -U postgres -d docassistant

# 查看表
\dt

# 查看用户
SELECT * FROM users;
```

### Redis 操作

```bash
# 连接 Redis
docker compose -f docker-compose.dev.yml exec redis redis-cli

# 查看所有 key
KEYS *
```

## 环境配置

### 开发环境

开发环境使用默认配置，无需额外配置。

### 生产环境

必须配置 `.env` 文件：

```env
# 数据库密码
POSTGRES_PASSWORD=your_strong_password

# JWT 密钥（使用 openssl rand -base64 64 生成）
JWT_SECRET=your_jwt_secret_min_256_bits

# 邮件配置
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_app_password
```

## 项目结构

```
.
├── backend/                 # Spring Boot 后端
│   ├── src/main/java/      # Java 源代码
│   ├── src/main/resources/ # 配置文件
│   ├── Dockerfile          # 生产环境镜像
│   └── Dockerfile.dev      # 开发环境镜像
├── frontend/               # Vue 3 前端
│   ├── src/                # 源代码
│   ├── Dockerfile          # 生产环境镜像
│   └── Dockerfile.dev      # 开发环境镜像
├── docker-compose.yml      # 生产环境配置
└── docker-compose.dev.yml  # 开发环境配置
```

## API 文档

启动后端后访问：http://localhost:8080/swagger-ui.html

## 测试

```bash
# 后端测试
docker compose -f docker-compose.dev.yml exec backend mvn test

# 前端测试
docker compose -f docker-compose.dev.yml exec frontend npm run test
```

## 故障排查

### Docker Desktop 未启动

**错误信息**: `open //./pipe/dockerDesktopLinuxEngine: The system cannot find the file specified`

**解决方案**:
1. 启动 Docker Desktop 应用程序
2. 等待 Docker Desktop 完全启动（图标变为绿色）
3. 重新运行命令

### 其他问题

查看详细的故障排查指南：[TROUBLESHOOTING.md](TROUBLESHOOTING.md)

包含：
- Docker 相关问题
- 数据库连接问题
- 后端/前端问题
- 邮件发送问题
- 性能问题
- 网络问题

## 文档

### 开发文档
- [README.md](README.md) - 快速开始和开发说明
- [API_DOCUMENTATION.md](API_DOCUMENTATION.md) - API 接口文档和请求示例
- [DEPLOY.md](DEPLOY.md) - 生产环境部署指南
- [TROUBLESHOOTING.md](TROUBLESHOOTING.md) - 详细故障排查

### 用户文档
- [USER_GUIDE.md](USER_GUIDE.md) - 用户使用指南

## 许可证

MIT License
