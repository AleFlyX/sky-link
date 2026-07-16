# 任务管理：从创建到状态流转（小白版）

这套任务功能不是“谁登录了都能改所有任务”。它分三层把事情管住：先用 JWT 认出当前是谁，再用权限码决定能不能调用接口，最后由业务代码决定这个人能否操作**这一条具体任务**。

## 先看完整流程

```text
前端点击“新建任务”
  -> POST /api/v1/tasks
  -> JWT 过滤器把当前用户 ID 放入 AuthContext
  -> @RequirePermission("task:create") 校验功能权限
  -> TaskServiceImpl 校验标题、执行人、同部门规则
  -> 插入 task 表（创建者 = 当前登录用户，状态 = 未开始）
  -> 读取任务和相关用户信息
  -> GlobalResponseAdvice 包成 { code, message, data }
  -> 前端拿到新任务
```

## 关键文件地图

| 目的 | 真实代码位置 | 你应关注什么 |
| --- | --- | --- |
| HTTP 接口和权限码 | `backend/land/src/main/java/com/skylink/land/controller/TaskController.java:18` | URL、动作权限、当前用户来源 |
| 核心业务规则 | `backend/land/src/main/java/com/skylink/land/service/task/impl/TaskServiceImpl.java:32` | 同部门、参与者、管理员、事务 |
| 请求/响应对象 | `backend/land/src/main/java/com/skylink/land/dto/task/TaskDto.java` | 前后端传什么字段 |
| 任务实体 | `backend/land/src/main/java/com/skylink/land/entity/task/Task.java` | `task` 表字段映射 |
| 数据访问 | `backend/land/src/main/java/com/skylink/land/mapper/task/TaskMapper.java` | MyBatis-Plus Mapper |

## 接口先做“功能权限”检查

代码位置：`TaskController.java:28-68`

```java
@PostMapping // 1. 只接收 POST /api/v1/tasks。
@RequirePermission("task:create") // 2. 当前用户必须拥有 task:create 权限码。
public TaskDto.TaskResponse createTask(@RequestBody TaskDto.CreateTaskRequest request) {
    // 3. 不信任前端传来的 creatorId；直接从已验证的登录上下文取真实用户 ID。
    return taskService.createTask(AuthContext.requireUserId(), request);
}

@PutMapping("/{taskId}/status") // 4. 修改状态是独立动作，不混在普通编辑接口里。
@RequirePermission("task:status:update") // 5. 先验证“可以使用状态修改功能”。
public TaskDto.TaskResponse updateTaskStatus(
    @PathVariable Long taskId, // 6. URL 中的具体任务 ID。
    @RequestBody TaskDto.UpdateTaskStatusRequest request // 7. 请求体只提供目标状态。
) {
    // 8. Service 还会验证：你是不是这项任务的执行者或管理员。
    return taskService.updateTaskStatus(AuthContext.requireUserId(), taskId, request);
}
```

权限码只回答“能不能进入这个功能”。例如一个拥有 `task:update` 的普通成员，也不能借此修改别人的任务；“是不是这条任务的创建者”要由 Service 继续判断。

## 创建任务：为什么要事务

代码位置：`TaskServiceImpl.java:61-79`

```java
@Transactional(rollbackFor = Exception.class) // 1. 本方法出错时，数据库修改整体回滚。
public TaskDto.TaskResponse createTask(Long currentUserId, TaskDto.CreateTaskRequest request) {
    requireCurrentUserId(currentUserId); // 2. 没有登录用户 ID，直接拒绝。
    validateCreateRequest(request); // 3. 标题、执行人、优先级等基础校验。
    validateExecutorAssignment(currentUserId, request.getExecutorId()); // 4. 校验同部门分配。

    Task task = new Task(); // 5. 新建要保存的实体，而非直接相信请求对象。
    task.setTitle(request.getTitle().trim()); // 6. 去掉标题首尾空白。
    task.setCreatorId(currentUserId); // 7. 创建者永远是当前登录人。
    task.setExecutorId(request.getExecutorId()); // 8. 执行人来自已校验的请求字段。
    task.setPriority(request.getPriority() == null ? 2 : request.getPriority()); // 9. 未填写时默认中优先级。
    task.setStatus(STATUS_NOT_STARTED); // 10. 新任务固定从“未开始”起步。
    task.setDeadline(toLocalDateTime(request.getDeadline())); // 11. 把 API 时间转换成实体时间。
    taskMapper.insert(task); // 12. 写入 task 表，并由 MyBatis-Plus 回填主键。

    return buildTaskResponse(requireTask(task.getTaskId())); // 13. 再查一次，组装完整返回对象。
}
```

