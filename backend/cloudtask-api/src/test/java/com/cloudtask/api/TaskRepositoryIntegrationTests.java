
package com.cloudtask.api;

import com.cloudtask.api.entity.Task;
import com.cloudtask.api.entity.User;
import com.cloudtask.api.entity.UserRole;
import com.cloudtask.api.repository.TaskRepository;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class TaskRepositoryIntegrationTests {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void userShouldSeeTasksCreatedByOrAssignedToThem() {

        User user = persistUser("user@test.com", UserRole.USER);
        User anotherUser = persistUser("another@test.com", UserRole.USER);

        Task createdTask = persistTask("Created by user", user, null, "TODO", "MEDIUM");
        Task assignedTask = persistTask("Assigned to user", anotherUser, user, "IN_PROGRESS", "HIGH");

        persistTask("Unrelated task", anotherUser, null, "TODO", "LOW");

        flushAndClear();

        List<Task> results = taskRepository.findTasksWithFilters(
                user.getId(), false, null, null, null,
                PageRequest.of(0, 10)
        ).getContent();

        Set<UUID> resultIds = results.stream()
                .map(Task::getId)
                .collect(Collectors.toSet());

        assertEquals(2, results.size());
        assertTrue(resultIds.contains(createdTask.getId()));
        assertTrue(resultIds.contains(assignedTask.getId()));
    }

    @Test
    void adminShouldSeeAllTasksIncludingUnassignedTasks() {

        User admin = persistUser("admin@test.com", UserRole.ADMIN);
        User user = persistUser("user@test.com", UserRole.USER);

        Task unassignedTask = persistTask("Unassigned task", user, null, "TODO", "MEDIUM");
        Task assignedTask = persistTask("Assigned task", user, admin, "IN_PROGRESS", "HIGH");

        flushAndClear();

        List<Task> results = taskRepository.findTasksWithFilters(
                admin.getId(), true, null, null, null,
                PageRequest.of(0, 10)
        ).getContent();

        Set<UUID> resultIds = results.stream()
                .map(Task::getId)
                .collect(Collectors.toSet());

        assertEquals(2, results.size());
        assertTrue(resultIds.contains(unassignedTask.getId()));
        assertTrue(resultIds.contains(assignedTask.getId()));
    }

    @Test
    void repositoryShouldFilterTasksByStatus() {

        User user = persistUser("user@test.com", UserRole.USER);

        persistTask("TODO task", user, null, "TODO", "MEDIUM");
        persistTask("Completed task", user, null, "DONE", "MEDIUM");

        flushAndClear();

        List<Task> results = taskRepository.findTasksWithFilters(
                user.getId(), false, "TODO", null, null,
                PageRequest.of(0, 10)
        ).getContent();

        assertEquals(1, results.size());
        assertEquals("TODO", results.get(0).getStatus());
    }

    @Test
    void repositoryShouldFilterByPriorityAndAssignee() {

        User user = persistUser("user@test.com", UserRole.USER);
        User assignee = persistUser("assignee@test.com", UserRole.USER);
        User anotherAssignee = persistUser("another-assignee@test.com", UserRole.USER);

        persistTask("Matching task", user, assignee, "TODO", "HIGH");
        persistTask("Wrong priority", user, assignee, "TODO", "LOW");
        persistTask("Wrong assignee", user, anotherAssignee, "TODO", "HIGH");

        flushAndClear();

        List<Task> results = taskRepository.findTasksWithFilters(
                user.getId(), false, "TODO", "HIGH", assignee.getId(),
                PageRequest.of(0, 10)
        ).getContent();

        assertEquals(1, results.size());
        assertEquals("Matching task", results.get(0).getTitle());
    }

    @Test
    void repositoryShouldReturnCorrectPaginationMetadata() {

        User user = persistUser("pagination@test.com", UserRole.USER);

        for (int i = 1; i <= 12; i++) {
            persistTask("Task " + i, user, null, "TODO", "MEDIUM");
        }

        flushAndClear();

        Page<Task> firstPage = taskRepository.findTasksWithFilters(
                user.getId(), false, null, null, null,
                PageRequest.of(0, 5)
        );

        Page<Task> secondPage = taskRepository.findTasksWithFilters(
                user.getId(), false, null, null, null,
                PageRequest.of(1, 5)
        );

        assertEquals(5, firstPage.getContent().size());
        assertEquals(5, secondPage.getContent().size());

        assertEquals(12, firstPage.getTotalElements());
        assertEquals(3, firstPage.getTotalPages());

        assertEquals(0, firstPage.getNumber());
        assertEquals(1, secondPage.getNumber());
        assertEquals(5, secondPage.getSize());
    }

    @Test
    void repositoryShouldSortTasksByTitleAscending() {

        User user = persistUser("sorting@test.com", UserRole.USER);

        persistTask("Charlie", user, null, "TODO", "MEDIUM");
        persistTask("Alpha", user, null, "TODO", "MEDIUM");
        persistTask("Bravo", user, null, "TODO", "MEDIUM");

        flushAndClear();

        Page<Task> results = taskRepository.findTasksWithFilters(
                user.getId(), false, null, null, null,
                PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "title"))
        );

        List<String> titles = results.getContent().stream()
                .map(Task::getTitle)
                .toList();

        assertEquals(List.of("Alpha", "Bravo", "Charlie"), titles);
    }

    private User persistUser(String email, UserRole role) {

        User user = new User(
                "Test User",
                email,
                "test-password-hash",
                role
        );

        entityManager.persist(user);
        return user;
    }

    private Task persistTask(
            String title,
            User creator,
            User assignee,
            String status,
            String priority
    ) {

        Task task = new Task();

        task.setTitle(title);
        task.setDescription("Integration test task");
        task.setCreatedBy(creator);
        task.setAssignedTo(assignee);
        task.setStatus(status);
        task.setPriority(priority);

        entityManager.persist(task);
        return task;
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
