package com.skylink.land.service.task;

import com.skylink.land.dto.common.PageResponse;
import com.skylink.land.dto.task.TaskDto;

public interface TaskService {

    TaskDto.TaskResponse createTask(Long currentUserId, TaskDto.CreateTaskRequest request);

    PageResponse<TaskDto.TaskResponse> listTasks(Long currentUserId, TaskDto.TaskQueryRequest request);

    TaskDto.TaskResponse getTask(Long currentUserId, Long taskId);

    TaskDto.TaskResponse updateTask(Long currentUserId, Long taskId, TaskDto.UpdateTaskRequest request);

    TaskDto.TaskResponse updateTaskStatus(
        Long currentUserId,
        Long taskId,
        TaskDto.UpdateTaskStatusRequest request
    );

    void deleteTask(Long currentUserId, Long taskId);
}
