package com.prep.taskpulse.domain.task.dto;

import com.prep.taskpulse.domain.task.enums.TaskPriority;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CreateTaskRequest(
        @NotBlank
        @Size(max = 250)
        String title,
        String description,
        TaskPriority priority,
        @FutureOrPresent
        Instant dueDate) {
}
