
package com.cloudtask.api.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record TaskResponse(
        UUID id,
        String title,
        String description,
        String status,
        String priority,
        LocalDate dueDate,
        UUID createdBy,
        UUID assignedTo,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
