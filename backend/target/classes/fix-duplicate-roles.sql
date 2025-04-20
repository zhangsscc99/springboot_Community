-- 查找重复的角色
SELECT name, COUNT(*) as count FROM roles GROUP BY name HAVING COUNT(*) > 1;

-- 保留每种角色的第一条记录，删除剩余记录
-- 注意: 在删除前需要确保没有user_roles表的外键引用这些角色

-- 找出每种角色的第一个ID (保留这些)
CREATE TEMPORARY TABLE IF NOT EXISTS roles_to_keep AS
SELECT MIN(id) as id, name FROM roles GROUP BY name;

-- 删除不在roles_to_keep表中的角色记录
-- 首先删除对这些角色的引用
DELETE FROM user_roles
WHERE role_id IN (
    SELECT r.id FROM roles r
    WHERE r.id NOT IN (SELECT id FROM roles_to_keep)
);

-- 然后删除多余的角色
DELETE FROM roles
WHERE id NOT IN (SELECT id FROM roles_to_keep);

-- 确认清理后的角色
SELECT * FROM roles ORDER BY id;

-- 删除临时表
DROP TEMPORARY TABLE IF EXISTS roles_to_keep; 