
package com.cloudtask.api.repository;

import com.cloudtask.api.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {

    List<Task> findAllByCreatedBy_Id(UUID userId);

    List<Task> findAllByAssignedTo_Id(UUID userId);
}
