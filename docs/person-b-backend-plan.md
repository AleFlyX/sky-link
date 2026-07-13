# 三天分工文档 - 成员 B（后端主线二）

## 负责范围

成员 B 负责核心协作业务，包括好友、群聊、消息、文件、文档、任务、日程、公告和统计相关后端能力。

对应模块：

- 好友：`/friends/**`
- 群聊：`/groups/**`
- 消息：`/messages/**`
- 文件：`/files/**`
- 文档：`/documents/**`
- 任务：`/tasks/**`
- 日程：`/schedules/**`
- 公告：`/notices/**`
- 统计：`/statistics/overview`

对应重点表：

- `friendship` `chat_group` `group_member` `message`
- `sys_file` `file_share` `file_favorite` `file_log`
- `document` `document_permission` `document_favorite`
- `task` `task_attachment`
- `schedule`
- `notice` `notice_read`
- `delete_log`

## 落地目标

- 第 1 天结束前，协作业务主表和接口骨架完整。
- 第 2 天结束前，文件、任务、消息三条主流程可演示。
- 第 3 天结束前，文档、公告、日程和统计至少达到“可展示版本”。

## 任务拆解

### 必做

- 好友申请与好友列表
- 群聊创建与成员管理
- 消息发送与历史消息
- 文件上传元数据、列表、分享、删除、收藏
- 任务 CRUD、状态更新、任务附件
- 公告 CRUD、已读、未读数量
- 日程 CRUD

### 可延后

- 更复杂的会话列表排序优化
- 文件分享多目标批量细化
- 更复杂的统计筛选条件

## Day 1

- 完成协作业务主表实体、DTO、VO、Mapper、Service 骨架。
- 先落库最核心链路：好友、群聊、消息、文件、任务。
- 明确消息类型、任务状态、公告类型等枚举值，和成员 A、C 对齐。

具体清单：

- 完成 `friendship`、`chat_group`、`group_member`、`message` 实体与接口骨架
- 完成 `sys_file`、`file_share`、`file_favorite`、`task`、`task_attachment` 骨架
- 给成员 C 一份枚举映射说明：
  - 消息类型
  - 任务状态
  - 公告类型
  - 文件权限

交付物：

- 协作域主要实体和接口骨架
- 文件/消息/任务数据结构说明
- 一份“优先联调接口顺序”清单

## Day 2

- 实现好友申请、好友列表、群创建、群成员管理、消息发送与历史消息查询。
- 实现文件上传元数据、文件列表、文件分享、文件删除、文件收藏。
- 实现任务 CRUD、任务状态更新、任务附件关联。

具体清单：

- 跑通 `/friends/requests` `/friends` `/groups` `/groups/{id}/members`
- 跑通 `/messages` `/messages/sessions` `/messages/read`
- 跑通 `/files` `/files/{id}` `/files/{id}/share` `/files/shares`
- 跑通 `/tasks` `/tasks/{id}` `/tasks/{id}/status`

交付物：

- 协作主流程接口可自测
- 文件、任务、消息三条主链路打通
- 至少 10 个接口能返回真实数据

## Day 3

- 实现文档 CRUD、文档权限、文档收藏。
- 实现日程 CRUD、公告 CRUD、公告已读、未读数量、统计概览。
- 联调成员 C 的文件页、任务页、公告页、消息页，并补齐 `file_log`、`delete_log`。

具体清单：

- 跑通 `/documents`、`/documents/{id}/permissions`
- 跑通 `/schedules`
- 跑通 `/notices` `/notices/{id}/read` `/notices/unread/count`
- 跑通 `/statistics/overview`
- 对文件删除、任务删除、文档删除补删除日志记录

验收标准：

- 文件、任务、公告、文档至少能完成增删改查主流程
- 消息、群聊、好友至少能跑通基础业务链
- 统计接口能返回课程设计展示所需数据
- 成员 C 可以基于真实接口完成主要业务页面展示

## 提交节奏

- Day 1 下午前提交实体和骨架
- Day 2 中午前提交文件和任务接口
- Day 2 晚上提交消息和群聊接口
- Day 3 下午进入联调修 bug 状态

## 风险与兜底

- 如果 WebSocket 来不及，消息先做轮询或普通历史查询展示。
- 如果真实上传链路来不及，先完成文件元数据接口，上传可临时返回模拟路径。
- 如果统计接口来不及做全，先返回静态聚合结果或基础统计字段。

## 协作说明

- 依赖成员 A 先给出登录鉴权和用户身份解析。
- 为成员 C 提供最先可联调的接口顺序建议：
  1. 文件
  2. 任务
  3. 公告
  4. 消息
- 所有需要记录行为的删除操作，同步写入 `delete_log`。
