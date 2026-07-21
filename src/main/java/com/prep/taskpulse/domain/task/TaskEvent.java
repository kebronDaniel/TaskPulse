package com.prep.taskpulse.domain.task;

import com.prep.taskpulse.domain.task.enums.TaskEventType;

import java.time.Instant;
import java.util.UUID;

public record TaskEvent(UUID eventId, TaskEventType type,
                        UUID taskId, UUID projectId, UUID workspaceId, UUID assigneeId, Instant occurredAt) {
}
