-- 初始化角色数据
INSERT INTO roles (name) VALUES ('ROLE_USER');
INSERT INTO roles (name) VALUES ('ROLE_MODERATOR');
INSERT INTO roles (name) VALUES ('ROLE_ADMIN');

-- 创建管理员用户（密码：admin123）
INSERT INTO users (username, email, password, avatar, bio, created_at, updated_at) 
VALUES ('admin', 'admin@jinshuxqm.com', '$2a$10$ixWPJBRz9Bvh0Eus1/2G6.WvFqEONFtkwUdwDe7/LX9FiAQkgCdSG', 
'https://i.pinimg.com/474x/f1/9f/f7/f19ff7b351b4a4ca268b98e20fea7976.jpg', 
'锦书情感社区管理员', NOW(), NOW());

-- 为管理员添加ADMIN角色
INSERT INTO user_roles (user_id, role_id) 
VALUES (1, 3); 