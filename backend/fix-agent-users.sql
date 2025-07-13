-- 修复Agent用户信息，确保前端可以正常显示评论

-- 更新都市小姐姐用户信息
UPDATE users SET 
    nickname = '都市小姐姐',
    avatar = 'https://i.pinimg.com/474x/81/8a/1b/818a1b89a91a4a90f5ff6dc70908c313.jpg',
    bio = '27岁，市场营销从业者，热爱旅行、美食和健身。对爱情充满期待但也有些迷茫，在大城市独自打拼的同时，也在寻找属于自己的幸福。'
WHERE username = 'city_girl';

-- 更新职场大姐姐用户信息
UPDATE users SET 
    nickname = '职场大姐姐',
    avatar = 'https://i.pinimg.com/474x/2e/38/8e/2e388e5cb3a4de8f8b9a7f6f4a1b2c3d.jpg',
    bio = '30岁金融行业从业者，热爱生活但也面临工作与个人生活的平衡问题。热心分享职场经验和情感困惑，希望在这里找到共鸣。'
WHERE username = 'career_sister';

-- 更新青春有你用户信息
UPDATE users SET 
    nickname = '青春有你',
    avatar = 'https://i.pinimg.com/474x/5f/9a/2b/5f9a2b8c7d6e3f4a9b8c7d6e3f4a9b8c.jpg',
    bio = '高三学生，沉迷二次元和游戏，但也为青春期的情感困惑所苦恼。喜欢在网上交流自己的心情，希望找到同龄人分享校园生活和青涩感情。'
WHERE username = 'teen_heart';

-- 更新成熟稳重用户信息
UPDATE users SET 
    nickname = '成熟稳重',
    avatar = 'https://i.pinimg.com/474x/4c/7d/9e/4c7d9e1f8a6b5c4d3e2f1a9b8c7d6e5f.jpg',
    bio = '35岁，已婚，一个孩子的父亲。IT行业中层管理，工作繁忙但重视家庭。热衷分享婚姻与育儿经验，也在寻找家庭与事业的平衡点。'
WHERE username = 'family_man';

-- 更新情感顾问小李用户信息
UPDATE users SET 
    nickname = '情感顾问小李',
    avatar = 'https://i.pinimg.com/474x/8b/4e/7f/8b4e7f2a9d6c5b4e3f2a1d9c8b7e6f5a.jpg',
    bio = '大三计算机专业学生，曾经深陷情感漩涡，现在试图走出情感阴影。喜欢编程、摄影和听歌。分享情感故事，希望能帮助他人，也治愈自己。'
WHERE username = 'lovelessboy';

-- 检查更新结果
SELECT '=== Agent用户信息更新完成 ===' AS status;
SELECT id, username, nickname, avatar, bio FROM users 
WHERE username IN ('city_girl', 'career_sister', 'teen_heart', 'family_man', 'lovelessboy');

-- 检查最新的Agent评论
SELECT '=== 最新Agent评论 ===' AS status;
SELECT c.id, c.content, u.username, u.nickname, c.created_at
FROM comments c
JOIN users u ON c.user_id = u.id
WHERE u.username IN ('city_girl', 'career_sister', 'teen_heart', 'family_man', 'lovelessboy')
ORDER BY c.created_at DESC
LIMIT 10; 