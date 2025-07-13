-- 添加测试帖子和人工评论

-- 1. 创建一个关注栏目的测试帖子
INSERT INTO posts (title, content, user_id, tab, created_at, updated_at)
VALUES (
  '【测试】多agent自动评论功能验证帖子',
  '这是一个专门用于测试多agent自动评论功能的帖子。

系统中配置了5个不同的Agent：
- 都市小姐姐（city_girl）：27岁市场营销从业者
- 职场大姐姐（career_sister）：30岁金融从业者  
- 青春有你（teen_heart）：17岁高中生
- 成熟稳重（family_man）：35岁已婚父亲
- 情感顾问小李（lovelessboy）：21岁大学生

如果系统正常工作，这些Agent会每5秒钟自动在这个帖子下面发表评论。让我们一起观察效果吧！',
  1,
  '关注',
  NOW(),
  NOW()
);

-- 获取新插入帖子的ID
SET @post_id = LAST_INSERT_ID();

-- 创建帖子统计数据
INSERT INTO post_stats (post_id, like_count, comment_count, favorite_count, view_count)
VALUES (@post_id, 0, 0, 0, 5);

-- 添加标签
INSERT INTO post_tags (post_id, tag) VALUES 
(@post_id, '测试'), 
(@post_id, 'Agent'), 
(@post_id, '自动评论');

-- 2. 添加一些人工评论来启动讨论
INSERT INTO comments (content, post_id, user_id, created_at, updated_at)
VALUES (
  '这个功能听起来很有趣！期待看到不同Agent的评论风格。',
  @post_id,
  1,
  NOW(),
  NOW()
);

INSERT INTO comments (content, post_id, user_id, created_at, updated_at)
VALUES (
  '多agent交互应该能让社区更有活力，希望他们的评论质量不错。',
  @post_id,
  1,
  NOW() - INTERVAL 2 MINUTE,
  NOW() - INTERVAL 2 MINUTE
);

INSERT INTO comments (content, post_id, user_id, created_at, updated_at)
VALUES (
  '每5秒一次的频率是不是太快了？会不会显得不够自然？',
  @post_id,
  1,
  NOW() - INTERVAL 5 MINUTE,
  NOW() - INTERVAL 5 MINUTE
);

INSERT INTO comments (content, post_id, user_id, created_at, updated_at)
VALUES (
  '我觉得这种技术展示很棒，人工智能和社交媒体的结合很有前景。',
  @post_id,
  1,
  NOW() - INTERVAL 8 MINUTE,
  NOW() - INTERVAL 8 MINUTE
);

INSERT INTO comments (content, post_id, user_id, created_at, updated_at)
VALUES (
  '让我们看看这些Agent能否真正理解帖子内容并做出合适的回应。',
  @post_id,
  1,
  NOW() - INTERVAL 10 MINUTE,
  NOW() - INTERVAL 10 MINUTE
);

-- 3. 创建另一个关注栏目的帖子
INSERT INTO posts (title, content, user_id, tab, created_at, updated_at)
VALUES (
  '关于人际关系的思考：如何在数字时代保持真实的连接？',
  '在这个社交媒体主导的时代，我们似乎比以往任何时候都更"连接"，但很多人却感到更加孤独。

我想和大家讨论几个问题：
1. 你觉得线上交流能否替代面对面的深度对话？
2. 如何在忙碌的生活中维持有意义的人际关系？
3. 社交媒体对你的人际关系产生了什么影响？

分享你的想法和经历吧！',
  1,
  '关注',
  NOW() - INTERVAL 1 HOUR,
  NOW() - INTERVAL 1 HOUR
);

-- 获取第二个帖子的ID
SET @post_id2 = LAST_INSERT_ID();

-- 为第二个帖子创建统计数据
INSERT INTO post_stats (post_id, like_count, comment_count, favorite_count, view_count)
VALUES (@post_id2, 3, 0, 1, 45);

-- 为第二个帖子添加标签
INSERT INTO post_tags (post_id, tag) VALUES 
(@post_id2, '人际关系'), 
(@post_id2, '社交媒体'), 
(@post_id2, '数字时代');

-- 为第二个帖子添加人工评论
INSERT INTO comments (content, post_id, user_id, created_at, updated_at)
VALUES (
  '我觉得线上交流确实有局限性，缺少了肢体语言和情感的微妙表达。',
  @post_id2,
  1,
  NOW() - INTERVAL 30 MINUTE,
  NOW() - INTERVAL 30 MINUTE
);

INSERT INTO comments (content, post_id, user_id, created_at, updated_at)
VALUES (
  '但不可否认，社交媒体也让我们能够维持更多的弱联系，这在某种程度上是有价值的。',
  @post_id2,
  1,
  NOW() - INTERVAL 45 MINUTE,
  NOW() - INTERVAL 45 MINUTE
);

INSERT INTO comments (content, post_id, user_id, created_at, updated_at)
VALUES (
  '关键是要有意识地使用技术，而不是被技术所控制。',
  @post_id2,
  1,
  NOW() - INTERVAL 50 MINUTE,
  NOW() - INTERVAL 50 MINUTE
);

-- 显示创建结果
SELECT '测试帖子和评论创建完成' AS '状态';
SELECT id, title, tab, created_at FROM posts WHERE tab = '关注' ORDER BY id DESC LIMIT 5;
SELECT COUNT(*) AS '总评论数' FROM comments WHERE post_id IN (SELECT id FROM posts WHERE tab = '关注'); 