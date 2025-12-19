# 故障排查指南

## Docker 相关问题

### 1. Docker Desktop 未启动

**错误信息**:
```
open //./pipe/dockerDesktopLinuxEngine: The system cannot find the file specified
```

**解决方案**:
1. 打开 Docker Desktop 应用程序
2. 等待 Docker Desktop 完全启动（系统托盘图标变为绿色）
3. 验证 Docker 已启动：
   ```bash
   docker --version
   docker compose version
   ```
4. 重新运行命令

### 2. 无法连接到 Docker Hub（网络问题）

**错误信息**:
```
failed to resolve reference "docker.io/library/redis:7-alpine"
dial tcp 128.121.146.228:443: connectex: A connection attempt failed
```

**原因**: 无法连接到 Docker Hub（常见于中国大陆）

**解决方案 1: 配置 Docker 镜像加速器（推荐）**

1. 打开 Docker Desktop
2. 点击右上角设置图标 → Settings
3. 选择 Docker Engine
4. 在 JSON 配置中添加镜像加速器：

```json
{
  "registry-mirrors": [
    "https://docker.m.daocloud.io",
    "https://docker.1panel.live",
    "https://hub.rat.dev"
  ]
}
```

5. 点击 "Apply & Restart"
6. 等待 Docker 重启完成
7. 重新运行命令：
   ```bash
   docker compose -f docker-compose.dev.yml up -d
   ```

**解决方案 2: 使用代理**

如果你有代理服务器：

1. Docker Desktop → Settings → Resources → Proxies
2. 启用 "Manual proxy configuration"
3. 配置 HTTP/HTTPS 代理
4. 点击 "Apply & Restart"

**解决方案 3: 手动下载镜像**

如果以上方法都不行，可以使用国内镜像源手动拉取：

```bash
# 使用阿里云镜像
docker pull registry.cn-hangzhou.aliyuncs.com/library/postgres:15-alpine
docker tag registry.cn-hangzhou.aliyuncs.com/library/postgres:15-alpine postgres:15-alpine

docker pull registry.cn-hangzhou.aliyuncs.com/library/redis:7-alpine
docker tag registry.cn-hangzhou.aliyuncs.com/library/redis:7-alpine redis:7-alpine
```

**验证镜像加速器是否生效**:
```bash
docker info | grep -A 5 "Registry Mirrors"
```

### 3. 端口被占用

**错误信息**:
```
Bind for 0.0.0.0:8080 failed: port is already allocated
```

**解决方案**:

**方法 1**: 停止占用端口的程序
```bash
# Windows - 查找占用端口的进程
netstat -ano | findstr :8080

# 结束进程（替换 PID）
taskkill /PID <进程ID> /F
```

**方法 2**: 修改端口映射

编辑 `docker-compose.dev.yml`:
```yaml
backend:
  ports:
    - "8081:8080"  # 改为 8081

frontend:
  ports:
    - "5174:5173"  # 改为 5174
```

### 4. 镜像构建失败

**错误信息**:
```
failed to solve: failed to read dockerfile
```

**解决方案**:
```bash
# 清理 Docker 缓存
docker system prune -a

# 重新构建
docker compose -f docker-compose.dev.yml build --no-cache
docker compose -f docker-compose.dev.yml up -d
```

### 5. 容器无法启动

**解决方案**:
```bash
# 查看容器状态
docker compose -f docker-compose.dev.yml ps

# 查看详细日志
docker compose -f docker-compose.dev.yml logs backend
docker compose -f docker-compose.dev.yml logs frontend

# 重启容器
docker compose -f docker-compose.dev.yml restart backend
```

## 数据库相关问题

### 1. 数据库连接失败

**错误信息**:
```
Connection refused: postgres:5432
```

**解决方案**:
```bash
# 检查数据库容器状态
docker compose -f docker-compose.dev.yml ps postgres

# 查看数据库日志
docker compose -f docker-compose.dev.yml logs postgres

# 测试数据库连接
docker compose -f docker-compose.dev.yml exec postgres pg_isready

# 手动连接数据库
docker compose -f docker-compose.dev.yml exec postgres psql -U postgres -d docassistant
```

### 2. 数据库初始化失败

**解决方案**:
```bash
# 停止服务并删除数据
docker compose -f docker-compose.dev.yml down -v

# 重新启动（Flyway 会自动执行迁移）
docker compose -f docker-compose.dev.yml up -d

# 查看后端日志确认迁移成功
docker compose -f docker-compose.dev.yml logs -f backend
```

### 3. 数据丢失

**解决方案**:

数据保存在 Docker volumes 中，除非使用 `-v` 参数删除，否则不会丢失。

