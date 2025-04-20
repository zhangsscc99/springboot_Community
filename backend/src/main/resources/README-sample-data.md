# 示例帖子数据导入指南

本文档提供关于如何使用`sample-posts.sql`脚本向社区平台数据库导入示例帖子数据的详细说明。

## 前提条件

在执行导入脚本前，请确保：

1. 数据库连接配置正确并能够正常访问
2. 数据库中**至少存在一个用户账号**（脚本将使用第一个可用用户作为帖子作者）
3. 您有足够的权限执行SQL语句（INSERT, SELECT等）

## 使用方法

### 方法一：使用MySQL命令行客户端

1. 打开命令行终端
2. 连接到MySQL数据库：
   ```bash
   mysql -u 用户名 -p 数据库名
   ```
   例如：
   ```bash
   mysql -u root -p jinshuxqm_community
   ```
3. 输入密码后，执行以下命令导入脚本：
   ```sql
   source /完整路径/sample-posts.sql
   ```
   例如：
   ```sql
   source /c/Users/zhang/relationship_complex/relationship_community/backend/src/main/resources/sample-posts.sql
   ```

### 方法二：使用MySQL Workbench或其他图形工具

1. 打开MySQL Workbench或其他数据库管理工具
2. 连接到您的数据库
3. 打开新的SQL查询窗口
4. 将`sample-posts.sql`文件的内容复制粘贴到查询窗口，或使用"从文件执行SQL脚本"功能导入该文件
5. 执行脚本

## 脚本说明

`sample-posts.sql`脚本执行以下操作：

1. 检索数据库中的管理员用户（username='admin'）作为帖子作者
2. 如果找不到管理员用户，则使用数据库中的第一个可用用户
3. 检查每个帖子标题是否已存在，避免重复导入
4. 插入5个涵盖不同关系主题的示例帖子，包括：
   - 人际关系网络建立（关注板块）
   - 有效沟通技巧（推荐板块）
   - 家庭关系冲突处理（关注板块）
   - 数字时代的人际连接（热榜板块）
   - 依恋理论及其对关系的影响（情感知识板块）
5. 为每个帖子创建随机的统计数据（浏览量、点赞数等）
6. 为每个帖子添加相关标签

脚本设计为可重复执行，不会导致数据重复。如果检测到标题已存在的帖子，会自动跳过。

## 导入后的数据

脚本导入的帖子数据具有以下特点：

1. **多样化的主题**：从人际关系、沟通技巧到心理学理论，覆盖多种关系主题
2. **不同的分类标签**：帖子分布在不同板块，包括"关注"、"推荐"、"热榜"和"情感知识"
3. **随机的统计数据**：每个帖子都有随机生成的浏览量、点赞数、评论数和收藏数
4. **相关标签**：每个帖子都有3个相关标签，便于分类和搜索
5. **高质量内容**：所有帖子都包含格式化的Markdown文本，带有加粗标题和分段内容

## 注意事项

- 如果数据库中没有用户，脚本将无法执行并显示错误信息："数据库中没有用户，请先创建至少一个用户账号"
- 建议在执行该脚本前先运行`init-admin-data.sql`脚本创建管理员用户
- 脚本执行完毕后会显示成功插入的帖子数量和最新帖子列表

## 验证数据导入

执行以下SQL查询可以验证数据是否成功导入：

```sql
-- 查看导入的帖子
SELECT id, title, tab FROM posts ORDER BY id DESC LIMIT 10;

-- 查看帖子统计数据
SELECT p.title, ps.view_count, ps.like_count, ps.comment_count 
FROM posts p 
JOIN post_stats ps ON p.id = ps.post_id 
ORDER BY p.id DESC;

-- 查看帖子标签
SELECT p.title, pt.tag 
FROM posts p 
JOIN post_tags pt ON p.id = pt.post_id 
ORDER BY p.id DESC;
```

## 故障排除

如果在执行脚本时遇到问题：

1. **找不到用户错误**：确保数据库中至少有一个用户账号。可以先运行`init-admin-data.sql`创建管理员用户。
2. **表不存在错误**：确保数据库结构已正确创建。应用启动时应自动创建所需的表结构。
3. **语法错误**：如果使用较旧版本的MySQL，可能需要调整某些SQL语法。
4. **权限错误**：确保连接数据库的用户有足够的权限执行INSERT和SELECT语句。

如有进一步问题，请联系开发团队或在GitHub仓库提交Issue。 
