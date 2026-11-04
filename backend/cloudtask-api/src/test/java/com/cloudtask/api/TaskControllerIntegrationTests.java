
package com.cloudtask.api;

import com.cloudtask.api.config.SecurityConfig;
import com.cloudtask.api.controller.TaskController;
import com.cloudtask.api.dto.TaskResponse;
import com.cloudtask.api.security.JwtAuthenticationFilter;
import com.cloudtask.api.service.CustomUserDetailsService;
import com.cloudtask.api.service.JwtService;
import com.cloudtask.api.service.TaskService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TaskController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        AdminAuthorizationTestController.class
})
class TaskControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaskService taskService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private final String testEmail = "test@cloudtask.com";

    // TASK API TESTS

    @Test
    void createTaskShouldReturnCreated() throws Exception {

        UUID taskId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        TaskResponse response = new TaskResponse(
                taskId,
                "Build authentication API",
                "Implement JWT authentication",
                "TODO",
                "MEDIUM",
                LocalDate.now().plusDays(7),
                userId,
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(taskService.createTask(any(), eq(testEmail)))
                .thenReturn(response);

        mockMvc.perform(post("/api/tasks")
                        .with(user(testEmail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Build authentication API",
                                  "description": "Implement JWT authentication",
                                  "dueDate": "2026-11-10"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Build authentication API"))
                .andExpect(jsonPath("$.status").value("TODO"));
    }

    @Test
    void getMyTasksShouldReturnTaskList() throws Exception {

        UUID taskId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        TaskResponse response = new TaskResponse(
                taskId,
                "Build authentication API",
                "Implement JWT authentication",
                "TODO",
                "MEDIUM",
                null,
                userId,
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(taskService.getMyTasks(testEmail, null, null, null))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/tasks")
                        .with(user(testEmail)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title")
                        .value("Build authentication API"));
    }

    @Test
    void updateTaskStatusShouldReturnUpdatedTask() throws Exception {

        UUID taskId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        TaskResponse response = new TaskResponse(
                taskId,
                "Build authentication API",
                "Implement JWT authentication",
                "DONE",
                "MEDIUM",
                null,
                userId,
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(taskService.updateTaskStatus(eq(taskId), eq("DONE"), eq(testEmail)))
                .thenReturn(response);

        mockMvc.perform(patch("/api/tasks/" + taskId + "/status")
                        .with(user(testEmail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "DONE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"));
    }

    @Test
    void createTaskShouldRejectInvalidRequest() throws Exception {

        mockMvc.perform(post("/api/tasks")
                        .with(user(testEmail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors.title").exists());

        verify(taskService, never()).createTask(any(), anyString());
    }

    @Test
    void getMyTasksShouldRejectUnauthenticatedRequest() throws Exception {

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message")
                        .value("Authentication required"));

        verify(taskService, never()).getMyTasks(anyString());
    }

    // ROLE-BASED ACCESS CONTROL TESTS

    @Test
    void userShouldBeForbiddenFromAdminEndpoints() throws Exception {

        mockMvc.perform(get("/api/admin/test")
                        .with(user(testEmail).roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void managerShouldBeForbiddenFromAdminEndpoints() throws Exception {

        mockMvc.perform(get("/api/admin/test")
                        .with(user(testEmail).roles("MANAGER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminShouldPassAdminAuthorization() throws Exception {

        mockMvc.perform(get("/api/admin/test")
                        .with(user(testEmail).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string("Admin access granted"));
    }

    @Test
    void userShouldBeForbiddenFromManagerEndpoints() throws Exception {

        mockMvc.perform(get("/api/manager/test")
                        .with(user(testEmail).roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void managerShouldPassManagerAuthorization() throws Exception {

        mockMvc.perform(get("/api/manager/test")
                        .with(user(testEmail).roles("MANAGER")))
                .andExpect(status().isOk())
                .andExpect(content().string("Manager access granted"));
    }

    @Test
    void adminShouldAlsoPassManagerAuthorization() throws Exception {

        mockMvc.perform(get("/api/manager/test")
                        .with(user(testEmail).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string("Manager access granted"));
    }

    @Test
    void unauthenticatedUserShouldNotAccessAdminEndpoints() throws Exception {

        mockMvc.perform(get("/api/admin/test"))
                .andExpect(status().isUnauthorized());
    }
}
