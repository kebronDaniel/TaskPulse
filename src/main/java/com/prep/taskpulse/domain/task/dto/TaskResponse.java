package com.prep.taskpulse.domain.task.dto;

import com.prep.taskpulse.domain.task.enums.TaskPriority;
import com.prep.taskpulse.domain.task.enums.TaskStatus;

import java.time.Instant;
import java.util.UUID;

public record TaskResponse(
        UUID id,
        String title,
        String description,
        TaskStatus taskStatus,
        TaskPriority taskPriority,
        Instant createdAt,
        Instant updatedAt
) {
}
