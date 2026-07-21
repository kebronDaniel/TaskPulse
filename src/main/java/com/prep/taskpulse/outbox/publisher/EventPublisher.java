package com.prep.taskpulse.outbox.publisher;

import java.util.concurrent.CompletableFuture;

public interface EventPublisher {
    CompletableFuture<Void> publish(String key, String payload);
}
