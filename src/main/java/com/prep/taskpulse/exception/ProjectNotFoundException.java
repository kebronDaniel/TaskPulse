package com.prep.taskpulse.exception;

import java.util.UUID;

public class ProjectNotFoundException extends ResourceNotFoundException{
    public ProjectNotFoundException(UUID id) {
        super("Task not found: " + id);
    }
}
