~~~ sql
-- 设置字符集和存储引擎
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 1. 部门表 (Department)
-- ----------------------------
DROP TABLE IF EXISTS `department`;
CREATE TABLE `department` (
  `department_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '部门编号',
  `department_name` VARCHAR(50) NOT NULL COMMENT '部门名称',
  `leader_id` BIGINT DEFAULT NULL COMMENT '负责人ID（关联用户表）',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '部门描述',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
  PRIMARY KEY (`department_id`),
  KEY `idx_leader_id` (`leader_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='部门表';

-- ----------------------------
-- 2. 用户表 (User)
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `user_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `password` VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
  `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
  `avatar` VARCHAR(255) DEFAULT '/uploads/avatars/default.jpg' COMMENT '头像相对路径',
  `email` VARCHAR(100) NOT NULL COMMENT '邮箱',
  `phone` VARCHAR(20) NOT NULL COMMENT '手机号',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态 0-禁用 1-启用',
  `department_id` BIGINT DEFAULT NULL COMMENT '所属部门ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_email` (`email`),
  UNIQUE KEY `uk_phone` (`phone`),
  KEY `idx_department_id` (`department_id`),
  KEY `idx_status` (`status`),
  CONSTRAINT `fk_user_department` FOREIGN KEY (`department_id`) REFERENCES `department` (`department_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ----------------------------
-- 3. 角色表 (Role)
-- ----------------------------
DROP TABLE IF EXISTS `role`;
CREATE TABLE `role` (
  `role_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_name` VARCHAR(50) NOT NULL COMMENT '角色名称',
  `role_code` VARCHAR(50) NOT NULL COMMENT '角色编码（如 ROLE_ADMIN）',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '角色描述',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
  PRIMARY KEY (`role_id`),
  UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- ----------------------------
-- 4. 权限表 (Permission)
-- ----------------------------
DROP TABLE IF EXISTS `permission`;
CREATE TABLE `permission` (
  `permission_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '权限ID',
  `permission_name` VARCHAR(50) NOT NULL COMMENT '权限名称',
  `permission_code` VARCHAR(100) NOT NULL COMMENT '权限编码（如 user:add）',
  `permission_type` TINYINT DEFAULT 1 COMMENT '权限类型 1-菜单 2-按钮',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
  PRIMARY KEY (`permission_id`),
  UNIQUE KEY `uk_permission_code` (`permission_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';

-- ----------------------------
-- 5. 用户角色关联表 (UserRole)
-- ----------------------------
DROP TABLE IF EXISTS `user_role`;
CREATE TABLE `user_role` (
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`user_id`, `role_id`),
  KEY `idx_role_id` (`role_id`),
  CONSTRAINT `fk_userrole_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_userrole_role` FOREIGN KEY (`role_id`) REFERENCES `role` (`role_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

-- ----------------------------
-- 6. 好友表 (Friend)
-- ----------------------------
DROP TABLE IF EXISTS `friend`;
CREATE TABLE `friend` (
  `friend_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '好友关系ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID（发起方）',
  `friend_user_id` BIGINT NOT NULL COMMENT '好友用户ID（接收方）',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态 0-待审核 1-已同意 2-已拒绝',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '状态更新时间',
  PRIMARY KEY (`friend_id`),
  UNIQUE KEY `uk_user_friend` (`user_id`, `friend_user_id`),
  KEY `idx_friend_user_id` (`friend_user_id`),
  KEY `idx_status` (`status`),
  CONSTRAINT `fk_friend_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_friend_target` FOREIGN KEY (`friend_user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='好友表';

-- ----------------------------
-- 7. 群聊表 (Group)
-- ----------------------------
DROP TABLE IF EXISTS `group`;
CREATE TABLE `group` (
  `group_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '群聊ID',
  `group_name` VARCHAR(50) NOT NULL COMMENT '群聊名称',
  `owner_id` BIGINT NOT NULL COMMENT '群主ID',
  `avatar` VARCHAR(255) DEFAULT '/uploads/group/default.jpg' COMMENT '群头像相对路径',
  `notice` VARCHAR(255) DEFAULT NULL COMMENT '群公告',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
  PRIMARY KEY (`group_id`),
  KEY `idx_owner_id` (`owner_id`),
  CONSTRAINT `fk_group_owner` FOREIGN KEY (`owner_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群聊表';

-- ----------------------------
-- 8. 群成员表 (GroupMember)
-- ----------------------------
DROP TABLE IF EXISTS `group_member`;
CREATE TABLE `group_member` (
  `group_id` BIGINT NOT NULL COMMENT '群聊ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `role` VARCHAR(20) NOT NULL DEFAULT 'member' COMMENT '角色 owner-群主 admin-管理员 member-普通成员',
  `join_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  PRIMARY KEY (`group_id`, `user_id`),
  KEY `idx_user_id` (`user_id`),
  CONSTRAINT `fk_gmember_group` FOREIGN KEY (`group_id`) REFERENCES `group` (`group_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_gmember_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群成员表';

-- ----------------------------
-- 9. 消息表 (Message)
-- ----------------------------
DROP TABLE IF EXISTS `message`;
CREATE TABLE `message` (
  `message_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `sender_id` BIGINT DEFAULT NULL COMMENT '发送者ID（系统消息可为空）',
  `receiver_id` BIGINT DEFAULT NULL COMMENT '接收者ID（单聊）',
  `group_id` BIGINT DEFAULT NULL COMMENT '群聊ID（群聊）',
  `message_type` VARCHAR(20) NOT NULL COMMENT '消息类型 text/image/file/system',
  `content` TEXT NOT NULL COMMENT '消息内容（或文件路径）',
  `send_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  `read_status` TINYINT NOT NULL DEFAULT 0 COMMENT '已读状态 0-未读 1-已读',
  `is_recalled` TINYINT NOT NULL DEFAULT 0 COMMENT '是否撤回 0-否 1-是',
  PRIMARY KEY (`message_id`),
  KEY `idx_sender_id` (`sender_id`),
  KEY `idx_receiver_id` (`receiver_id`),
  KEY `idx_group_id` (`group_id`),
  KEY `idx_send_time` (`send_time`),
  KEY `idx_read_status` (`read_status`),
  CONSTRAINT `fk_msg_sender` FOREIGN KEY (`sender_id`) REFERENCES `user` (`user_id`) ON DELETE SET NULL,
  CONSTRAINT `fk_msg_receiver` FOREIGN KEY (`receiver_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_msg_group` FOREIGN KEY (`group_id`) REFERENCES `group` (`group_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息表';

-- ----------------------------
-- 10. 文件表 (File)
-- ----------------------------
DROP TABLE IF EXISTS `file`;
CREATE TABLE `file` (
  `file_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '文件ID',
  `file_name` VARCHAR(255) NOT NULL COMMENT '文件原始名称',
  `file_path` VARCHAR(500) NOT NULL COMMENT '文件存储相对路径（如 /uploads/files/xxx.pdf）',
  `file_size` BIGINT NOT NULL COMMENT '文件大小（字节）',
  `file_type` VARCHAR(50) DEFAULT NULL COMMENT '文件类型（如 pdf, docx, jpg）',
  `owner_id` BIGINT NOT NULL COMMENT '上传者ID',
  `upload_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
  PRIMARY KEY (`file_id`),
  KEY `idx_owner_id` (`owner_id`),
  KEY `idx_file_type` (`file_type`),
  CONSTRAINT `fk_file_owner` FOREIGN KEY (`owner_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件表';

-- ----------------------------
-- 11. 文件共享表 (FileShare)
-- ----------------------------
DROP TABLE IF EXISTS `file_share`;
CREATE TABLE `file_share` (
  `share_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '共享记录ID',
  `file_id` BIGINT NOT NULL COMMENT '文件ID',
  `target_user_id` BIGINT DEFAULT NULL COMMENT '目标用户ID（分享给个人）',
  `target_group_id` BIGINT DEFAULT NULL COMMENT '目标群组ID（分享给群组）',
  `permission` VARCHAR(20) NOT NULL DEFAULT 'view' COMMENT '权限 view-只读 edit-可编辑',
  `expire_time` DATETIME DEFAULT NULL COMMENT '过期时间（NULL表示永久）',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`share_id`),
  KEY `idx_file_id` (`file_id`),
  KEY `idx_target_user` (`target_user_id`),
  KEY `idx_target_group` (`target_group_id`),
  CONSTRAINT `fk_fshare_file` FOREIGN KEY (`file_id`) REFERENCES `file` (`file_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_fshare_user` FOREIGN KEY (`target_user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_fshare_group` FOREIGN KEY (`target_group_id`) REFERENCES `group` (`group_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件共享表';

-- ----------------------------
-- 12. 在线文档表 (Document)
-- ----------------------------
DROP TABLE IF EXISTS `document`;
CREATE TABLE `document` (
  `document_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '文档ID',
  `title` VARCHAR(100) NOT NULL COMMENT '文档标题',
  `content` LONGTEXT COMMENT '文档内容（Markdown格式）',
  `creator_id` BIGINT NOT NULL COMMENT '创建者ID',
  `status` VARCHAR(20) NOT NULL DEFAULT 'private' COMMENT '状态 private-私有 team-团队共享',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
  PRIMARY KEY (`document_id`),
  KEY `idx_creator_id` (`creator_id`),
  KEY `idx_status` (`status`),
  CONSTRAINT `fk_doc_creator` FOREIGN KEY (`creator_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='在线文档表';

-- ----------------------------
-- 13. 文档权限表 (DocumentPermission)
-- ----------------------------
DROP TABLE IF EXISTS `document_permission`;
CREATE TABLE `document_permission` (
  `document_id` BIGINT NOT NULL COMMENT '文档ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `permission_type` VARCHAR(20) NOT NULL COMMENT '权限类型 read-只读 comment-评论 edit-编辑 manage-管理',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`document_id`, `user_id`),
  KEY `idx_user_id` (`user_id`),
  CONSTRAINT `fk_docperm_doc` FOREIGN KEY (`document_id`) REFERENCES `document` (`document_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_docperm_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档权限表';

-- ----------------------------
-- 14. 任务表 (Task)
-- ----------------------------
DROP TABLE IF EXISTS `task`;
CREATE TABLE `task` (
  `task_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `title` VARCHAR(100) NOT NULL COMMENT '任务标题',
  `content` TEXT COMMENT '任务描述',
  `creator_id` BIGINT NOT NULL COMMENT '创建人ID',
  `executor_id` BIGINT DEFAULT NULL COMMENT '负责人ID（执行人）',
  `priority` TINYINT NOT NULL DEFAULT 1 COMMENT '优先级 1-低 2-中 3-高',
  `status` VARCHAR(20) NOT NULL DEFAULT '未开始' COMMENT '状态 未开始/进行中/已完成',
  `deadline` DATETIME DEFAULT NULL COMMENT '截止时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
  PRIMARY KEY (`task_id`),
  KEY `idx_creator_id` (`creator_id`),
  KEY `idx_executor_id` (`executor_id`),
  KEY `idx_status` (`status`),
  KEY `idx_deadline` (`deadline`),
  CONSTRAINT `fk_task_creator` FOREIGN KEY (`creator_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_task_executor` FOREIGN KEY (`executor_id`) REFERENCES `user` (`user_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务表';

-- ----------------------------
-- 15. 日程表 (Schedule)
-- ----------------------------
DROP TABLE IF EXISTS `schedule`;
CREATE TABLE `schedule` (
  `schedule_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日程ID',
  `title` VARCHAR(100) NOT NULL COMMENT '日程标题',
  `content` TEXT COMMENT '日程描述',
  `user_id` BIGINT NOT NULL COMMENT '所属用户ID',
  `start_time` DATETIME NOT NULL COMMENT '开始时间',
  `end_time` DATETIME NOT NULL COMMENT '结束时间',
  `remind_time` DATETIME DEFAULT NULL COMMENT '提醒时间',
  `repeat_type` VARCHAR(20) NOT NULL DEFAULT 'none' COMMENT '重复类型 none-不重复 daily-每天 weekly-每周 monthly-每月',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
  PRIMARY KEY (`schedule_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_start_time` (`start_time`),
  KEY `idx_end_time` (`end_time`),
  CONSTRAINT `fk_schedule_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='日程表';

-- ----------------------------
-- 16. 公告通知表 (Notice)
-- ----------------------------
DROP TABLE IF EXISTS `notice`;
CREATE TABLE `notice` (
  `notice_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '公告ID',
  `title` VARCHAR(100) NOT NULL COMMENT '公告标题',
  `content` TEXT NOT NULL COMMENT '公告内容',
  `publisher_id` BIGINT NOT NULL COMMENT '发布者ID',
  `publish_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态 0-草稿 1-已发布',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
  PRIMARY KEY (`notice_id`),
  KEY `idx_publisher_id` (`publisher_id`),
  KEY `idx_status` (`status`),
  KEY `idx_publish_time` (`publish_time`),
  CONSTRAINT `fk_notice_publisher` FOREIGN KEY (`publisher_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公告通知表';

-- ----------------------------
-- 17. 登录日志表 (LoginLog)
-- ----------------------------
DROP TABLE IF EXISTS `login_log`;
CREATE TABLE `login_log` (
  `log_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `user_id` BIGINT DEFAULT NULL COMMENT '用户ID（未登录为空）',
  `login_ip` VARCHAR(45) DEFAULT NULL COMMENT '登录IP地址',
  `device` VARCHAR(100) DEFAULT NULL COMMENT '设备信息',
  `browser` VARCHAR(100) DEFAULT NULL COMMENT '浏览器信息',
  `login_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
  `login_result` TINYINT NOT NULL DEFAULT 1 COMMENT '登录结果 0-失败 1-成功',
  PRIMARY KEY (`log_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_login_time` (`login_time`),
  KEY `idx_login_result` (`login_result`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='登录日志表';

-- ----------------------------
-- 18. 操作日志表 (OperationLog)
-- ----------------------------
DROP TABLE IF EXISTS `operation_log`;
CREATE TABLE `operation_log` (
  `operation_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '操作ID',
  `user_id` BIGINT NOT NULL COMMENT '操作用户ID',
  `module` VARCHAR(50) NOT NULL COMMENT '操作模块（如 用户管理）',
  `operation` VARCHAR(100) NOT NULL COMMENT '操作描述（如 删除用户）',
  `request_url` VARCHAR(255) DEFAULT NULL COMMENT '请求URL',
  `request_method` VARCHAR(10) DEFAULT NULL COMMENT '请求方法（GET/POST等）',
  `operation_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`operation_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_module` (`module`),
  KEY `idx_operation_time` (`operation_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

-- 恢复外键检查
SET FOREIGN_KEY_CHECKS = 1;

~~~