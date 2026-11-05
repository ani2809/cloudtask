
package com.cloudtask.api.controller;

import com.cloudtask.api.dto.CreateTaskRequest;
import com.cloudtask.api.dto.TaskResponse;
import com.cloudtask.api.dto.UpdateTaskStatusRequest;
import com.cloudtask.api.service.TaskService;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse createTask(
            @Valid @RequestBody CreateTaskRequest request,
            Authentication authentication) {

        return taskService.createTask(
                request,
                authentication.getName()
        );
    }

    @GetMapping
    public Page<TaskResponse> getMyTasks(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) UUID assigneeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection,
            Authentication authentication) {

        return taskService.getMyTasks(
                authentication.getName(),
                status,
                priority,
                assigneeId,
                page,
                size,
                sortBy,
                sortDirection
        );
    }

    @PatchMapping("/{taskId}/status")
    public TaskResponse updateTaskStatus(
            @PathVariable UUID taskId,
            @Valid @RequestBody UpdateTaskStatusRequest request,
            Authentication authentication) {

        return taskService.updateTaskStatus(
                taskId,
                request.status(),
                authentication.getName()
        );
    }
}
