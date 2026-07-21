package com.skylink.land.service.task.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.skylink.land.dto.common.PageResponse;
import com.skylink.land.dto.task.TaskDto;
import com.skylink.land.dto.user.UserDto;
import com.skylink.land.entity.identity.User;
import com.skylink.land.entity.task.Task;
import com.skylink.land.exception.BusinessException;
import com.skylink.land.exception.ErrorCode;
import com.skylink.land.mapper.identity.UserMapper;
import com.skylink.land.mapper.task.TaskMapper;
import com.skylink.land.service.identity.UserService;
import com.skylink.land.service.task.TaskService;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
public class TaskServiceImpl implements TaskService {

    private static final int PRIORITY_LOW = 1;

    private static final int PRIORITY_HIGH = 3;

    private static final int STATUS_NOT_STARTED = 1;

    private static final int STATUS_IN_PROGRESS = 2;

    private static final int STATUS_COMPLETED = 3;

    private static final String ROLE_SUPER_ADMIN = "ROLE_SUPER_ADMIN";

    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final TaskMapper taskMapper;

    private final UserMapper userMapper;

    private final UserService userService;

    public TaskServiceImpl(TaskMapper taskMapper, UserMapper userMapper, UserService userService) {
        this.taskMapper = taskMapper;
        this.userMapper = userMapper;
        this.userService = userService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskDto.TaskResponse createTask(Long currentUserId, TaskDto.CreateTaskRequest request) {
        // 创建任务牵涉校验和写库；任一步抛异常时，事务会撤销本次数据库变更。
        requireCurrentUserId(currentUserId);
        validateCreateRequest(request);
        // 当前实现只允许把任务分配给与创建者处于同一部门的已存在用户。
        validateExecutorAssignment(currentUserId, request.getExecutorId());

        Task task = new Task();
        task.setTitle(request.getTitle().trim());
        task.setContent(trimToNull(request.getContent()));
        // 创建者由登录态决定，避免客户端把任务伪装成“别人创建”。
        task.setCreatorId(currentUserId);
        task.setExecutorId(request.getExecutorId());
        task.setPriority(request.getPriority() == null ? 2 : request.getPriority());
        // 新任务统一从“未开始”起步，不能由前端直接指定任意初始状态。
        task.setStatus(STATUS_NOT_STARTED);
        task.setDeadline(toLocalDateTime(request.getDeadline()));
        taskMapper.insert(task);

        // 插入后重新查询并补齐创建者/执行者展示信息，而不是只回传刚才的半成品实体。
        return buildTaskResponse(requireTask(task.getTaskId()));
    }

    @Override
    public PageResponse<TaskDto.TaskResponse> listTasks(Long currentUserId, TaskDto.TaskQueryRequest request) {
        requireCurrentUserId(currentUserId);
        TaskDto.TaskQueryRequest query = request == null ? new TaskDto.TaskQueryRequest() : request;
        Integer status = parseOptionalStatus(query.getStatus());
        validatePriority(query.getPriority());
        Set<Long> matchedUserIds = findUserIdsByKeyword(query.getKeyword());

        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<Task>()
            // 即使用户有 task:list 功能权限，也只能在列表中看到自己创建或执行的任务。
            .and(participant -> participant
                .eq(Task::getCreatorId, currentUserId)
                .or()
                .eq(Task::getExecutorId, currentUserId))
            .and(StringUtils.hasText(query.getKeyword()), keyword -> {
                String value = query.getKeyword().trim();
                keyword.like(Task::getTitle, value)
                    .or()
                    .like(Task::getContent, value);
                if (!matchedUserIds.isEmpty()) {
                    keyword.or().in(Task::getExecutorId, matchedUserIds)
                        .or()
                        .in(Task::getCreatorId, matchedUserIds);
                }
            })
            .eq(status != null, Task::getStatus, status)
            .eq(query.getPriority() != null, Task::getPriority, query.getPriority())
            .eq(query.getExecutorId() != null, Task::getExecutorId, query.getExecutorId())
            .orderByDesc(Task::getCreateTime);

        Page<Task> page = taskMapper.selectPage(query.toMybatisPage(), wrapper);
        Map<Long, User> users = loadUsers(page.getRecords());
        return PageResponse.of(page.convert(task -> toTaskResponse(task, users)));
    }

    @Override
    public TaskDto.TaskResponse getTask(Long currentUserId, Long taskId) {
        Task task = requireTask(taskId);
        requireCanView(currentUserId, task);
        return buildTaskResponse(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskDto.TaskResponse updateTask(Long currentUserId, Long taskId, TaskDto.UpdateTaskRequest request) {
        Task task = requireTask(taskId);
        // 编辑任务内容属于“资源级权限”：创建者或管理员才能操作这条具体记录。
        requireCreatorOrAdministrator(currentUserId, task);
        if (request == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "request body is required");
        }

        if (request.getTitle() != null) {
            validateTitle(request.getTitle());
            task.setTitle(request.getTitle().trim());
        }
        if (request.getContent() != null) {
            task.setContent(trimToNull(request.getContent()));
        }
        if (request.getExecutorId() != null) {
            // 换执行人时也必须重新做同部门校验，不能只在创建时检查一次。
            validateExecutorAssignment(task.getCreatorId(), request.getExecutorId());
            task.setExecutorId(request.getExecutorId());
        }
        if (request.getPriority() != null) {
            validatePriority(request.getPriority());
            task.setPriority(request.getPriority());
        }
        if (request.getDeadline() != null) {
            task.setDeadline(toLocalDateTime(request.getDeadline()));
        }

        taskMapper.updateById(task);
        return buildTaskResponse(requireTask(taskId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskDto.TaskResponse updateTaskStatus(
        Long currentUserId,
        Long taskId,
        TaskDto.UpdateTaskStatusRequest request
    ) {
        if (request == null || !StringUtils.hasText(request.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "status is required");
        }

        Task task = requireTask(taskId);
        // 状态推进交给执行者；创建者本身不一定能代替执行者推进工作流。
        requireExecutorOrAdministrator(currentUserId, task);
        int status = parseStatus(request.getStatus());
        task.setStatus(status);
        if (status == STATUS_IN_PROGRESS && task.getStartTime() == null) {
            // 只记录第一次开始工作的时刻，之后反复切换状态不会覆盖原始开始时间。
            task.setStartTime(LocalDateTime.now());
        }
        taskMapper.updateById(task);
        return buildTaskResponse(requireTask(taskId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTask(Long currentUserId, Long taskId) {
        Task task = requireTask(taskId);
        // 删除是高风险动作，仍需资源级授权，不能只依赖 Controller 的功能权限注解。
        requireCreatorOrAdministrator(currentUserId, task);
        taskMapper.deleteById(taskId);
    }

    private Task requireTask(Long taskId) {
        if (taskId == null || taskId < 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "taskId is invalid");
        }
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "task not found");
        }
        return task;
    }

    private void requireCanView(Long currentUserId, Task task) {
        requireCurrentUserId(currentUserId);
        if (!isParticipant(currentUserId, task) && !isAdministrator(currentUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "only task participants or administrators can view the task");
        }
    }

    private void requireCreatorOrAdministrator(Long currentUserId, Task task) {
        requireCurrentUserId(currentUserId);
        if (!Objects.equals(task.getCreatorId(), currentUserId) && !isAdministrator(currentUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "only task creator or administrator can perform this operation");
        }
    }

    private void requireExecutorOrAdministrator(Long currentUserId, Task task) {
        requireCurrentUserId(currentUserId);
        if (!Objects.equals(task.getExecutorId(), currentUserId) && !isAdministrator(currentUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "only task executor or administrator can update task status");
        }
    }

    private boolean isParticipant(Long currentUserId, Task task) {
        return Objects.equals(task.getCreatorId(), currentUserId) || Objects.equals(task.getExecutorId(), currentUserId);
    }

    private boolean isAdministrator(Long userId) {
        List<String> roleCodes = userService.listRoleCodes(userId);
        return roleCodes != null && (roleCodes.contains(ROLE_SUPER_ADMIN) || roleCodes.contains(ROLE_ADMIN));
    }

    private void validateCreateRequest(TaskDto.CreateTaskRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "request body is required");
        }
        validateTitle(request.getTitle());
        if (request.getExecutorId() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "executorId is required");
        }
        validatePriority(request.getPriority());
    }

    private void validateTitle(String title) {
        if (!StringUtils.hasText(title)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "title is required");
        }
        if (title.trim().length() > 100) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "title must not exceed 100 characters");
        }
    }

    private void validateExecutorAssignment(Long creatorId, Long executorId) {
        User creator = requireCreatorUser(creatorId);
        User executor = requireExecutorUser(executorId);

        if (creator.getDepartmentId() == null) {
            // 无法确定创建者所属范围时，宁可拒绝分配，也不放开跨部门任务。
            throw new BusinessException(
                ErrorCode.BAD_REQUEST,
                "task creator must belong to a department before assigning tasks"
            );
        }
        if (executor.getDepartmentId() == null) {
            throw new BusinessException(
                ErrorCode.BAD_REQUEST,
                "executor must belong to the same department as the task creator"
            );
        }
        if (!Objects.equals(creator.getDepartmentId(), executor.getDepartmentId())) {
            // 部门 ID 是后端最终判断依据，前端展示的部门名称不能作为安全条件。
            throw new BusinessException(
                ErrorCode.BAD_REQUEST,
                "executor must belong to the same department as the task creator"
            );
        }
    }

    private User requireCreatorUser(Long creatorId) {
        User creator = userMapper.selectById(creatorId);
        if (creator == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "task creator not found");
        }
        return creator;
    }

    private User requireExecutorUser(Long executorId) {
        if (executorId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "executorId is required");
        }
        User executor = userMapper.selectById(executorId);
        if (executor == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "executor user does not exist");
        }
        return executor;
    }

    private void validatePriority(Integer priority) {
        if (priority != null && (priority < PRIORITY_LOW || priority > PRIORITY_HIGH)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "priority must be between 1 and 3");
        }
    }

    private Integer parseOptionalStatus(String status) {
        return StringUtils.hasText(status) ? parseStatus(status) : null;
    }

    private int parseStatus(String status) {
        return switch (status.trim()) {
            case "未开始", "todo" -> STATUS_NOT_STARTED;
            case "进行中", "doing" -> STATUS_IN_PROGRESS;
            case "已完成", "done" -> STATUS_COMPLETED;
            case "已取消", "cancelled" -> 4;
            default -> throw new BusinessException(
                ErrorCode.BAD_REQUEST,
                "status must be 未开始/todo, 进行中/doing, 已完成/done or 已取消/cancelled"
            );
        };
    }

    private String toStatusName(Integer status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case STATUS_NOT_STARTED -> "未开始";
            case STATUS_IN_PROGRESS -> "进行中";
            case STATUS_COMPLETED -> "已完成";
            case 4 -> "已取消";
            default -> "未知";
        };
    }

    private TaskDto.TaskResponse buildTaskResponse(Task task) {
        return toTaskResponse(task, loadUsers(List.of(task)));
    }

    private Map<Long, User> loadUsers(Collection<Task> tasks) {
        Set<Long> userIds = new LinkedHashSet<>();
        for (Task task : tasks) {
            if (task.getCreatorId() != null) {
                userIds.add(task.getCreatorId());
            }
            if (task.getExecutorId() != null) {
                userIds.add(task.getExecutorId());
            }
        }
        if (CollectionUtils.isEmpty(userIds)) {
            return Map.of();
        }
        List<User> users = userMapper.selectBatchIds(userIds);
        if (CollectionUtils.isEmpty(users)) {
            return Map.of();
        }
        return users.stream().collect(Collectors.toMap(User::getUserId, Function.identity()));
    }

    private Set<Long> findUserIdsByKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return Set.of();
        }

        String value = keyword.trim();
        return userMapper.selectList(
            new LambdaQueryWrapper<User>()
                .select(User::getUserId)
                .and(wrapper -> wrapper
                    .like(User::getUsername, value)
                    .or()
                    .like(User::getNickname, value))
        ).stream().map(User::getUserId).collect(Collectors.toSet());
    }

    private TaskDto.TaskResponse toTaskResponse(Task task, Map<Long, User> users) {
        return TaskDto.TaskResponse.builder()
            .taskId(task.getTaskId())
            .title(task.getTitle())
            .content(task.getContent())
            .status(toStatusName(task.getStatus()))
            .priority(task.getPriority())
            .deadline(task.getDeadline())
            .startTime(task.getStartTime())
            .createTime(task.getCreateTime())
            .updateTime(task.getUpdateTime())
            .creator(toUserSummary(users.get(task.getCreatorId())))
            .executor(toUserSummary(users.get(task.getExecutorId())))
            .build();
    }

    private UserDto.UserSummaryResponse toUserSummary(User user) {
        if (user == null) {
            return null;
        }
        return UserDto.UserSummaryResponse.builder()
            .userId(user.getUserId())
            .username(user.getUsername())
            .nickname(user.getNickname())
            .email(user.getEmail())
            .phone(user.getPhone())
            .status(user.getStatus())
            .departmentId(user.getDepartmentId())
            .createTime(user.getCreateTime())
            .build();
    }

    private LocalDateTime toLocalDateTime(OffsetDateTime value) {
        return value == null ? null : LocalDateTime.ofInstant(value.toInstant(), ZoneId.systemDefault());
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void requireCurrentUserId(Long currentUserId) {
        if (currentUserId == null || currentUserId < 1) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "current user is missing");
        }
    }
}
