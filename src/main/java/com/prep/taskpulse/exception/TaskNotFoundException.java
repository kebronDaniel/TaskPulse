package com.prep.taskpulse.exception;

import java.util.UUID;

public class TaskNotFoundException extends ResourceNotFoundException{
    public TaskNotFoundException(UUID id) {
        super("Task not found: " + id);
    }
}
