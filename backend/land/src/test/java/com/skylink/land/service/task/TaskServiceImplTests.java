package com.skylink.land.service.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.skylink.land.dto.task.TaskDto;
import com.skylink.land.entity.identity.User;
import com.skylink.land.entity.task.Task;
import com.skylink.land.exception.BusinessException;
import com.skylink.land.exception.ErrorCode;
import com.skylink.land.mapper.identity.UserMapper;
import com.skylink.land.mapper.task.TaskMapper;
import com.skylink.land.service.identity.UserService;
import com.skylink.land.service.task.impl.TaskServiceImpl;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskServiceImplTests {

    @Mock TaskMapper taskMapper;
    @Mock UserMapper userMapper;
    @Mock UserService userService;

    private TaskServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TaskServiceImpl(taskMapper, userMapper, userService);
    }

    @Test
    void createTaskRequiresExecutorId() {
        TaskDto.CreateTaskRequest request = new TaskDto.CreateTaskRequest();
        request.setTitle("部门任务");

        assertThatThrownBy(() -> service.createTask(1L, request))
            .isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST))
            .hasMessageContaining("executorId is required");

        verifyNoInteractions(taskMapper);
    }

    @Test
    void createTaskRejectsExecutorFromDifferentDepartment() {
        TaskDto.CreateTaskRequest request = new TaskDto.CreateTaskRequest();
        request.setTitle("部门任务");
        request.setExecutorId(2L);

        when(userMapper.selectById(1L)).thenReturn(user(1L, 101L));
        when(userMapper.selectById(2L)).thenReturn(user(2L, 202L));

        assertThatThrownBy(() -> service.createTask(1L, request))
            .isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST))
            .hasMessageContaining("same department");

        verifyNoInteractions(taskMapper);
    }

    @Test
    void createTaskAcceptsExecutorFromSameDepartment() {
        TaskDto.CreateTaskRequest request = new TaskDto.CreateTaskRequest();
        request.setTitle("部门任务");
        request.setExecutorId(2L);
        request.setPriority(2);

        when(userMapper.selectById(1L)).thenReturn(user(1L, 101L));
        when(userMapper.selectById(2L)).thenReturn(user(2L, 101L));

        AtomicReference<Task> insertedTask = new AtomicReference<>();
        doAnswer(invocation -> {
            Task task = invocation.getArgument(0);
            task.setTaskId(88L);
            insertedTask.set(task);
            return 1;
        }).when(taskMapper).insert(any(Task.class));
        when(taskMapper.selectById(88L)).thenAnswer(invocation -> insertedTask.get());

        TaskDto.TaskResponse response = service.createTask(1L, request);

        assertThat(response.getTaskId()).isEqualTo(88L);
        assertThat(response.getExecutor().getUserId()).isEqualTo(2L);
        verify(taskMapper).insert(any(Task.class));
    }

    @Test
    void updateTaskRejectsExecutorOutsideCreatorDepartment() {
        Task task = new Task();
        task.setTaskId(99L);
        task.setCreatorId(1L);
        task.setExecutorId(2L);
        task.setTitle("部门任务");

        TaskDto.UpdateTaskRequest request = new TaskDto.UpdateTaskRequest();
        request.setExecutorId(3L);

        when(taskMapper.selectById(99L)).thenReturn(task);
        when(userService.listRoleCodes(9L)).thenReturn(List.of("ROLE_ADMIN"));
        when(userMapper.selectById(1L)).thenReturn(user(1L, 101L));
        when(userMapper.selectById(3L)).thenReturn(user(3L, 202L));

        assertThatThrownBy(() -> service.updateTask(9L, 99L, request))
            .isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST))
            .hasMessageContaining("same department");
    }

    private User user(Long userId, Long departmentId) {
        User user = new User();
        user.setUserId(userId);
        user.setUsername("user" + userId);
        user.setDepartmentId(departmentId);
        return user;
    }
}
