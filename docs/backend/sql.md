~~~sql
-- SkyLink 数据库建表脚本（规范版）
-- 设计约定：
-- 1. 主业务表使用 BIGINT 自增主键；多对多关联表使用复合主键。
-- 2. 角色、权限等配置型实体同时保留稳定业务编码唯一约束。
-- 3. 业务数据优先采用逻辑删除，不依赖外键级联物理删除历史数据。
-- 4. 使用更明确的表名，避免 group、file 等保留字/泛化名。

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 1. 部门表
-- ----------------------------
DROP TABLE IF EXISTS `department`;
CREATE TABLE `department` (
  `department_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '部门ID',
  `department_name` VARCHAR(50) NOT NULL COMMENT '部门名称',
  `leader_id` BIGINT DEFAULT NULL COMMENT '负责人用户ID',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '部门描述',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
  PRIMARY KEY (`department_id`),
  UNIQUE KEY `uk_department_name` (`department_name`),
  KEY `idx_department_leader_id` (`leader_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='部门表';

-- ----------------------------
-- 2. 用户表
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `user_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `password` VARCHAR(255) NOT NULL COMMENT '密码（BCrypt 等单向哈希）',
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
  UNIQUE KEY `uk_user_username` (`username`),
  UNIQUE KEY `uk_user_email` (`email`),
  UNIQUE KEY `uk_user_phone` (`phone`),
  KEY `idx_user_department_id` (`department_id`),
  KEY `idx_user_status` (`status`),
  CONSTRAINT `fk_user_department` FOREIGN KEY (`department_id`) REFERENCES `department` (`department_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

ALTER TABLE `department`
  ADD CONSTRAINT `fk_department_leader` FOREIGN KEY (`leader_id`) REFERENCES `user` (`user_id`) ON DELETE SET NULL;

-- ----------------------------
-- 3. 角色表
-- ----------------------------
DROP TABLE IF EXISTS `role`;
CREATE TABLE `role` (
  `role_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_name` VARCHAR(50) NOT NULL COMMENT '角色名称',
  `role_code` VARCHAR(50) NOT NULL COMMENT '角色编码，如 ROLE_ADMIN',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '角色描述',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态 0-禁用 1-启用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
  PRIMARY KEY (`role_id`),
  UNIQUE KEY `uk_role_name` (`role_name`),
  UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- ----------------------------
-- 4. 权限表
-- ----------------------------
DROP TABLE IF EXISTS `permission`;
CREATE TABLE `permission` (
  `permission_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '权限ID',
  `permission_name` VARCHAR(50) NOT NULL COMMENT '权限名称',
  `permission_code` VARCHAR(100) NOT NULL COMMENT '权限编码，如 user:add',
  `permission_type` TINYINT NOT NULL DEFAULT 1 COMMENT '权限类型 1-菜单 2-按钮 3-接口',
  `parent_id` BIGINT DEFAULT NULL COMMENT '父权限ID',
  `sort_no` INT NOT NULL DEFAULT 0 COMMENT '排序号',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
  PRIMARY KEY (`permission_id`),
  UNIQUE KEY `uk_permission_code` (`permission_code`),
  KEY `idx_permission_parent_id` (`parent_id`),
  CONSTRAINT `fk_permission_parent` FOREIGN KEY (`parent_id`) REFERENCES `permission` (`permission_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';

-- ----------------------------
-- 5. 用户角色关联表
-- ----------------------------
DROP TABLE IF EXISTS `user_role`;
CREATE TABLE `user_role` (
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`user_id`, `role_id`),
  KEY `idx_user_role_role_id` (`role_id`),
  CONSTRAINT `fk_user_role_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_user_role_role` FOREIGN KEY (`role_id`) REFERENCES `role` (`role_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

-- ----------------------------
-- 6. 角色权限关联表
-- ----------------------------
DROP TABLE IF EXISTS `role_permission`;
CREATE TABLE `role_permission` (
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  `permission_id` BIGINT NOT NULL COMMENT '权限ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`role_id`, `permission_id`),
  KEY `idx_role_permission_permission_id` (`permission_id`),
  CONSTRAINT `fk_role_permission_role` FOREIGN KEY (`role_id`) REFERENCES `role` (`role_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_role_permission_permission` FOREIGN KEY (`permission_id`) REFERENCES `permission` (`permission_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关联表';

-- ----------------------------
-- 7. 好友关系表
-- 说明：好友是对称关系，不使用自增主键。
-- ----------------------------
DROP TABLE IF EXISTS `friendship`;
CREATE TABLE `friendship` (
  `user_id` BIGINT NOT NULL COMMENT '用户ID，恒小于 friend_user_id',
  `friend_user_id` BIGINT NOT NULL COMMENT '好友用户ID',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态 0-待确认 1-已成为好友 2-已拒绝 3-已拉黑',
  `initiator_id` BIGINT NOT NULL COMMENT '发起人ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`user_id`, `friend_user_id`),
  KEY `idx_friendship_initiator_id` (`initiator_id`),
  KEY `idx_friendship_status` (`status`),
  CONSTRAINT `ck_friendship_user_order` CHECK (`user_id` < `friend_user_id`),
  CONSTRAINT `fk_friendship_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_friendship_friend_user` FOREIGN KEY (`friend_user_id`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_friendship_initiator` FOREIGN KEY (`initiator_id`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='好友关系表';

-- ----------------------------
-- 8. 群聊表
-- ----------------------------
DROP TABLE IF EXISTS `chat_group`;
CREATE TABLE `chat_group` (
  `group_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '群聊ID',
  `group_name` VARCHAR(50) NOT NULL COMMENT '群聊名称',
  `owner_id` BIGINT NOT NULL COMMENT '群主ID',
  `avatar` VARCHAR(255) DEFAULT '/uploads/group/default.jpg' COMMENT '群头像相对路径',
  `notice` VARCHAR(255) DEFAULT NULL COMMENT '群公告',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态 0-停用 1-正常',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
  PRIMARY KEY (`group_id`),
  KEY `idx_chat_group_owner_id` (`owner_id`),
  CONSTRAINT `fk_chat_group_owner` FOREIGN KEY (`owner_id`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群聊表';

-- ----------------------------
-- 9. 群成员表
-- ----------------------------
DROP TABLE IF EXISTS `group_member`;
CREATE TABLE `group_member` (
  `group_id` BIGINT NOT NULL COMMENT '群聊ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `member_role` TINYINT NOT NULL DEFAULT 3 COMMENT '成员角色 1-群主 2-管理员 3-普通成员',
  `join_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  PRIMARY KEY (`group_id`, `user_id`),
  KEY `idx_group_member_user_id` (`user_id`),
  CONSTRAINT `fk_group_member_group` FOREIGN KEY (`group_id`) REFERENCES `chat_group` (`group_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_group_member_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群成员表';

-- ----------------------------
-- 10. 消息表
-- 说明：单聊和群聊二选一。
-- ----------------------------
DROP TABLE IF EXISTS `message`;
CREATE TABLE `message` (
  `message_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `sender_id` BIGINT DEFAULT NULL COMMENT '发送者ID，系统消息可为空',
  `receiver_id` BIGINT DEFAULT NULL COMMENT '接收者ID，单聊使用',
  `group_id` BIGINT DEFAULT NULL COMMENT '群聊ID，群聊使用',
  `message_type` TINYINT NOT NULL COMMENT '消息类型 1-文本 2-图片 3-文件 4-系统',
  `content` TEXT NOT NULL COMMENT '消息内容或资源路径',
  `send_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  `read_status` TINYINT NOT NULL DEFAULT 0 COMMENT '已读状态 0-未读 1-已读',
  `is_recalled` TINYINT NOT NULL DEFAULT 0 COMMENT '是否撤回 0-否 1-是',
  PRIMARY KEY (`message_id`),
  KEY `idx_message_sender_id` (`sender_id`),
  KEY `idx_message_receiver_id` (`receiver_id`),
  KEY `idx_message_group_id` (`group_id`),
  KEY `idx_message_send_time` (`send_time`),
  CONSTRAINT `ck_message_target` CHECK (
    (`receiver_id` IS NOT NULL AND `group_id` IS NULL) OR
    (`receiver_id` IS NULL AND `group_id` IS NOT NULL)
  ),
  CONSTRAINT `fk_message_sender` FOREIGN KEY (`sender_id`) REFERENCES `user` (`user_id`) ON DELETE SET NULL,
  CONSTRAINT `fk_message_receiver` FOREIGN KEY (`receiver_id`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_message_group` FOREIGN KEY (`group_id`) REFERENCES `chat_group` (`group_id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息表';

-- ----------------------------
-- 11. 文件表
-- ----------------------------
DROP TABLE IF EXISTS `sys_file`;
CREATE TABLE `sys_file` (
  `file_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '文件ID',
  `file_name` VARCHAR(255) NOT NULL COMMENT '文件原始名称',
  `file_path` VARCHAR(500) NOT NULL COMMENT '文件存储相对路径',
  `file_size` BIGINT NOT NULL COMMENT '文件大小（字节）',
  `file_ext` VARCHAR(20) DEFAULT NULL COMMENT '文件扩展名，如 pdf、docx',
  `mime_type` VARCHAR(100) DEFAULT NULL COMMENT 'MIME 类型',
  `owner_id` BIGINT NOT NULL COMMENT '上传者ID',
  `upload_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
  PRIMARY KEY (`file_id`),
  KEY `idx_sys_file_owner_id` (`owner_id`),
  KEY `idx_sys_file_upload_time` (`upload_time`),
  CONSTRAINT `fk_sys_file_owner` FOREIGN KEY (`owner_id`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件表';

-- ----------------------------
-- 12. 文件共享表
-- 说明：目标用户和目标群组二选一。
-- ----------------------------
DROP TABLE IF EXISTS `file_share`;
CREATE TABLE `file_share` (
  `share_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '共享记录ID',
  `file_id` BIGINT NOT NULL COMMENT '文件ID',
  `target_user_id` BIGINT DEFAULT NULL COMMENT '目标用户ID',
  `target_group_id` BIGINT DEFAULT NULL COMMENT '目标群组ID',
  `permission_type` TINYINT NOT NULL DEFAULT 1 COMMENT '权限 1-查看 2-编辑',
  `expire_time` DATETIME DEFAULT NULL COMMENT '过期时间，NULL 表示永久',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`share_id`),
  UNIQUE KEY `uk_file_share_user` (`file_id`, `target_user_id`),
  UNIQUE KEY `uk_file_share_group` (`file_id`, `target_group_id`),
  KEY `idx_file_share_target_user_id` (`target_user_id`),
  KEY `idx_file_share_target_group_id` (`target_group_id`),
  CONSTRAINT `ck_file_share_target` CHECK (
    (`target_user_id` IS NOT NULL AND `target_group_id` IS NULL) OR
    (`target_user_id` IS NULL AND `target_group_id` IS NOT NULL)
  ),
  CONSTRAINT `fk_file_share_file` FOREIGN KEY (`file_id`) REFERENCES `sys_file` (`file_id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_file_share_user` FOREIGN KEY (`target_user_id`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_file_share_group` FOREIGN KEY (`target_group_id`) REFERENCES `chat_group` (`group_id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件共享表';

-- ----------------------------
-- 13. 在线文档表
-- ----------------------------
DROP TABLE IF EXISTS `document`;
CREATE TABLE `document` (
  `document_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '文档ID',
  `title` VARCHAR(100) NOT NULL COMMENT '文档标题',
  `content` LONGTEXT COMMENT '文档内容，建议 Markdown 或富文本 JSON',
  `creator_id` BIGINT NOT NULL COMMENT '创建者ID',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1-私有 2-团队共享 3-归档',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
  PRIMARY KEY (`document_id`),
  KEY `idx_document_creator_id` (`creator_id`),
  KEY `idx_document_status` (`status`),
  CONSTRAINT `fk_document_creator` FOREIGN KEY (`creator_id`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='在线文档表';

-- ----------------------------
-- 14. 文档权限表
-- ----------------------------
DROP TABLE IF EXISTS `document_permission`;
CREATE TABLE `document_permission` (
  `document_id` BIGINT NOT NULL COMMENT '文档ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `permission_type` TINYINT NOT NULL COMMENT '权限 1-只读 2-评论 3-编辑 4-管理',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`document_id`, `user_id`),
  KEY `idx_document_permission_user_id` (`user_id`),
  CONSTRAINT `fk_document_permission_document` FOREIGN KEY (`document_id`) REFERENCES `document` (`document_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_document_permission_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档权限表';

-- ----------------------------
-- 15. 任务表
-- ----------------------------
DROP TABLE IF EXISTS `task`;
CREATE TABLE `task` (
  `task_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `title` VARCHAR(100) NOT NULL COMMENT '任务标题',
  `content` TEXT DEFAULT NULL COMMENT '任务描述',
  `creator_id` BIGINT NOT NULL COMMENT '创建人ID',
  `executor_id` BIGINT DEFAULT NULL COMMENT '执行人ID',
  `priority` TINYINT NOT NULL DEFAULT 2 COMMENT '优先级 1-低 2-中 3-高',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1-未开始 2-进行中 3-已完成 4-已取消',
  `deadline` DATETIME DEFAULT NULL COMMENT '截止时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
  PRIMARY KEY (`task_id`),
  KEY `idx_task_creator_id` (`creator_id`),
  KEY `idx_task_executor_id` (`executor_id`),
  KEY `idx_task_status_deadline` (`status`, `deadline`),
  CONSTRAINT `fk_task_creator` FOREIGN KEY (`creator_id`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_task_executor` FOREIGN KEY (`executor_id`) REFERENCES `user` (`user_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务表';

-- ----------------------------
-- 16. 日程表
-- ----------------------------
DROP TABLE IF EXISTS `schedule`;
CREATE TABLE `schedule` (
  `schedule_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日程ID',
  `title` VARCHAR(100) NOT NULL COMMENT '日程标题',
  `content` TEXT DEFAULT NULL COMMENT '日程描述',
  `user_id` BIGINT NOT NULL COMMENT '所属用户ID',
  `start_time` DATETIME NOT NULL COMMENT '开始时间',
  `end_time` DATETIME NOT NULL COMMENT '结束时间',
  `remind_time` DATETIME DEFAULT NULL COMMENT '提醒时间',
  `repeat_type` TINYINT NOT NULL DEFAULT 0 COMMENT '重复类型 0-不重复 1-每天 2-每周 3-每月',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
  PRIMARY KEY (`schedule_id`),
  KEY `idx_schedule_user_start_time` (`user_id`, `start_time`),
  CONSTRAINT `ck_schedule_time` CHECK (`end_time` >= `start_time`),
  CONSTRAINT `fk_schedule_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='日程表';

-- ----------------------------
-- 17. 公告通知表
-- ----------------------------
DROP TABLE IF EXISTS `notice`;
CREATE TABLE `notice` (
  `notice_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '公告ID',
  `title` VARCHAR(100) NOT NULL COMMENT '公告标题',
  `content` TEXT NOT NULL COMMENT '公告内容',
  `publisher_id` BIGINT NOT NULL COMMENT '发布者ID',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态 0-草稿 1-已发布 2-已撤回',
  `publish_time` DATETIME DEFAULT NULL COMMENT '发布时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
  PRIMARY KEY (`notice_id`),
  KEY `idx_notice_publisher_id` (`publisher_id`),
  KEY `idx_notice_status_publish_time` (`status`, `publish_time`),
  CONSTRAINT `fk_notice_publisher` FOREIGN KEY (`publisher_id`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公告通知表';

-- ----------------------------
-- 18. 登录日志表
-- 日志数据保留历史，不对用户做外键约束。
-- ----------------------------
DROP TABLE IF EXISTS `login_log`;
CREATE TABLE `login_log` (
  `log_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `user_id` BIGINT DEFAULT NULL COMMENT '用户ID，未登录或账号已删除时可为空',
  `login_ip` VARCHAR(45) DEFAULT NULL COMMENT '登录IP地址',
  `device` VARCHAR(100) DEFAULT NULL COMMENT '设备信息',
  `browser` VARCHAR(100) DEFAULT NULL COMMENT '浏览器信息',
  `login_result` TINYINT NOT NULL DEFAULT 1 COMMENT '登录结果 0-失败 1-成功',
  `login_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
  PRIMARY KEY (`log_id`),
  KEY `idx_login_log_user_id` (`user_id`),
  KEY `idx_login_log_login_time` (`login_time`),
  KEY `idx_login_log_login_result` (`login_result`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='登录日志表';

-- ----------------------------
-- 19. 操作日志表
-- 日志数据保留历史，不对用户做外键约束。
-- ----------------------------
DROP TABLE IF EXISTS `operation_log`;
CREATE TABLE `operation_log` (
  `operation_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '操作日志ID',
  `user_id` BIGINT DEFAULT NULL COMMENT '操作用户ID',
  `module` VARCHAR(50) NOT NULL COMMENT '操作模块',
  `operation` VARCHAR(100) NOT NULL COMMENT '操作描述',
  `request_url` VARCHAR(255) DEFAULT NULL COMMENT '请求URL',
  `request_method` VARCHAR(10) DEFAULT NULL COMMENT '请求方法',
  `operation_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`operation_id`),
  KEY `idx_operation_log_user_id` (`user_id`),
  KEY `idx_operation_log_module` (`module`),
  KEY `idx_operation_log_operation_time` (`operation_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

SET FOREIGN_KEY_CHECKS = 1;
~~~
