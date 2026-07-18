package com.skylink.land.controller;

import com.skylink.land.auth.AuthContext;
import com.skylink.land.auth.RequirePermission;
import com.skylink.land.dto.common.ApiResponse;
import com.skylink.land.dto.common.PageResponse;
import com.skylink.land.dto.task.TaskDto;
import com.skylink.land.service.task.TaskService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    @RequirePermission("task:create")
    public TaskDto.TaskResponse createTask(@RequestBody TaskDto.CreateTaskRequest request) {
        // 不使用前端可能伪造的 creatorId，而是从已通过 JWT 认证的上下文取得当前用户。
        return taskService.createTask(AuthContext.requireUserId(), request);
    }

    @GetMapping
    @RequirePermission("task:list")
    public PageResponse<TaskDto.TaskResponse> listTasks(TaskDto.TaskQueryRequest request) {
        return taskService.listTasks(AuthContext.requireUserId(), request);
    }

    @GetMapping("/{taskId}")
    @RequirePermission("task:get")
    public TaskDto.TaskResponse getTask(@PathVariable Long taskId) {
        return taskService.getTask(AuthContext.requireUserId(), taskId);
    }

    @PutMapping("/{taskId}")
    @RequirePermission("task:update")
    public TaskDto.TaskResponse updateTask(
        @PathVariable Long taskId,
        @RequestBody TaskDto.UpdateTaskRequest request
    ) {
        return taskService.updateTask(AuthContext.requireUserId(), taskId, request);
    }

    @PutMapping("/{taskId}/status")
    @RequirePermission("task:status:update")
    public TaskDto.TaskResponse updateTaskStatus(
        @PathVariable Long taskId,
        @RequestBody TaskDto.UpdateTaskStatusRequest request
    ) {
        // “拥有状态修改功能”由注解检查；“是否是这条任务的执行人”由 Service 再检查。
        return taskService.updateTaskStatus(AuthContext.requireUserId(), taskId, request);
    }

    @DeleteMapping("/{taskId}")
    @RequirePermission("task:delete")
    public ApiResponse<Void> deleteTask(@PathVariable Long taskId) {
        // Service 会在删除前确认当前用户是创建者或系统管理员，Controller 不直接写数据库。
        taskService.deleteTask(AuthContext.requireUserId(), taskId);
        return ApiResponse.success("task deleted", null);
    }
}
