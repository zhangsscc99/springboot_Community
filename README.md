# 锦书情感社区

一个类似小红书和知乎的社区平台，专注于情感和心理健康话题的分享与讨论。

## 项目结构

- `frontend` - Vue.js前端项目
- `backend` - Spring Boot后端API

## 快速开始

### 后端开发环境设置（无需Docker）

1. **安装MySQL**:
   - 下载并安装 [MySQL 8.0 社区版](https://dev.mysql.com/downloads/mysql/)
   - 安装过程中，设置root用户密码为 "password"（或者修改application.properties中的配置）

2. **启动Spring Boot服务器**:
   ```bash
   cd backend
   ./mvnw spring-boot:run
   ```
   
   首次运行时Spring Boot会:
   - 自动创建数据库（如不存在）
   - 创建所有必要的表
   - 初始化默认角色和管理员用户

   服务器将在 http://localhost:8083/api 上运行

### 前端开发环境设置

```bash
cd frontend
npm install
npm run serve
```

前端开发服务器将在 http://localhost:3010 上运行。

## 默认账户

首次启动后，系统会创建默认管理员账户：

- 用户名: admin
- 密码: admin123

## API文档

API文档可以在 http://localhost:8083/api/swagger-ui.html 上找到（服务器运行后）。

## 功能

- 用户认证（登录/注册）
- 帖子浏览、创建、评论
- 用户资料
- 响应式设计（移动端/桌面端） 


花时间 20h 




npm install -g serve
serve -s dist -l 3010