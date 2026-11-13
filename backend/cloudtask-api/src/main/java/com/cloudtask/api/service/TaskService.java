
package com.cloudtask.api.service;

import com.cloudtask.api.dto.CreateTaskRequest;
import com.cloudtask.api.dto.TaskResponse;
import com.cloudtask.api.entity.Task;
import com.cloudtask.api.entity.User;
import com.cloudtask.api.entity.UserRole;
import com.cloudtask.api.repository.TaskRepository;
import com.cloudtask.api.repository.UserRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;
import java.util.UUID;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    private static final int MAX_PAGE_SIZE = 100;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "createdAt",
            "updatedAt",
            "dueDate",
            "title",
            "priority",
            "status"
    );

    public TaskService(
            TaskRepository taskRepository,
            UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public TaskResponse createTask(CreateTaskRequest request, String userEmail) {

        User creator = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));

        Task task = new Task();
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setCreatedBy(creator);

        if (request.assignedTo() != null) {
            User assignee = userRepository.findById(request.assignedTo())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Assigned user not found"));

            task.setAssignedTo(assignee);
        }

        if (request.dueDate() != null) {
            task.setDueDate(request.dueDate());
        }

        Task savedTask = taskRepository.save(task);

        return toResponse(savedTask);
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> getMyTasks(
            String userEmail,
            String status,
            String priority,
            UUID assigneeId,
            int page,
            int size,
            String sortBy,
            String sortDirection) {

        if (page < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page number cannot be negative");
        }

        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page size must be between 1 and " + MAX_PAGE_SIZE);
        }

        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid sort field: " + sortBy);
        }

        Sort.Direction direction;

        try {
            direction = Sort.Direction.fromString(sortDirection);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Sort direction must be ASC or DESC");
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));

        boolean isAdmin = user.getRole() == UserRole.ADMIN;

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(direction, sortBy)
        );

        return taskRepository.findTasksWithFilters(
                        user.getId(),
                        isAdmin,
                        status,
                        priority,
                        assigneeId,
                        pageable)
                .map(this::toResponse);
    }

    @Transactional
    public TaskResponse updateTaskStatus(
            UUID taskId,
            String newStatus,
            String userEmail) {

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Task not found"));

        boolean isAdmin = user.getRole() == UserRole.ADMIN;

        boolean isCreator =
                task.getCreatedBy().getId().equals(user.getId());

        boolean isAssignee =
                task.getAssignedTo() != null
                        && task.getAssignedTo().getId().equals(user.getId());

        if (!isAdmin && !isCreator && !isAssignee) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not authorized to update this task");
        }

        task.setStatus(newStatus);

        Task updatedTask = taskRepository.save(task);

        return toResponse(updatedTask);
    }

    @Transactional
public TaskResponse updateTaskPriority(
        UUID taskId,
        String newPriority,
        String userEmail) {

    User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "User not found"));

    Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Task not found"));

    boolean isAdmin = user.getRole() == UserRole.ADMIN;

    boolean isCreator =
            task.getCreatedBy().getId().equals(user.getId());

    boolean isAssignee =
            task.getAssignedTo() != null
                    && task.getAssignedTo().getId().equals(user.getId());

    if (!isAdmin && !isCreator && !isAssignee) {
        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "You are not authorized to update this task");
    }

    task.setPriority(newPriority);

    Task updatedTask = taskRepository.save(task);

    return toResponse(updatedTask);
}
    private TaskResponse toResponse(Task task) {

        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getDueDate(),
                task.getCreatedBy().getId(),
                task.getAssignedTo() != null
                        ? task.getAssignedTo().getId()
                        : null,
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
