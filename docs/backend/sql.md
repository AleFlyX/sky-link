~~~sql
-- SkyLink 数据库建表脚本（与 spec.md / model.md 对齐版）
-- 设计约定：
-- 1. 主业务表使用 BIGINT 自增主键；多对多关联表使用复合主键。
-- 2. 角色、权限、系统配置等配置型实体保留稳定业务编码唯一约束。
-- 3. 业务数据优先采用逻辑删除，不依赖级联物理删除历史数据。
-- 4. 对文档收藏、系统配置等需求补充实体承载。

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
  `sort_no` INT NOT NULL DEFAULT 0 COMMENT '排序号',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
  PRIMARY KEY (`permission_id`),
  UNIQUE KEY `uk_permission_code` (`permission_code`)
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
-- 7. 好友申请表
-- ----------------------------
DROP TABLE IF EXISTS `friend_request`;
CREATE TABLE `friend_request` (
  `request_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '好友申请ID',
  `requester_id` BIGINT NOT NULL COMMENT '申请人ID',
  `receiver_id` BIGINT NOT NULL COMMENT '接收人ID',
  `message` VARCHAR(255) DEFAULT NULL COMMENT '申请附言',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态 0-待处理 1-已同意 2-已拒绝',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '处理时间',
  PRIMARY KEY (`request_id`),
  KEY `idx_friend_request_users` (`requester_id`, `receiver_id`),
  KEY `idx_friend_request_receiver_status` (`receiver_id`, `status`),
  CONSTRAINT `ck_friend_request_users` CHECK (`requester_id` <> `receiver_id`),
  CONSTRAINT `fk_friend_request_requester` FOREIGN KEY (`requester_id`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_friend_request_receiver` FOREIGN KEY (`receiver_id`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='好友申请表';

-- 8. 好友关系表，仅保存已经建立的关系
DROP TABLE IF EXISTS `friendship`;
CREATE TABLE `friendship` (
  `user_id` BIGINT NOT NULL COMMENT '用户ID，恒小于 friend_user_id',
  `friend_user_id` BIGINT NOT NULL COMMENT '好友用户ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '成为好友时间',
  PRIMARY KEY (`user_id`, `friend_user_id`),
  CONSTRAINT `ck_friendship_user_order` CHECK (`user_id` < `friend_user_id`),
  CONSTRAINT `fk_friendship_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_friendship_friend_user` FOREIGN KEY (`friend_user_id`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='好友关系表';

-- ----------------------------
-- 9. 群聊表
-- ----------------------------
DROP TABLE IF EXISTS `chat_group`;
CREATE TABLE `chat_group` (
  `group_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '群聊ID',
  `group_name` VARCHAR(50) NOT NULL COMMENT '群聊名称',
  `owner_id` BIGINT NOT NULL COMMENT '群主ID',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态 0-停用 1-正常',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
  PRIMARY KEY (`group_id`),
  KEY `idx_chat_group_owner_id` (`owner_id`),
  CONSTRAINT `fk_chat_group_owner` FOREIGN KEY (`owner_id`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群聊表';

-- ----------------------------
-- 10. 群成员表
-- ----------------------------
DROP TABLE IF EXISTS `group_member`;
CREATE TABLE `group_member` (
  `group_id` BIGINT NOT NULL COMMENT '群聊ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `member_role` TINYINT NOT NULL DEFAULT 3 COMMENT '成员角色 1-群主 2-管理员 3-普通成员 4-已退出',
  `join_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  PRIMARY KEY (`group_id`, `user_id`),
  KEY `idx_group_member_user_id` (`user_id`),
  CONSTRAINT `fk_group_member_group` FOREIGN KEY (`group_id`) REFERENCES `chat_group` (`group_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_group_member_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群成员表';

-- ----------------------------
-- 11. 消息表
-- ----------------------------
DROP TABLE IF EXISTS `message`;
CREATE TABLE `message` (
  `message_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `sender_id` BIGINT DEFAULT NULL COMMENT '发送者ID，系统消息可为空',
  `receiver_id` BIGINT DEFAULT NULL COMMENT '接收者ID，单聊使用',
  `group_id` BIGINT DEFAULT NULL COMMENT '群聊ID，群聊使用',
  `message_type` TINYINT NOT NULL COMMENT '消息类型 1-文本 2-图片 3-系统 4-emoji',
  `content` TEXT NOT NULL COMMENT '消息内容或资源路径',
  `send_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
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
-- 12. 在线文档表
-- ----------------------------
DROP TABLE IF EXISTS `document`;
CREATE TABLE `document` (
  `document_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '文档ID',
  `title` VARCHAR(100) NOT NULL COMMENT '文档标题',
  `content` LONGTEXT COMMENT '文档内容，建议 Markdown 或富文本 JSON',
  `creator_id` BIGINT NOT NULL COMMENT '创建者ID',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1-私有 2-部门可见 3-归档',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
  PRIMARY KEY (`document_id`),
  KEY `idx_document_creator_id` (`creator_id`),
  KEY `idx_document_status` (`status`),
  CONSTRAINT `fk_document_creator` FOREIGN KEY (`creator_id`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='在线文档表';

-- ----------------------------
-- 12.1 文档协同当前状态表
-- ----------------------------
DROP TABLE IF EXISTS `document_collaboration_state`;
CREATE TABLE `document_collaboration_state` (
  `document_id` BIGINT NOT NULL COMMENT '文档ID',
  `ydoc_state` LONGBLOB NOT NULL COMMENT 'Y.Doc合并状态',
  `state_vector` LONGBLOB NOT NULL COMMENT 'Yjs状态向量',
  `revision` BIGINT NOT NULL DEFAULT 0 COMMENT '持久化修订号',
  `updated_by` BIGINT DEFAULT NULL COMMENT '最后编辑用户ID',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`document_id`),
  CONSTRAINT `fk_document_collaboration_document` FOREIGN KEY (`document_id`) REFERENCES `document` (`document_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_document_collaboration_user` FOREIGN KEY (`updated_by`) REFERENCES `user` (`user_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档协同当前状态';

-- ----------------------------
-- 13. 文档权限表
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
-- 14. 文档群组权限表
-- ----------------------------
DROP TABLE IF EXISTS `document_group_permission`;
CREATE TABLE `document_group_permission` (
  `document_id` BIGINT NOT NULL COMMENT '文档ID',
  `group_id` BIGINT NOT NULL COMMENT '群组ID',
  `permission_type` TINYINT NOT NULL COMMENT '权限 1-只读 2-评论 3-编辑 4-管理',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`document_id`, `group_id`),
  KEY `idx_document_group_permission_group_id` (`group_id`),
  CONSTRAINT `fk_document_group_permission_document` FOREIGN KEY (`document_id`) REFERENCES `document` (`document_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_document_group_permission_group` FOREIGN KEY (`group_id`) REFERENCES `chat_group` (`group_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档群组权限表';

-- ----------------------------
-- 15. 文档收藏表
-- ----------------------------
DROP TABLE IF EXISTS `document_favorite`;
CREATE TABLE `document_favorite` (
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `document_id` BIGINT NOT NULL COMMENT '文档ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  PRIMARY KEY (`user_id`, `document_id`),
  KEY `idx_document_favorite_document_id` (`document_id`),
  CONSTRAINT `fk_document_favorite_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_document_favorite_document` FOREIGN KEY (`document_id`) REFERENCES `document` (`document_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档收藏表';

-- ----------------------------
-- 16. 任务表
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
  `start_time` DATETIME DEFAULT NULL COMMENT '开始时间',
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
-- 17. 系统配置表
-- ----------------------------
DROP TABLE IF EXISTS `system_config`;
CREATE TABLE `system_config` (
  `config_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `config_key` VARCHAR(100) NOT NULL COMMENT '配置键',
  `config_value` VARCHAR(1000) DEFAULT NULL COMMENT '配置值',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '配置说明',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`config_id`),
  UNIQUE KEY `uk_system_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

SET FOREIGN_KEY_CHECKS = 1;
~~~

## 安全初始化

应用启动时会先执行 `backend/land/src/main/resources/data.sql`，幂等插入基础角色、接口权限和角色权限关系；随后 `SecurityDataInitializer` 会继续修复缺失或被逻辑删除的安全数据，并为已启用但缺少 `user_role` 的历史用户补绑默认 `ROLE_USER`。首次超级管理员仍需通过 `SKYLINK_BOOTSTRAP_ADMIN_*` 环境变量显式创建，创建完成后关闭引导开关并移除引导密码。

## 自动建表

应用默认启用 Spring SQL 初始化并按顺序执行 `backend/land/src/main/resources/schema.sql` 和 `backend/land/src/main/resources/data.sql`。运行时脚本只使用 `CREATE TABLE IF NOT EXISTS` 与幂等 `INSERT IGNORE`，不会删除已有业务数据；数据库本身仍需由部署环境提前创建。

当前自动建表范围以 `docs/spec-current.md` 定义的数据边界和本文件中的 17 张表为准。文件、日程、系统公告、任务附件和业务审计已因工期从本期需求中移出；仓库中对应保留实体不参与当前 schema 初始化。
