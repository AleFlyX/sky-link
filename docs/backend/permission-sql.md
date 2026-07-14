<!--role-->

INSERT IGNORE INTO role
    (role_name, role_code, description, status, create_time, update_time, is_deleted)
VALUES
    ('Super Administrator', 'ROLE_SUPER_ADMIN', 'Has full access to all system permissions', 1, NOW(), NOW(), 0),
    ('Administrator', 'ROLE_ADMIN', 'Manages users, departments, documents, and tasks', 1, NOW(), NOW(), 0),
    ('Project Leader', 'ROLE_PROJECT_LEADER', 'Manages project tasks and task progress', 1, NOW(), NOW(), 0),
    ('User', 'ROLE_USER', 'Default role for regular authenticated users', 1, NOW(), NOW(), 0);



<!--permission-->


INSERT IGNORE INTO permission
    (permission_name, permission_code, permission_type, parent_id, sort_no, create_time, update_time, is_deleted)
VALUES
    ('Get Current User Profile', 'user:me:get', 3, NULL, 10, NOW(), NOW(), 0),
    ('Update Current User Profile', 'user:me:update', 3, NULL, 11, NOW(), NOW(), 0),
    ('Update Current User Password', 'user:password:update', 3, NULL, 12, NOW(), NOW(), 0),
    ('List Users', 'user:list', 3, NULL, 13, NOW(), NOW(), 0),
    ('Get User Detail', 'user:get', 3, NULL, 14, NOW(), NOW(), 0),
    ('Update User Status', 'user:status:update', 3, NULL, 15, NOW(), NOW(), 0),
    ('Assign User Roles', 'user:role:add', 3, NULL, 16, NOW(), NOW(), 0),
    ('Remove User Role', 'user:role:delete', 3, NULL, 17, NOW(), NOW(), 0),

    ('List Departments', 'department:list', 3, NULL, 30, NOW(), NOW(), 0),
    ('Create Department', 'department:create', 3, NULL, 31, NOW(), NOW(), 0),
    ('Update Department', 'department:update', 3, NULL, 32, NOW(), NOW(), 0),
    ('Delete Department', 'department:delete', 3, NULL, 33, NOW(), NOW(), 0),
    ('List Department Members', 'department:members:list', 3, NULL, 34, NOW(), NOW(), 0),

    ('Create Document', 'document:create', 3, NULL, 50, NOW(), NOW(), 0),
    ('List Documents', 'document:list', 3, NULL, 51, NOW(), NOW(), 0),
    ('Get Document Detail', 'document:get', 3, NULL, 52, NOW(), NOW(), 0),
    ('Update Document', 'document:update', 3, NULL, 53, NOW(), NOW(), 0),
    ('Delete Document', 'document:delete', 3, NULL, 54, NOW(), NOW(), 0),
    ('Set User Document Permission', 'document:permission:user:set', 3, NULL, 55, NOW(), NOW(), 0),
    ('Set Group Document Permission', 'document:permission:group:set', 3, NULL, 56, NOW(), NOW(), 0),
    ('List Document Permissions', 'document:permission:list', 3, NULL, 57, NOW(), NOW(), 0),
    ('Remove User Document Permission', 'document:permission:user:delete', 3, NULL, 58, NOW(), NOW(), 0),
    ('Remove Group Document Permission', 'document:permission:group:delete', 3, NULL, 59, NOW(), NOW(), 0),

    ('Create Task', 'task:create', 3, NULL, 70, NOW(), NOW(), 0),
    ('List Tasks', 'task:list', 3, NULL, 71, NOW(), NOW(), 0),
    ('Get Task Detail', 'task:get', 3, NULL, 72, NOW(), NOW(), 0),
    ('Update Task', 'task:update', 3, NULL, 73, NOW(), NOW(), 0),
    ('Update Task Status', 'task:status:update', 3, NULL, 74, NOW(), NOW(), 0),
    ('Delete Task', 'task:delete', 3, NULL, 75, NOW(), NOW(), 0);




<!--role_permission-->

-- 1. ROLE_SUPER_ADMIN -> all permissions
INSERT IGNORE INTO role_permission
    (role_id, permission_id, create_time)
SELECT
    r.role_id,
    p.permission_id,
    NOW()
FROM role r
CROSS JOIN permission p
WHERE r.role_code = 'ROLE_SUPER_ADMIN';

-- 2. Other role-permission mappings
INSERT IGNORE INTO role_permission
    (role_id, permission_id, create_time)
SELECT
    r.role_id,
    p.permission_id,
    NOW()
