-- 测试Agent自动评论功能
-- 创建一个"关注"栏目的测试帖子

-- 直接插入测试帖子（使用user_id=1，通常是admin用户）
INSERT INTO posts (title, content, user_id, tab, created_at, updated_at)
VALUES (
  '【测试】Agent自动评论功能测试帖子',
  '这是一个专门用于测试Agent自动评论功能的帖子。

如果系统正常工作，应该会有多个Agent（都市小姐姐、职场大姐姐、青春有你、成熟稳重、情感顾问小李）在5秒内自动在这个帖子下面发表评论。

让我们看看效果如何！',
  1,
  '关注',
  NOW(),
  NOW()
);

-- 获取新插入帖子的ID
SET @post_id = LAST_INSERT_ID();

-- 创建帖子统计数据
INSERT INTO post_stats (post_id, like_count, comment_count, favorite_count, view_count)
VALUES (@post_id, 0, 0, 0, 1);

-- 添加标签
INSERT INTO post_tags (post_id, tag) VALUES 
(@post_id, '测试'), 
(@post_id, 'Agent'), 
(@post_id, '自动评论');

-- 显示结果
SELECT CONCAT('测试帖子创建成功，ID: ', @post_id) AS '状态';
SELECT id, title, tab FROM posts WHERE id = @post_id; 