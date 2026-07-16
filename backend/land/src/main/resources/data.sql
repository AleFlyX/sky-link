SET NAMES utf8mb4;

INSERT IGNORE INTO `role`
    (`role_name`, `role_code`, `description`, `status`, `create_time`, `update_time`, `is_deleted`)
VALUES
    ('Super Administrator', 'ROLE_SUPER_ADMIN', 'Has full access to all system permissions', 1, NOW(), NOW(), 0),
    ('Administrator', 'ROLE_ADMIN', 'Manages users, departments, documents, and tasks', 1, NOW(), NOW(), 0),
    ('Project Leader', 'ROLE_PROJECT_LEADER', 'Base user permissions plus project task management', 1, NOW(), NOW(), 0),
    ('User', 'ROLE_USER', 'Default role for regular authenticated users', 1, NOW(), NOW(), 0);

INSERT IGNORE INTO `permission`
    (`permission_name`, `permission_code`, `permission_type`, `sort_no`, `create_time`, `update_time`, `is_deleted`)
VALUES
    ('Get Current User Profile', 'user:me:get', 3, 10, NOW(), NOW(), 0),
    ('Update Current User Profile', 'user:me:update', 3, 11, NOW(), NOW(), 0),
    ('Update Current User Password', 'user:password:update', 3, 12, NOW(), NOW(), 0),
    ('List Users', 'user:list', 3, 13, NOW(), NOW(), 0),
    ('Get User Detail', 'user:get', 3, 14, NOW(), NOW(), 0),
    ('Update User Status', 'user:status:update', 3, 15, NOW(), NOW(), 0),
    ('Delete User', 'user:delete', 3, 16, NOW(), NOW(), 0),
    ('Assign User Roles', 'user:role:add', 3, 17, NOW(), NOW(), 0),
    ('Remove User Role', 'user:role:delete', 3, 18, NOW(), NOW(), 0),
    ('Create User', 'user:create', 3, 19, NOW(), NOW(), 0),
    ('List Friends', 'friend:list', 3, 20, NOW(), NOW(), 0),
    ('List Friend Requests', 'friend:request:list', 3, 21, NOW(), NOW(), 0),
    ('Send Friend Request', 'friend:request:create', 3, 22, NOW(), NOW(), 0),
    ('Delete Friend', 'friend:delete', 3, 23, NOW(), NOW(), 0),
    ('List Messages', 'message:list', 3, 24, NOW(), NOW(), 0),
    ('Send Message', 'message:send', 3, 25, NOW(), NOW(), 0),
    ('Recall Message', 'message:recall', 3, 26, NOW(), NOW(), 0),
    ('List Groups', 'group:list', 3, 27, NOW(), NOW(), 0),
    ('List Departments', 'department:list', 3, 30, NOW(), NOW(), 0),
    ('Create Department', 'department:create', 3, 31, NOW(), NOW(), 0),
    ('Update Department', 'department:update', 3, 32, NOW(), NOW(), 0),
    ('Delete Department', 'department:delete', 3, 33, NOW(), NOW(), 0),
    ('List Department Members', 'department:members:list', 3, 34, NOW(), NOW(), 0),
    ('Add Department Members', 'department:members:add', 3, 35, NOW(), NOW(), 0),
    ('Remove Department Members', 'department:members:remove', 3, 36, NOW(), NOW(), 0),
    ('List Roles', 'role:list', 3, 40, NOW(), NOW(), 0),
    ('Create Role', 'role:create', 3, 41, NOW(), NOW(), 0),
    ('Update Role', 'role:update', 3, 42, NOW(), NOW(), 0),
    ('Delete Role', 'role:delete', 3, 43, NOW(), NOW(), 0),
    ('Set Role Permissions', 'role:permission:set', 3, 44, NOW(), NOW(), 0),
    ('List Permissions', 'permission:list', 3, 50, NOW(), NOW(), 0),
    ('Create Permission', 'permission:create', 3, 51, NOW(), NOW(), 0),
    ('Update Permission', 'permission:update', 3, 52, NOW(), NOW(), 0),
    ('Delete Permission', 'permission:delete', 3, 53, NOW(), NOW(), 0),
    ('Create Document', 'document:create', 3, 60, NOW(), NOW(), 0),
    ('List Documents', 'document:list', 3, 61, NOW(), NOW(), 0),
    ('Get Document Detail', 'document:get', 3, 62, NOW(), NOW(), 0),
    ('Update Document', 'document:update', 3, 63, NOW(), NOW(), 0),
    ('Delete Document', 'document:delete', 3, 64, NOW(), NOW(), 0),
    ('Set User Document Permission', 'document:permission:user:set', 3, 65, NOW(), NOW(), 0),
    ('Set Group Document Permission', 'document:permission:group:set', 3, 66, NOW(), NOW(), 0),
    ('List Document Permissions', 'document:permission:list', 3, 67, NOW(), NOW(), 0),
    ('Remove User Document Permission', 'document:permission:user:delete', 3, 68, NOW(), NOW(), 0),
    ('Remove Group Document Permission', 'document:permission:group:delete', 3, 69, NOW(), NOW(), 0),
    ('Create Task', 'task:create', 3, 70, NOW(), NOW(), 0),
    ('List Tasks', 'task:list', 3, 71, NOW(), NOW(), 0),
    ('Get Task Detail', 'task:get', 3, 72, NOW(), NOW(), 0),
    ('Update Task', 'task:update', 3, 73, NOW(), NOW(), 0),
    ('Update Task Status', 'task:status:update', 3, 74, NOW(), NOW(), 0),
    ('Delete Task', 'task:delete', 3, 75, NOW(), NOW(), 0);

