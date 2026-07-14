package com.prep.taskpulse.domain.task.dto;

import com.prep.taskpulse.domain.task.enums.TaskPriority;
import com.prep.taskpulse.domain.task.enums.TaskStatus;

import java.time.Instant;
import java.util.UUID;

public record TaskSearchCriteria(
        TaskPriority priority,
        TaskStatus status,
        UUID assigneeId,
        Instant beforeDue,
        Instant createdAfter) {}
