
package com.cloudtask.api.repository;

import com.cloudtask.api.entity.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {

    @Query("""
            SELECT t FROM Task t
            LEFT JOIN t.assignedTo assignee
            WHERE (
                :isAdmin = true
                OR t.createdBy.id = :userId
                OR assignee.id = :userId
            )
            AND (:status IS NULL OR t.status = :status)
            AND (:priority IS NULL OR t.priority = :priority)
            AND (:assigneeId IS NULL OR assignee.id = :assigneeId)
            """)
    Page<Task> findTasksWithFilters(
            @Param("userId") UUID userId,
            @Param("isAdmin") boolean isAdmin,
            @Param("status") String status,
            @Param("priority") String priority,
            @Param("assigneeId") UUID assigneeId,
            Pageable pageable
    );
}