## 最容易忽略的规则：只能分给同部门的人

代码位置：`TaskServiceImpl.java:245-266`

```java
private void validateExecutorAssignment(Long creatorId, Long executorId) {
    User creator = requireCreatorUser(creatorId); // 1. 查出创建者；不存在就报错。
    User executor = requireExecutorUser(executorId); // 2. 查出执行人；不存在也报错。

    if (creator.getDepartmentId() == null) { // 3. 创建者没有部门，无法界定任务范围。
        throw new BusinessException(ErrorCode.BAD_REQUEST,
            "task creator must belong to a department before assigning tasks");
    }
    if (executor.getDepartmentId() == null) { // 4. 执行人没有部门，同样不允许分配。
        throw new BusinessException(ErrorCode.BAD_REQUEST,
            "executor must belong to the same department as the task creator");
    }
    if (!Objects.equals(creator.getDepartmentId(), executor.getDepartmentId())) {
        // 5. 两个部门 ID 不同，拒绝跨部门分配。
        throw new BusinessException(ErrorCode.BAD_REQUEST,
            "executor must belong to the same department as the task creator");
    }
}
```

所以“用户列表是否只显示同部门”与这里不同：本段代码只限制**任务分配**，不是在这里过滤用户列表。

## 谁能看、改、更新状态、删除？

| 动作 | 业务层真实判断 | 代码位置 |
| --- | --- | --- |
| 看详情 | 创建者、执行者或管理员 | `TaskServiceImpl.java:195-200` |
| 改标题/内容/执行人/优先级/截止时间 | 创建者或管理员 | `TaskServiceImpl.java:202-207` |
| 改状态 | 执行者或管理员 | `TaskServiceImpl.java:209-214` |
| 删除 | 创建者或管理员 | `TaskServiceImpl.java:176-182` |

管理员识别的是角色码 `ROLE_ADMIN` 或 `ROLE_SUPER_ADMIN`，见 `TaskServiceImpl.java:220-223`。

## 状态为什么首次“进行中”才写开始时间

代码位置：`TaskServiceImpl.java:154-174`

```java
int status = parseStatus(request.getStatus()); // 1. 把 todo/doing/done 等文本翻译成状态数字。
task.setStatus(status); // 2. 保存新的状态。
if (status == STATUS_IN_PROGRESS && task.getStartTime() == null) {
    // 3. 只有第一次进入“进行中”才记录开始时间。
    //    以后反复修改状态，不覆盖最初开始工作的时间。
    task.setStartTime(LocalDateTime.now());
}
taskMapper.updateById(task); // 4. 使用主键更新这条任务。
```

可接受状态是“未开始/todo、进行中/doing、已完成/done、已取消/cancelled”，对应解析逻辑在 `TaskServiceImpl.java:294-308`。

## 列表为什么看不到无关任务

代码位置：`TaskServiceImpl.java:81-113`

列表查询先固定加上 `(creator_id = 当前用户 OR executor_id = 当前用户)`。这意味着即使拥有 `task:list` 功能权限，普通用户的列表也只会出现自己创建或自己执行的任务。随后才叠加关键字、状态、优先级、执行人等筛选，并通过 `selectPage` 做分页。

## 常见误解

| 误解 | 实际情况 |
| --- | --- |
| 有 `task:update` 就能改任意任务 | 不对，还必须是创建者或管理员。 |
| 执行人能修改任务全部内容 | 不对，执行人主要能更新状态。 |
| 任务能跨部门指派 | 当前代码明确禁止。 |
| 创建任务时前端可指定创建者 | 不行，后端从 `AuthContext` 取当前登录用户。 |
| 状态是前端自己决定的 | 后端会解析、校验并最终写库。 |

## 人话复盘

任务管理的顺序是：先认人，再验功能权限，再验“这条任务与你有没有关系”，最后才写数据库。创建任务时还加了同部门限制；状态则交给执行人或管理员推进。这样的分层能同时防止越权和错误数据。
