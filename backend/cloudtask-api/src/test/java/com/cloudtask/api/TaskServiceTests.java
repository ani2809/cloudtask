
package com.cloudtask.api;

import com.cloudtask.api.dto.CreateTaskRequest;
import com.cloudtask.api.dto.TaskResponse;
import com.cloudtask.api.entity.Task;
import com.cloudtask.api.entity.User;
import com.cloudtask.api.repository.TaskRepository;
import com.cloudtask.api.repository.UserRepository;
import com.cloudtask.api.service.TaskService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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

        when(testUser.getId()).thenReturn(testUserId);
    }

    @Test
    void createTaskShouldSaveTaskSuccessfully() {

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
                request,
                "test@cloudtask.com"
        );

        assertNotNull(response);
        assertEquals("Build authentication API", response.title());
        assertEquals("Implement JWT authentication", response.description());
        assertEquals("TODO", response.status());
        assertEquals("MEDIUM", response.priority());
        assertEquals(testUserId, response.createdBy());

        verify(taskRepository, times(1)).save(any(Task.class));
    }

    @Test
    void getMyTasksShouldReturnTasksCreatedByUser() {

        Task task = createTestTask();

        when(userRepository.findByEmail("test@cloudtask.com"))
                .thenReturn(Optional.of(testUser));

        when(taskRepository.findTasksWithFilters(
                testUserId, null, null, null))
                .thenReturn(List.of(task));

        List<TaskResponse> responses =
                taskService.getMyTasks(
                        "test@cloudtask.com",
                        null,
                        null,
                        null
                );

        assertEquals(1, responses.size());
        assertEquals("Build task API", responses.get(0).title());
        assertEquals("TODO", responses.get(0).status());
        assertEquals(testUserId, responses.get(0).createdBy());

        verify(taskRepository).findTasksWithFilters(
                testUserId, null, null, null);
    }

    @Test
    void getMyTasksShouldFilterByStatus() {

        Task task = createTestTask();

        when(userRepository.findByEmail("test@cloudtask.com"))
                .thenReturn(Optional.of(testUser));

        when(taskRepository.findTasksWithFilters(
                testUserId, "TODO", null, null))
                .thenReturn(List.of(task));

        List<TaskResponse> responses =
                taskService.getMyTasks(
                        "test@cloudtask.com",
                        "TODO",
                        null,
                        null
                );

        assertEquals(1, responses.size());
        assertEquals("TODO", responses.get(0).status());

        verify(taskRepository).findTasksWithFilters(
                testUserId, "TODO", null, null);
    }

    @Test
    void getMyTasksShouldFilterByPriority() {

        Task task = createTestTask();
        task.setPriority("HIGH");

        when(userRepository.findByEmail("test@cloudtask.com"))
                .thenReturn(Optional.of(testUser));

        when(taskRepository.findTasksWithFilters(
                testUserId, null, "HIGH", null))
                .thenReturn(List.of(task));

        List<TaskResponse> responses =
                taskService.getMyTasks(
                        "test@cloudtask.com",
                        null,
                        "HIGH",
                        null
                );

        assertEquals(1, responses.size());
        assertEquals("HIGH", responses.get(0).priority());

        verify(taskRepository).findTasksWithFilters(
                testUserId, null, "HIGH", null);
    }

    @Test
    void getMyTasksShouldFilterByAssignee() {

        UUID assigneeId = UUID.randomUUID();

        User assignee = mock(User.class);
        when(assignee.getId()).thenReturn(assigneeId);

        Task task = createTestTask();
        task.setAssignedTo(assignee);

        when(userRepository.findByEmail("test@cloudtask.com"))
                .thenReturn(Optional.of(testUser));

        when(taskRepository.findTasksWithFilters(
                testUserId, null, null, assigneeId))
                .thenReturn(List.of(task));

        List<TaskResponse> responses =
                taskService.getMyTasks(
                        "test@cloudtask.com",
                        null,
                        null,
                        assigneeId
                );

        assertEquals(1, responses.size());
        assertEquals(assigneeId, responses.get(0).assignedTo());

        verify(taskRepository).findTasksWithFilters(
                testUserId, null, null, assigneeId);
    }

    @Test
    void getMyTasksShouldSupportCombinedFilters() {

        UUID assigneeId = UUID.randomUUID();

        User assignee = mock(User.class);
        when(assignee.getId()).thenReturn(assigneeId);

        Task task = createTestTask();
        task.setPriority("HIGH");
        task.setAssignedTo(assignee);

        when(userRepository.findByEmail("test@cloudtask.com"))
                .thenReturn(Optional.of(testUser));

        when(taskRepository.findTasksWithFilters(
                testUserId, "TODO", "HIGH", assigneeId))
                .thenReturn(List.of(task));

        List<TaskResponse> responses =
                taskService.getMyTasks(
                        "test@cloudtask.com",
                        "TODO",
                        "HIGH",
                        assigneeId
                );

        assertEquals(1, responses.size());
        assertEquals("TODO", responses.get(0).status());
        assertEquals("HIGH", responses.get(0).priority());
        assertEquals(assigneeId, responses.get(0).assignedTo());

        verify(taskRepository).findTasksWithFilters(
                testUserId, "TODO", "HIGH", assigneeId);
    }

    @Test
    void updateTaskStatusShouldRejectAnotherUsersTask() {

        UUID anotherUserId = UUID.randomUUID();

        User anotherUser = mock(User.class);
        when(anotherUser.getId()).thenReturn(anotherUserId);

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
                        task.getId(),
                        "DONE",
                        "test@cloudtask.com"
                )
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());

        verify(taskRepository, never()).save(any(Task.class));
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
