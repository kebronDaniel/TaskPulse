package com.prep.taskpulse.outbox;

import java.util.UUID;

public record OutboxMessage(UUID eventId, String partitionKey, String payload) {
}
