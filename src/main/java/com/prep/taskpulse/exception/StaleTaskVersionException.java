package com.prep.taskpulse.exception;

import java.util.UUID;

public class StaleTaskVersionException extends RuntimeException {
    public StaleTaskVersionException(UUID taskId, Long expectedVersion, Long actualVersion) {
        super("Task %s was modified by another request. Expected version %d but found %d"
                .formatted(taskId, expectedVersion, actualVersion));
    }
}
