
package com.cloudtask.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record CreateTaskRequest(

        @NotBlank(message = "Task title is required")
        @Size(max = 200, message = "Title cannot exceed 200 characters")
        String title,

        String description,

        LocalDate dueDate,

        UUID assignedTo
) {
}
