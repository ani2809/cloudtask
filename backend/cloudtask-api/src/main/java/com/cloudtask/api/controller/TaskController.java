
package com.cloudtask.api.controller;

import com.cloudtask.api.dto.CreateTaskRequest;
import com.cloudtask.api.dto.TaskResponse;
import com.cloudtask.api.dto.UpdateTaskStatusRequest;
import com.cloudtask.api.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
    public List<TaskResponse> getMyTasks(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) UUID assigneeId,
            Authentication authentication) {

        return taskService.getMyTasks(
                authentication.getName(),
                status,
                priority,
                assigneeId
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
