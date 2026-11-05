
package com.cloudtask.api;

import com.cloudtask.api.dto.CreateTaskRequest;
import com.cloudtask.api.dto.TaskResponse;
import com.cloudtask.api.entity.Task;
import com.cloudtask.api.entity.User;
import com.cloudtask.api.entity.UserRole;
import com.cloudtask.api.repository.TaskRepository;
import com.cloudtask.api.repository.UserRepository;
import com.cloudtask.api.service.TaskService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTests {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TaskService taskService;

    private User testUser;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testUser = mock(User.class);
    }

    @Test
    void createTaskShouldSaveTaskSuccessfully() {

        when(testUser.getId()).thenReturn(testUserId);

        CreateTaskRequest request = new CreateTaskRequest(
                "Build authentication API",
                "Implement JWT authentication",
                LocalDate.now().plusDays(7),
                null
        );

        when(userRepository.findByEmail("test@cloudtask.com"))
                .thenReturn(Optional.of(testUser));

        when(taskRepository.save(any(Task.class)))
                .thenAnswer(invocation -> {
                    Task task = invocation.getArgument(0);
                    task.setId(UUID.randomUUID());
                    return task;
                });

        TaskResponse response = taskService.createTask(
                request, "test@cloudtask.com");

        assertNotNull(response);
        assertEquals("Build authentication API", response.title());
        assertEquals("Implement JWT authentication", response.description());
        assertEquals("TODO", response.status());
        assertEquals("MEDIUM", response.priority());
        assertEquals(testUserId, response.createdBy());

        verify(taskRepository).save(any(Task.class));
    }

    @Test
    void getMyTasksShouldReturnTasksCreatedByUser() {

        when(testUser.getId()).thenReturn(testUserId);
        when(testUser.getRole()).thenReturn(UserRole.USER);

        Task task = createTestTask();

        when(userRepository.findByEmail("test@cloudtask.com"))
                .thenReturn(Optional.of(testUser));

        when(taskRepository.findTasksWithFilters(
                eq(testUserId), eq(false), isNull(), isNull(), isNull(),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(task)));

        Page<TaskResponse> responses = getTasks(
                "test@cloudtask.com", null, null, null);

        assertEquals(1, responses.getTotalElements());
        assertEquals("Build task API", responses.getContent().get(0).title());
        assertEquals("TODO", responses.getContent().get(0).status());
        assertEquals(testUserId, responses.getContent().get(0).createdBy());
    }

    @Test
    void getMyTasksShouldFilterByStatus() {

        when(testUser.getId()).thenReturn(testUserId);
        when(testUser.getRole()).thenReturn(UserRole.USER);

        Task task = createTestTask();

        when(userRepository.findByEmail("test@cloudtask.com"))
                .thenReturn(Optional.of(testUser));

        when(taskRepository.findTasksWithFilters(
                eq(testUserId), eq(false), eq("TODO"), isNull(), isNull(),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(task)));

        Page<TaskResponse> responses = getTasks(
                "test@cloudtask.com", "TODO", null, null);

        assertEquals(1, responses.getTotalElements());
        assertEquals("TODO", responses.getContent().get(0).status());
    }

    @Test
    void getMyTasksShouldFilterByPriority() {

        when(testUser.getId()).thenReturn(testUserId);
        when(testUser.getRole()).thenReturn(UserRole.USER);

        Task task = createTestTask();
        task.setPriority("HIGH");

        when(userRepository.findByEmail("test@cloudtask.com"))
                .thenReturn(Optional.of(testUser));

        when(taskRepository.findTasksWithFilters(
                eq(testUserId), eq(false), isNull(), eq("HIGH"), isNull(),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(task)));

        Page<TaskResponse> responses = getTasks(
                "test@cloudtask.com", null, "HIGH", null);

        assertEquals("HIGH", responses.getContent().get(0).priority());
    }

    @Test
    void getMyTasksShouldFilterByAssignee() {

        when(testUser.getId()).thenReturn(testUserId);
        when(testUser.getRole()).thenReturn(UserRole.USER);

        UUID assigneeId = UUID.randomUUID();

        User assignee = mock(User.class);
        when(assignee.getId()).thenReturn(assigneeId);

        Task task = createTestTask();
        task.setAssignedTo(assignee);

        when(userRepository.findByEmail("test@cloudtask.com"))
                .thenReturn(Optional.of(testUser));

        when(taskRepository.findTasksWithFilters(
                eq(testUserId), eq(false), isNull(), isNull(), eq(assigneeId),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(task)));

        Page<TaskResponse> responses = getTasks(
                "test@cloudtask.com", null, null, assigneeId);

        assertEquals(assigneeId, responses.getContent().get(0).assignedTo());
    }

    @Test
    void getMyTasksShouldSupportCombinedFilters() {

        when(testUser.getId()).thenReturn(testUserId);
        when(testUser.getRole()).thenReturn(UserRole.USER);

        UUID assigneeId = UUID.randomUUID();

        User assignee = mock(User.class);
        when(assignee.getId()).thenReturn(assigneeId);

        Task task = createTestTask();
        task.setPriority("HIGH");
        task.setAssignedTo(assignee);

        when(userRepository.findByEmail("test@cloudtask.com"))
                .thenReturn(Optional.of(testUser));

        when(taskRepository.findTasksWithFilters(
                eq(testUserId), eq(false), eq("TODO"), eq("HIGH"),
                eq(assigneeId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(task)));

        Page<TaskResponse> responses = getTasks(
                "test@cloudtask.com", "TODO", "HIGH", assigneeId);

        assertEquals(1, responses.getTotalElements());
        assertEquals("TODO", responses.getContent().get(0).status());
        assertEquals("HIGH", responses.getContent().get(0).priority());
        assertEquals(assigneeId, responses.getContent().get(0).assignedTo());
    }

    @Test
    void adminShouldRetrieveAllTasks() {

        User admin = mock(User.class);
        UUID adminId = UUID.randomUUID();

        when(admin.getId()).thenReturn(adminId);
        when(admin.getRole()).thenReturn(UserRole.ADMIN);

        Task task = createTestTask();

        when(userRepository.findByEmail("admin@cloudtask.com"))
                .thenReturn(Optional.of(admin));

        when(taskRepository.findTasksWithFilters(
                eq(adminId), eq(true), isNull(), isNull(), isNull(),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(task)));

        Page<TaskResponse> responses = getTasks(
                "admin@cloudtask.com", null, null, null);

        assertEquals(1, responses.getTotalElements());
        verify(taskRepository).findTasksWithFilters(
                eq(adminId), eq(true), isNull(), isNull(), isNull(),
                any(Pageable.class));
    }

    @Test
    void userShouldBeAbleToUpdateAssignedTask() {

        when(testUser.getId()).thenReturn(testUserId);
        when(testUser.getRole()).thenReturn(UserRole.USER);

        Task task = new Task();
        task.setId(UUID.randomUUID());
        task.setTitle("Assigned task");

        User anotherCreator = mock(User.class);
        when(anotherCreator.getId()).thenReturn(UUID.randomUUID());

        task.setCreatedBy(anotherCreator);
        task.setAssignedTo(testUser);

        when(userRepository.findByEmail("test@cloudtask.com"))
                .thenReturn(Optional.of(testUser));

        when(taskRepository.findById(task.getId()))
                .thenReturn(Optional.of(task));

        when(taskRepository.save(any(Task.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse response = taskService.updateTaskStatus(
                task.getId(), "DONE", "test@cloudtask.com");

        assertEquals("DONE", response.status());
        verify(taskRepository).save(task);
    }

    @Test
    void adminShouldBeAbleToUpdateAnotherUsersTask() {

        User admin = mock(User.class);

        when(admin.getId()).thenReturn(UUID.randomUUID());
        when(admin.getRole()).thenReturn(UserRole.ADMIN);

        User anotherCreator = mock(User.class);
        when(anotherCreator.getId()).thenReturn(UUID.randomUUID());

        Task task = new Task();
        task.setId(UUID.randomUUID());
        task.setTitle("Private task");
        task.setCreatedBy(anotherCreator);

        when(userRepository.findByEmail("admin@cloudtask.com"))
                .thenReturn(Optional.of(admin));

        when(taskRepository.findById(task.getId()))
                .thenReturn(Optional.of(task));

        when(taskRepository.save(any(Task.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse response = taskService.updateTaskStatus(
                task.getId(), "DONE", "admin@cloudtask.com");

        assertEquals("DONE", response.status());
        verify(taskRepository).save(task);
    }

    @Test
    void updateTaskStatusShouldRejectAnotherUsersTask() {

        when(testUser.getId()).thenReturn(testUserId);
        when(testUser.getRole()).thenReturn(UserRole.USER);

        User anotherUser = mock(User.class);
        when(anotherUser.getId()).thenReturn(UUID.randomUUID());

        Task task = new Task();
        task.setId(UUID.randomUUID());
        task.setTitle("Private task");
        task.setCreatedBy(anotherUser);

        when(userRepository.findByEmail("test@cloudtask.com"))
                .thenReturn(Optional.of(testUser));

        when(taskRepository.findById(task.getId()))
                .thenReturn(Optional.of(task));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> taskService.updateTaskStatus(
                        task.getId(), "DONE", "test@cloudtask.com"));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void getMyTasksShouldSupportPagination() {

        when(testUser.getId()).thenReturn(testUserId);
        when(testUser.getRole()).thenReturn(UserRole.USER);

        when(userRepository.findByEmail("test@cloudtask.com"))
                .thenReturn(Optional.of(testUser));

        when(taskRepository.findTasksWithFilters(
                eq(testUserId), eq(false), isNull(), isNull(), isNull(),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(
                        List.of(createTestTask()),
                        PageRequest.of(1, 5),
                        12));

        Page<TaskResponse> result = taskService.getMyTasks(
                "test@cloudtask.com",
                null, null, null,
                1, 5, "createdAt", "DESC");

        assertEquals(1, result.getNumber());
        assertEquals(5, result.getSize());
        assertEquals(12, result.getTotalElements());
        assertEquals(3, result.getTotalPages());
    }

    @Test
    void getMyTasksShouldRejectNegativePage() {

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> taskService.getMyTasks(
                        "test@cloudtask.com",
                        null, null, null,
                        -1, 10, "createdAt", "DESC"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(userRepository, taskRepository);
    }

    @Test
    void getMyTasksShouldRejectInvalidPageSize() {

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> taskService.getMyTasks(
                        "test@cloudtask.com",
                        null, null, null,
                        0, 101, "createdAt", "DESC"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(userRepository, taskRepository);
    }

    @Test
    void getMyTasksShouldRejectInvalidSortField() {

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> taskService.getMyTasks(
                        "test@cloudtask.com",
                        null, null, null,
                        0, 10, "passwordHash", "DESC"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(userRepository, taskRepository);
    }

    @Test
    void getMyTasksShouldRejectInvalidSortDirection() {

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> taskService.getMyTasks(
                        "test@cloudtask.com",
                        null, null, null,
                        0, 10, "createdAt", "SIDEWAYS"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(userRepository, taskRepository);
    }

    private Page<TaskResponse> getTasks(
            String email,
            String status,
            String priority,
            UUID assigneeId) {

        return taskService.getMyTasks(
                email,
                status,
                priority,
                assigneeId,
                0,
                10,
                "createdAt",
                "DESC"
        );
    }

    private Task createTestTask() {

        Task task = new Task();

        task.setId(UUID.randomUUID());
        task.setTitle("Build task API");
        task.setDescription("Implement task endpoints");
        task.setStatus("TODO");
        task.setPriority("MEDIUM");
        task.setCreatedBy(testUser);

        return task;
    }
}
