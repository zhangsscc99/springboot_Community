# 锦书情感社区后端

Spring Boot实现的锦书情感社区RESTful API后端服务。

## 技术栈

- Spring Boot 2.7.x
- Spring Security
- JWT身份验证
- Spring Data JPA
- MySQL数据库
- Maven构建系统

## 功能

- 用户注册和登录
- JWT基于令牌的身份验证
- 基于角色的权限控制
- 用户资料管理
- RESTful API

## 开发环境设置（简化版）

### 前提条件

- JDK 11或更高版本
- Maven 3.6或更高版本
- MySQL 8.0或更高版本

### 简易安装步骤

1. **安装MySQL 8.0**
   - 从[MySQL官网](https://dev.mysql.com/downloads/mysql/)下载安装
   - 安装时设置root密码为"password"（或修改application.properties中的配置）

2. **启动应用**
   ```bash
   ./mvnw spring-boot:run
   ```

应用将自动：
- 创建名为"jinshuxqm_community"的数据库（如不存在）
- 创建所有必要的表结构
- 插入初始数据（角色和管理员用户）

应用将在http://localhost:8083/api上运行。

### 默认账户

- 用户名: admin
- 密码: admin123

## API端点

### 认证

- **POST /api/auth/login** - 用户登录
- **POST /api/auth/register** - 用户注册

### 用户

- **GET /api/users/{id}** - 获取用户资料

### 测试

- **GET /api/test/all** - 公共访问
- **GET /api/test/user** - 用户访问
- **GET /api/test/mod** - 管理员访问
- **GET /api/test/admin** - 超级管理员访问

## 安全配置

后端使用JWT（JSON Web Token）实现无状态身份验证。所有受保护的API端点都需要在Authorization标头中包含有效的JWT令牌。

## 数据库

系统使用MySQL数据库存储数据。通过Spring Data JPA进行对象关系映射，可以轻松地替换为其他支持的数据库系统。 




(base) PS C:\Users\zhang\relationship_complex\relationship_community> curl.exe -X POST "http://communityapi.jinshuqingci.com/api/posts/4/comments" `
>>   -H "Content-Type: application/json" `
>>   -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ5dzQ1NTQiLCJpYXQiOjE3NDUxMzc3MTYsImV4cCI6MTc0NTIyNDExNn0.8eN5rTG0D2TMojXKUEekxRUpDqNvZWdpib1GIci2xs7tEOiFdHost5-zYhXZgtzDGaJm2zIsSgPvK3QDCRbqOw" `
>>   -d '{\"content\":\"这是一条测试评论\"}'
用户未登录，请先登录