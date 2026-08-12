package com.prep.taskpulse.exception;

import java.util.UUID;

public class ProjectNotFoundException extends ResourceNotFoundException {
  public ProjectNotFoundException(UUID id) {
    super("Project not found: " + id);
  }
}
