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
        requireCurrentUserId(currentUserId);
        validateCreateRequest(request);
        validateExecutor(request.getExecutorId());

        Task task = new Task();
        task.setTitle(request.getTitle().trim());
        task.setContent(trimToNull(request.getContent()));
        task.setCreatorId(currentUserId);
        task.setExecutorId(request.getExecutorId());
        task.setPriority(request.getPriority() == null ? 2 : request.getPriority());
        task.setStatus(STATUS_NOT_STARTED);
        task.setDeadline(toLocalDateTime(request.getDeadline()));
        taskMapper.insert(task);

        return buildTaskResponse(requireTask(task.getTaskId()));
    }

    @Override
    public PageResponse<TaskDto.TaskResponse> listTasks(Long currentUserId, TaskDto.TaskQueryRequest request) {
        requireCurrentUserId(currentUserId);
        TaskDto.TaskQueryRequest query = request == null ? new TaskDto.TaskQueryRequest() : request;
        Integer status = parseOptionalStatus(query.getStatus());
        validatePriority(query.getPriority());

        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<Task>()
            .and(participant -> participant
                .eq(Task::getCreatorId, currentUserId)
                .or()
                .eq(Task::getExecutorId, currentUserId))
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
            validateExecutor(request.getExecutorId());
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
        requireExecutorOrAdministrator(currentUserId, task);
        int status = parseStatus(request.getStatus());
        task.setStatus(status);
        if (status == STATUS_IN_PROGRESS && task.getStartTime() == null) {
            task.setStartTime(LocalDateTime.now());
        }
        taskMapper.updateById(task);
        return buildTaskResponse(requireTask(taskId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTask(Long currentUserId, Long taskId) {
        Task task = requireTask(taskId);
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

    private void validateExecutor(Long executorId) {
        if (executorId != null && userMapper.selectById(executorId) == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "executor user does not exist");
        }
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
            case "未开始" -> STATUS_NOT_STARTED;
            case "进行中" -> STATUS_IN_PROGRESS;
            case "已完成" -> STATUS_COMPLETED;
            default -> throw new BusinessException(
                ErrorCode.BAD_REQUEST,
                "status must be 未开始, 进行中 or 已完成"
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
            .avatar(user.getAvatar())
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
