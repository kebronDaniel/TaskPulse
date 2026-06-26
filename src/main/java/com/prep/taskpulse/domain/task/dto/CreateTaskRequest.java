package com.prep.taskpulse.domain.task.dto;

import com.prep.taskpulse.domain.task.enums.TaskPriority;

import java.time.Instant;

public record CreateTaskRequest(String title, String description, TaskPriority priority, Instant dueDate) {
}
