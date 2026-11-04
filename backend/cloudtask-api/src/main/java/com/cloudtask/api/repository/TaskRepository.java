
package com.cloudtask.api.repository;

import com.cloudtask.api.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {

    List<Task> findAllByCreatedBy_Id(UUID userId);

    List<Task> findAllByAssignedTo_Id(UUID userId);

    @Query("""
            SELECT t FROM Task t
            WHERE t.createdBy.id = :userId
              AND (:status IS NULL OR t.status = :status)
              AND (:priority IS NULL OR t.priority = :priority)
              AND (:assigneeId IS NULL OR t.assignedTo.id = :assigneeId)
            """)
    List<Task> findTasksWithFilters(
            @Param("userId") UUID userId,
            @Param("status") String status,
            @Param("priority") String priority,
            @Param("assigneeId") UUID assigneeId
    );
}
