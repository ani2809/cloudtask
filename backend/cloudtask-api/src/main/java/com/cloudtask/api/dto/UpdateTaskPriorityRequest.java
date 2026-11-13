package com.cloudtask.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateTaskPriorityRequest(

        @NotBlank(message = "Priority is required")
        @Pattern(
                regexp = "HIGH|MEDIUM|LOW",
                message = "Priority must be HIGH, MEDIUM, or LOW"
        )
        String priority

) {
}