FROM (
    SELECT 'ROLE_ADMIN' AS role_code, 'user:list' AS permission_code UNION ALL
    SELECT 'ROLE_ADMIN', 'user:get' UNION ALL
    SELECT 'ROLE_ADMIN', 'user:status:update' UNION ALL
    SELECT 'ROLE_ADMIN', 'department:list' UNION ALL
    SELECT 'ROLE_ADMIN', 'department:create' UNION ALL
    SELECT 'ROLE_ADMIN', 'department:update' UNION ALL
    SELECT 'ROLE_ADMIN', 'department:delete' UNION ALL
    SELECT 'ROLE_ADMIN', 'department:members:list' UNION ALL
    SELECT 'ROLE_ADMIN', 'document:list' UNION ALL
    SELECT 'ROLE_ADMIN', 'document:get' UNION ALL
    SELECT 'ROLE_ADMIN', 'document:delete' UNION ALL
    SELECT 'ROLE_ADMIN', 'task:create' UNION ALL
    SELECT 'ROLE_ADMIN', 'task:list' UNION ALL
    SELECT 'ROLE_ADMIN', 'task:get' UNION ALL
    SELECT 'ROLE_ADMIN', 'task:update' UNION ALL
    SELECT 'ROLE_ADMIN', 'task:status:update' UNION ALL
    SELECT 'ROLE_ADMIN', 'task:delete' UNION ALL
    SELECT 'ROLE_ADMIN', 'user:role:add' UNION ALL
    SELECT 'ROLE_ADMIN', 'user:role:delete' UNION ALL

    SELECT 'ROLE_PROJECT_LEADER', 'task:create' UNION ALL
    SELECT 'ROLE_PROJECT_LEADER', 'task:list' UNION ALL
    SELECT 'ROLE_PROJECT_LEADER', 'task:get' UNION ALL
    SELECT 'ROLE_PROJECT_LEADER', 'task:update' UNION ALL
    SELECT 'ROLE_PROJECT_LEADER', 'task:status:update' UNION ALL

    SELECT 'ROLE_USER', 'user:me:get' UNION ALL
    SELECT 'ROLE_USER', 'user:me:update' UNION ALL
    SELECT 'ROLE_USER', 'user:password:update' UNION ALL
    SELECT 'ROLE_USER', 'user:get' UNION ALL
    SELECT 'ROLE_USER', 'department:list' UNION ALL
    SELECT 'ROLE_USER', 'department:members:list' UNION ALL
    SELECT 'ROLE_USER', 'document:create' UNION ALL
    SELECT 'ROLE_USER', 'document:list' UNION ALL
    SELECT 'ROLE_USER', 'document:get' UNION ALL
    SELECT 'ROLE_USER', 'document:update' UNION ALL
    SELECT 'ROLE_USER', 'document:delete' UNION ALL
    SELECT 'ROLE_USER', 'document:permission:user:set' UNION ALL
    SELECT 'ROLE_USER', 'document:permission:group:set' UNION ALL
    SELECT 'ROLE_USER', 'document:permission:list' UNION ALL
    SELECT 'ROLE_USER', 'document:permission:user:delete' UNION ALL
    SELECT 'ROLE_USER', 'document:permission:group:delete' UNION ALL
    SELECT 'ROLE_USER', 'task:list' UNION ALL
    SELECT 'ROLE_USER', 'task:get' UNION ALL
    SELECT 'ROLE_USER', 'task:status:update'
) rp
JOIN role r
    ON r.role_code = rp.role_code
JOIN permission p
    ON p.permission_code = rp.permission_code;



<!--user_role-->

-- 为指定用户分配 ROLE_USER
INSERT IGNORE INTO user_role
    (user_id, role_id, create_time)
SELECT
    1001,
    r.role_id,
    NOW()
FROM role r
WHERE r.role_code = 'ROLE_USER';

-- 为指定用户分配 ROLE_ADMIN
INSERT IGNORE INTO user_role
    (user_id, role_id, create_time)
SELECT
    1002,
    r.role_id,
    NOW()
FROM role r
WHERE r.role_code = 'ROLE_ADMIN';

-- 为指定用户分配 ROLE_PROJECT_LEADER
INSERT IGNORE INTO user_role
    (user_id, role_id, create_time)
SELECT
    1003,
    r.role_id,
    NOW()
FROM role r
WHERE r.role_code = 'ROLE_PROJECT_LEADER';

-- 为指定用户分配 ROLE_SUPER_ADMIN
INSERT IGNORE INTO user_role
    (user_id, role_id, create_time)
SELECT
    1,
    r.role_id,
    NOW()
FROM role r
WHERE r.role_code = 'ROLE_SUPER_ADMIN';