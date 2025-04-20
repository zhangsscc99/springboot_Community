-- 锦书情感社区 - 角色与管理员初始化脚本
-- 此脚本用于初始化基本角色和管理员用户

-- 启用安全更新模式，防止意外修改
SET SQL_SAFE_UPDATES = 1;

-- 开始事务，确保操作的原子性
START TRANSACTION;

-- 设置变量跟踪操作状态
SET @roles_created = 0;
SET @admin_created = 0;
SET @roles_assigned = 0;

-- ======== 角色初始化 ========
-- 检查并创建用户角色
INSERT INTO roles (name)
SELECT 'ROLE_USER' 
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ROLE_USER');

SET @roles_created = @roles_created + ROW_COUNT();

-- 检查并创建管理员角色
INSERT INTO roles (name)
SELECT 'ROLE_MODERATOR' 
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ROLE_MODERATOR');

SET @roles_created = @roles_created + ROW_COUNT();

-- 检查并创建超级管理员角色
INSERT INTO roles (name)
SELECT 'ROLE_ADMIN' 
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ROLE_ADMIN');

SET @roles_created = @roles_created + ROW_COUNT();

-- 获取各角色的ID
SELECT @user_role_id := id FROM roles WHERE name = 'ROLE_USER' LIMIT 1;
SELECT @mod_role_id := id FROM roles WHERE name = 'ROLE_MODERATOR' LIMIT 1;
SELECT @admin_role_id := id FROM roles WHERE name = 'ROLE_ADMIN' LIMIT 1;

-- ======== 管理员用户初始化 ========
-- 注意：密码需要使用BCrypt加密
-- 以下密码是"admin123"使用BCrypt加密后的结果
-- 如需自定义密码，请使用BCryptPasswordEncoder生成加密字符串后替换

-- 密码为"admin123"的BCrypt加密值
SET @encrypted_password = '$2a$10$UJR1s3qMEp52J4OcUJJ.E.p7I1KZhUUtJBHrW5iIXbHggXoGfDpAC';

-- 检查并创建管理员用户
INSERT INTO users (username, email, password, avatar, bio, created_at, updated_at)
SELECT 'admin', 'admin@jinshuxqm.com', @encrypted_password, 
       'https://i.pinimg.com/474x/f1/9f/f7/f19ff7b351b4a4ca268b98e20fea7976.jpg', 
       '锦书情感社区管理员', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin');

SET @admin_created = ROW_COUNT();

-- 获取管理员用户ID
SELECT @admin_id := id FROM users WHERE username = 'admin' LIMIT 1;

-- ======== 分配用户角色 ========
-- 检查管理员是否已有角色，没有则进行分配
-- 分配ADMIN角色
INSERT INTO user_roles (user_id, role_id)
SELECT @admin_id, @admin_role_id
WHERE @admin_id IS NOT NULL 
  AND @admin_role_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM user_roles 
    WHERE user_id = @admin_id AND role_id = @admin_role_id
  );

SET @roles_assigned = @roles_assigned + ROW_COUNT();

-- 分配MODERATOR角色
INSERT INTO user_roles (user_id, role_id)
SELECT @admin_id, @mod_role_id
WHERE @admin_id IS NOT NULL 
  AND @mod_role_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM user_roles 
    WHERE user_id = @admin_id AND role_id = @mod_role_id
  );

SET @roles_assigned = @roles_assigned + ROW_COUNT();

-- 分配USER角色
INSERT INTO user_roles (user_id, role_id)
SELECT @admin_id, @user_role_id
WHERE @admin_id IS NOT NULL 
  AND @user_role_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM user_roles 
    WHERE user_id = @admin_id AND role_id = @user_role_id
  );

SET @roles_assigned = @roles_assigned + ROW_COUNT();

-- ======== 状态报告 ========
-- 输出执行结果
SELECT 
  CONCAT('创建了 ', @roles_created, ' 个角色') AS '角色初始化',
  CASE 
    WHEN @admin_created > 0 THEN '成功创建管理员用户'
    ELSE '管理员用户已存在'
  END AS '管理员创建',
  CONCAT('分配了 ', @roles_assigned, ' 个角色到管理员') AS '角色分配';

-- 显示当前角色情况
SELECT id, name AS '角色名称' FROM roles;

-- 显示管理员信息及角色
SELECT 
  u.id, 
  u.username AS '用户名', 
  r.name AS '拥有角色'
FROM 
  users u
JOIN user_roles ur ON u.id = ur.user_id
JOIN roles r ON ur.role_id = r.id
WHERE 
  u.username = 'admin';

-- 如果一切正常，提交事务
COMMIT;

-- 操作完成后恢复默认设置
SET SQL_SAFE_UPDATES = DEFAULT;

-- 附加说明
/*
注意事项:
1. 此脚本已预设BCrypt加密的密码，对应明文为"admin123"
2. 如需使用自定义密码，请使用BCryptPasswordEncoder生成新的加密字符串
3. 脚本设计为可重复执行，不会产生重复数据
4. 脚本会为管理员分配所有角色(ROLE_USER, ROLE_MODERATOR, ROLE_ADMIN)

生成BCrypt密码的Java代码示例:
```java
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        System.out.println(encoder.encode("your_password_here"));
    }
}
```
*/ 