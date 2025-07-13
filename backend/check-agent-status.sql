-- 检查Agent系统状态

-- 1. 检查Agent用户是否存在
SELECT '=== Agent用户检查 ===' AS info;
SELECT u.id, u.username, u.nickname, u.email, u.created_at 
FROM users u 
WHERE u.username IN ('city_girl', 'career_sister', 'teen_heart', 'family_man', 'lovelessboy');

-- 2. 检查最新的评论数据
SELECT '=== 最新评论检查 ===' AS info;
SELECT c.id, c.content, c.user_id, u.username, u.nickname, c.post_id, c.created_at
FROM comments c
LEFT JOIN users u ON c.user_id = u.id
ORDER BY c.created_at DESC
LIMIT 10;

-- 3. 检查关注栏目的帖子
SELECT '=== 关注栏目帖子检查 ===' AS info;
SELECT p.id, p.title, p.tab, p.user_id, u.username, ps.comment_count
FROM posts p
LEFT JOIN users u ON p.user_id = u.id
LEFT JOIN post_stats ps ON p.id = ps.post_id
WHERE p.tab = '关注'
ORDER BY p.created_at DESC;

-- 4. 检查评论和用户的关联情况
SELECT '=== 评论用户关联检查 ===' AS info;
SELECT 
    COUNT(CASE WHEN c.user_id IS NULL THEN 1 END) AS comments_without_user,
    COUNT(CASE WHEN u.id IS NULL THEN 1 END) AS comments_with_missing_user,
    COUNT(*) AS total_comments
FROM comments c
LEFT JOIN users u ON c.user_id = u.id;

-- 5. 检查Agent创建的评论
SELECT '=== Agent评论检查 ===' AS info;
SELECT c.id, c.content, u.username, u.nickname, c.created_at, p.title
FROM comments c
JOIN users u ON c.user_id = u.id
JOIN posts p ON c.post_id = p.id
WHERE u.username IN ('city_girl', 'career_sister', 'teen_heart', 'family_man', 'lovelessboy')
ORDER BY c.created_at DESC
LIMIT 15; 