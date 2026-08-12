package com.prep.taskpulse.domain.task.dto;

import com.prep.taskpulse.domain.task.enums.TaskPriority;
import java.time.Instant;
import java.util.UUID;

public record TaskSummaryResponse(
    UUID id,
    String title,
    TaskPriority priority,
    Instant dueDate,
    String projectName,
    String assigneeEmail) {}
