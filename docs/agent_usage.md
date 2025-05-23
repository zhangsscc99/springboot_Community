# 自动化用户Agent功能说明

这个功能创建了一个自动化Agent账号"lovelessboy"（情感顾问小李），它是一个为情感所伤的男大学生，会自动执行以下活动：

- **每10秒发布一篇关于情感话题的帖子**
- 定期点赞他人的帖子
- 定期评论他人的帖子
- 自动互动，快速填充论坛内容

## 高频发帖模式

本Agent设计为高频发帖模式，每10秒自动发布一篇情感相关帖子，可以快速为论坛生成内容：

1. **自动创建账号**：系统启动时自动检查并创建Agent账号
2. **极高发帖频率**：每10秒自动发布一篇情感相关帖子
3. **内容多样化**：拥有30+种不同的标题和20+种不同的内容模板
4. **完全自动化**：无需任何手动干预，持续运行

## 定时任务设置

Agent会按以下时间表自动工作：

- **发帖时间**：
  - 每10秒发布一篇帖子，高频填充论坛

- **互动时间**：
  - 每4小时一次（点赞、评论其他用户帖子）

## 帖子内容

Agent发布的帖子主题围绕情感问题，特别是失恋后的心理状态、异地恋的困境、如何面对分手等。这些内容旨在引发用户的共鸣和讨论，同时也为论坛提供持续的内容更新。

帖子内容库包含：
- 30多种不同的情感类标题
- 20多种不同的帖子内容
- 25种不同的评论模板

## 技术实现

- 使用Spring的`@PostConstruct`注解在应用启动时自动创建Agent账号
- 使用Spring的`@Scheduled(fixedRate = 10000)`实现每10秒发帖一次
- 使用`ScheduledTasks`类管理所有Agent行为
- 使用随机数生成器确保内容不重复，模拟真实用户

## 定制Agent行为

如需修改Agent的行为模式，可以编辑`ScheduledTasks.java`文件：

1. 修改发帖频率：调整`@Scheduled(fixedRate = 10000)`参数，单位为毫秒
2. 修改帖子内容：编辑`titles`和`contents`数组
3. 修改评论内容：编辑`comments`数组
4. 调整互动频率：修改随机数生成逻辑或cron表达式

## 禁用Agent功能

如果需要禁用Agent功能，可以在`application.properties`中添加：
```properties
spring.task.scheduling.enabled=false
```

或者在启动应用时使用命令行参数：
```
java -jar your-app.jar --spring.task.scheduling.enabled=false
```