INSERT IGNORE INTO `role_permission`
    (`role_id`, `permission_id`, `create_time`)
SELECT
    r.`role_id`,
    p.`permission_id`,
    NOW()
FROM `role` r
CROSS JOIN `permission` p
WHERE r.`role_code` = 'ROLE_SUPER_ADMIN';

INSERT IGNORE INTO `role_permission`
    (`role_id`, `permission_id`, `create_time`)
SELECT
    r.`role_id`,
    p.`permission_id`,
    NOW()
FROM (
    SELECT 'ROLE_ADMIN' AS role_code, 'user:list' AS permission_code UNION ALL
    SELECT 'ROLE_ADMIN', 'user:get' UNION ALL
    SELECT 'ROLE_ADMIN', 'user:status:update' UNION ALL
    SELECT 'ROLE_ADMIN', 'user:delete' UNION ALL
    SELECT 'ROLE_ADMIN', 'user:role:add' UNION ALL
    SELECT 'ROLE_ADMIN', 'user:role:delete' UNION ALL
    SELECT 'ROLE_ADMIN', 'user:create' UNION ALL
    SELECT 'ROLE_ADMIN', 'friend:list' UNION ALL
    SELECT 'ROLE_ADMIN', 'friend:request:list' UNION ALL
    SELECT 'ROLE_ADMIN', 'friend:request:create' UNION ALL
    SELECT 'ROLE_ADMIN', 'friend:delete' UNION ALL
    SELECT 'ROLE_ADMIN', 'message:list' UNION ALL
    SELECT 'ROLE_ADMIN', 'message:send' UNION ALL
    SELECT 'ROLE_ADMIN', 'message:recall' UNION ALL
    SELECT 'ROLE_ADMIN', 'group:list' UNION ALL
    SELECT 'ROLE_ADMIN', 'department:list' UNION ALL
    SELECT 'ROLE_ADMIN', 'department:create' UNION ALL
    SELECT 'ROLE_ADMIN', 'department:update' UNION ALL
    SELECT 'ROLE_ADMIN', 'department:delete' UNION ALL
    SELECT 'ROLE_ADMIN', 'department:members:list' UNION ALL
    SELECT 'ROLE_ADMIN', 'department:members:add' UNION ALL
    SELECT 'ROLE_ADMIN', 'department:members:remove' UNION ALL
    SELECT 'ROLE_ADMIN', 'role:list' UNION ALL
    SELECT 'ROLE_ADMIN', 'role:create' UNION ALL
    SELECT 'ROLE_ADMIN', 'role:update' UNION ALL
    SELECT 'ROLE_ADMIN', 'role:delete' UNION ALL
    SELECT 'ROLE_ADMIN', 'role:permission:set' UNION ALL
    SELECT 'ROLE_ADMIN', 'permission:list' UNION ALL
    SELECT 'ROLE_ADMIN', 'permission:create' UNION ALL
    SELECT 'ROLE_ADMIN', 'permission:update' UNION ALL
    SELECT 'ROLE_ADMIN', 'permission:delete' UNION ALL
    SELECT 'ROLE_ADMIN', 'document:create' UNION ALL
    SELECT 'ROLE_ADMIN', 'document:list' UNION ALL
    SELECT 'ROLE_ADMIN', 'document:get' UNION ALL
    SELECT 'ROLE_ADMIN', 'document:update' UNION ALL
    SELECT 'ROLE_ADMIN', 'document:delete' UNION ALL
    SELECT 'ROLE_ADMIN', 'document:permission:user:set' UNION ALL
    SELECT 'ROLE_ADMIN', 'document:permission:group:set' UNION ALL
    SELECT 'ROLE_ADMIN', 'document:permission:list' UNION ALL
    SELECT 'ROLE_ADMIN', 'document:permission:user:delete' UNION ALL
    SELECT 'ROLE_ADMIN', 'document:permission:group:delete' UNION ALL
    SELECT 'ROLE_ADMIN', 'task:create' UNION ALL
    SELECT 'ROLE_ADMIN', 'task:list' UNION ALL
    SELECT 'ROLE_ADMIN', 'task:get' UNION ALL
    SELECT 'ROLE_ADMIN', 'task:update' UNION ALL
    SELECT 'ROLE_ADMIN', 'task:status:update' UNION ALL
    SELECT 'ROLE_ADMIN', 'task:delete' UNION ALL
    SELECT 'ROLE_PROJECT_LEADER', 'user:me:get' UNION ALL
    SELECT 'ROLE_PROJECT_LEADER', 'user:me:update' UNION ALL
    SELECT 'ROLE_PROJECT_LEADER', 'user:password:update' UNION ALL
    SELECT 'ROLE_PROJECT_LEADER', 'user:get' UNION ALL
    SELECT 'ROLE_PROJECT_LEADER', 'friend:list' UNION ALL
    SELECT 'ROLE_PROJECT_LEADER', 'friend:request:list' UNION ALL
    SELECT 'ROLE_PROJECT_LEADER', 'friend:request:create' UNION ALL
    SELECT 'ROLE_PROJECT_LEADER', 'friend:delete' UNION ALL
    SELECT 'ROLE_PROJECT_LEADER', 'message:list' UNION ALL
    SELECT 'ROLE_PROJECT_LEADER', 'message:send' UNION ALL
    SELECT 'ROLE_PROJECT_LEADER', 'message:recall' UNION ALL
    SELECT 'ROLE_PROJECT_LEADER', 'group:list' UNION ALL
    SELECT 'ROLE_PROJECT_LEADER', 'department:list' UNION ALL
    SELECT 'ROLE_PROJECT_LEADER', 'department:members:list' UNION ALL
    SELECT 'ROLE_PROJECT_LEADER', 'document:create' UNION ALL
    SELECT 'ROLE_PROJECT_LEADER', 'document:list' UNION ALL
    SELECT 'ROLE_PROJECT_LEADER', 'document:get' UNION ALL
    SELECT 'ROLE_PROJECT_LEADER', 'document:update' UNION ALL
    SELECT 'ROLE_PROJECT_LEADER', 'document:delete' UNION ALL
    SELECT 'ROLE_PROJECT_LEADER', 'document:permission:user:set' UNION ALL
    SELECT 'ROLE_PROJECT_LEADER', 'document:permission:group:set' UNION ALL
    SELECT 'ROLE_PROJECT_LEADER', 'document:permission:list' UNION ALL
    SELECT 'ROLE_PROJECT_LEADER', 'document:permission:user:delete' UNION ALL
    SELECT 'ROLE_PROJECT_LEADER', 'document:permission:group:delete' UNION ALL
    SELECT 'ROLE_PROJECT_LEADER', 'task:list' UNION ALL
    SELECT 'ROLE_PROJECT_LEADER', 'task:get' UNION ALL
    SELECT 'ROLE_PROJECT_LEADER', 'task:create' UNION ALL
    SELECT 'ROLE_PROJECT_LEADER', 'task:update' UNION ALL
    SELECT 'ROLE_PROJECT_LEADER', 'task:status:update' UNION ALL
    SELECT 'ROLE_USER', 'user:me:get' UNION ALL
    SELECT 'ROLE_USER', 'user:me:update' UNION ALL
    SELECT 'ROLE_USER', 'user:password:update' UNION ALL
    SELECT 'ROLE_USER', 'user:get' UNION ALL
    SELECT 'ROLE_USER', 'friend:list' UNION ALL
    SELECT 'ROLE_USER', 'friend:request:list' UNION ALL
    SELECT 'ROLE_USER', 'friend:request:create' UNION ALL
    SELECT 'ROLE_USER', 'friend:delete' UNION ALL
    SELECT 'ROLE_USER', 'message:list' UNION ALL
    SELECT 'ROLE_USER', 'message:send' UNION ALL
    SELECT 'ROLE_USER', 'message:recall' UNION ALL
    SELECT 'ROLE_USER', 'group:list' UNION ALL
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
JOIN `role` r
    ON r.`role_code` = rp.role_code
JOIN `permission` p
    ON p.`permission_code` = rp.permission_code;
