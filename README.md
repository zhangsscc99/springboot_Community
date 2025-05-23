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


# 优化一览

4月更新
1修复了外部的点赞和收藏 
2做了分享功能 
3优化分享 加了文字描述 不只复制url 
4做了内部和外部点赞和收藏的立即显示 略去加载（乐观更新模式）
5点击点赞和收藏的时候 有一个放大的很丝滑的动画 弹跳缩放反馈（贝塞尔曲线） 实现平滑的过冲效果。这个有很好的体验感，可以一直延用。
6加入了帖子的删除功能 
7做了嵌套评论 
8进行了优化 给消息页面 对话具体内容页面 还有个人主页的"帖子" "喜欢" "收藏"栏目都做了缓存。
9修复了个人资料保存的信息丢失的问题 

5月更新
5.04
1修复了退出登录功能
2添加了昵称字段，添加了编辑昵称功能
3添加了好友搜索功能，搜索时能根据用户名或者昵称显示用户列表结果
4搜索好友结果页面能直接关注和取关。
5长按删除聊天框或者取消（有不显示和删除实际聊天记录两种删除）
6修复了个人页面的关注个数变化
7关注列表增加了能够实际关注和取关的功能
8新增关注人数和粉丝人数统计功能
9帖子时间的正确显示
10修复了前端点赞和收藏的bug
11帖子详情的关注也显示真实的功能

5.05
1做了一个agent 小李 他可以自己在论坛里发帖 每十秒发一次帖子
2做了5个agent







# 等待更新

1内容审核 
2短信验证   
3头像图片上传 
4智能推荐 
5ai提炼 
6评论点赞收藏通知 
7新增关注通知
8aspect内容


agent计划
1要能够评论别人
2要能够点赞别的帖子
3发帖时间变成每5分钟一篇
4多agent协作 能互相随机关注（估计可以用概率摇色子打标记实现） 点赞 评论（也是定时任务 但是要求摇色子设置概率）
5接入ai接口
6能够回复私信
7给每个agent不同的头像
8用户名要相对随机 不能搞得很像人机
9


大亮点：内容生成
人机
内容是最难的。
搞内容。

开发耗时：
论坛10h 理论上
部署2h（其实可以忽略）
维护更新 3h（各种维护 小细节的更改）





新增功能（agent） 10h 冷启动

# 多Agent系统

为了增加平台活跃度，系统集成了多个虚拟用户(Agent)，这些Agent能够自动执行以下操作：

1. **自动发帖** - 每5分钟随机选择一个活跃的Agent发布一篇帖子
2. **自动互动** - 每1分钟随机选择几个Agent进行点赞、评论、关注和收藏操作
3. **模拟真实用户** - 每个Agent有不同的人设、兴趣领域和活跃时间段

## 当前Agent列表

目前系统包含以下几个Agent角色：

1. **情感顾问小李** (21岁) - 情感受挫的大学生，分享情感困惑和心理历程
2. **职场大姐姐** (30岁) - 职场女性，关注事业与爱情平衡的话题
3. **青春有你** (17岁) - 高中生，分享青涩的校园暗恋和成长烦恼
4. **成熟稳重** (35岁) - 已婚男性，探讨婚姻与家庭生活
5. **都市小姐姐** (27岁) - 都市白领，分享都市单身生活和相亲经历

## Agent个性化设计

每个Agent都经过精心设计，包含以下属性：

- 基本信息（用户名、昵称、简介、年龄）
- 兴趣爱好标签
- 活跃时间段（模拟人类作息规律）
- 行为概率（发帖、点赞、评论、关注、收藏的概率）
- 专属内容库（帖子标题、内容模板、评论素材）

## 如何自定义Agent

可以通过修改 `backend/src/main/java/com/jinshuxqm/community/config/ScheduledTasks.java` 文件中的 `initAgentConfigs` 方法来：

1. 添加新的Agent角色
2. 调整现有Agent的行为模式
3. 更新Agent的内容库
4. 修改Agent的活跃时间和互动频率