```bash
# 查看 volumes
docker volume ls

# 备份数据
docker compose -f docker-compose.dev.yml exec -T postgres pg_dump -U postgres docassistant > backup.sql

# 恢复数据
docker compose -f docker-compose.dev.yml exec -T postgres psql -U postgres docassistant < backup.sql
```

## 后端相关问题

### 1. 后端无法启动

**解决方案**:
```bash
# 查看后端日志
docker compose -f docker-compose.dev.yml logs backend

# 常见问题：
# - 数据库未就绪：等待数据库启动完成
# - 端口被占用：修改端口映射
# - 配置错误：检查环境变量
```

### 2. 热重载不工作

**解决方案**:
```bash
# 确保 Spring Boot DevTools 已添加到 pom.xml
# 重启后端容器
docker compose -f docker-compose.dev.yml restart backend

# 查看日志确认 DevTools 已启用
docker compose -f docker-compose.dev.yml logs backend | grep "DevTools"
```

### 3. 远程调试无法连接

**解决方案**:
1. 确认调试端口已映射（5005）
2. 检查防火墙设置
3. 在 IDEA 中配置：
   - Host: localhost
   - Port: 5005
   - Transport: Socket
   - Debugger mode: Attach

## 前端相关问题

### 1. 前端无法访问

**解决方案**:
```bash
# 检查前端容器状态
docker compose -f docker-compose.dev.yml ps frontend

# 查看前端日志
docker compose -f docker-compose.dev.yml logs frontend

# 重启前端
docker compose -f docker-compose.dev.yml restart frontend
```

### 2. API 请求失败

**错误信息**:
```
Network Error / CORS Error
```

**解决方案**:
1. 确认后端已启动：http://localhost:8080/actuator/health
2. 检查前端环境变量：
   ```bash
   # 开发环境应该是
   VITE_API_BASE_URL=http://localhost:8080
   ```
3. 检查后端 CORS 配置

### 3. 热重载不工作

**解决方案**:
```bash
# 重启前端容器
docker compose -f docker-compose.dev.yml restart frontend

# 清除浏览器缓存
# 硬刷新：Ctrl + Shift + R (Windows) / Cmd + Shift + R (Mac)
```

## 邮件相关问题

### 1. 邮件发送失败

**错误信息**:
```
Failed to send email: Authentication failed
```

**解决方案**:

**Gmail**:
1. 启用两步验证
2. 生成应用专用密码：https://myaccount.google.com/apppasswords
3. 使用应用专用密码（16位，去掉空格）

**QQ 邮箱**:
1. 开启 SMTP 服务
2. 使用授权码（不是登录密码）

**检查配置**:
```bash
# 查看后端日志
docker compose -f docker-compose.dev.yml logs backend | grep "mail"
```

### 2. 验证码未收到

**解决方案**:
1. 检查垃圾邮件文件夹
2. 确认邮件配置正确
3. 查看后端日志确认邮件已发送
4. 开发环境会自动跳过邮箱验证

## 性能相关问题

### 1. 容器启动慢

**解决方案**:
```bash
# 清理未使用的镜像和容器
docker system prune

# 增加 Docker Desktop 的资源分配
# Settings → Resources → 增加 CPU 和内存
```

### 2. 热重载响应慢

**解决方案**:
1. 增加 Docker Desktop 的资源分配
2. 使用 SSD 硬盘
3. 关闭不必要的后台程序

## 网络相关问题

### 1. 容器间无法通信

**解决方案**:
```bash
# 检查网络
docker network ls

# 查看容器网络配置
docker compose -f docker-compose.dev.yml ps

# 重新创建网络
docker compose -f docker-compose.dev.yml down
docker compose -f docker-compose.dev.yml up -d
```

### 2. 无法访问外部网络

**解决方案**:
1. 检查 Docker Desktop 网络设置
2. 检查防火墙设置
3. 检查代理配置

## 常用诊断命令

```bash
# 查看所有容器状态
docker compose -f docker-compose.dev.yml ps

# 查看容器资源使用
docker stats

# 查看容器详细信息
docker compose -f docker-compose.dev.yml logs backend

# 进入容器内部
docker compose -f docker-compose.dev.yml exec backend bash

# 查看网络
docker network ls
docker network inspect firstproject_docassistant-dev-network

# 查看 volumes
docker volume ls
docker volume inspect firstproject_postgres_dev_data

# 清理所有资源（谨慎使用）
docker compose -f docker-compose.dev.yml down -v
docker system prune -a
```

## 获取帮助

如果以上方法都无法解决问题：

1. 查看完整日志：
   ```bash
   docker compose -f docker-compose.dev.yml logs > logs.txt
   ```

2. 检查 Docker Desktop 状态

3. 重启 Docker Desktop

4. 重启计算机

5. 查看项目 Issues 或提交新 Issue
