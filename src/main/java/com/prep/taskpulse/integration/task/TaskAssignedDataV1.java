package com.prep.taskpulse.integration.task;

import java.util.UUID;

public record TaskAssignedDataV1(
    UUID taskId, UUID projectId, UUID workspaceId, UUID assigneeId, String taskTitle) {
  public TaskAssignedDataV1 {
    if (taskId == null) {
      throw new IllegalArgumentException("taskId must not be null");
    }
    if (projectId == null) {
      throw new IllegalArgumentException("projectId must not be null");
    }
    if (workspaceId == null) {
      throw new IllegalArgumentException("workspaceId must not be null");
    }
    if (assigneeId == null) {
      throw new IllegalArgumentException("assigneeId must not be null");
    }
    if (taskTitle == null || taskTitle.isBlank()) {
      throw new IllegalArgumentException("taskTitle must not be blank");
    }
    if (taskTitle.length() > 255) {
      throw new IllegalArgumentException("taskTitle must not exceed 255 characters");
    }
  }
}
