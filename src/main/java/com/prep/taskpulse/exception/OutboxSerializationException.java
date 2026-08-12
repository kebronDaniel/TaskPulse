package com.prep.taskpulse.exception;

import java.util.UUID;

public class OutboxSerializationException extends RuntimeException {
  public OutboxSerializationException(UUID eventId, Throwable cause) {
    super("Failed to serialize :" + eventId, cause);
  }
}
