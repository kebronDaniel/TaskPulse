package com.prep.taskpulse.outbox.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "taskflow.outbox")
public record OutboxProperties(int batchSize, int maxAttempts, Duration claimTimeout) {
    public OutboxProperties{
        if (batchSize <= 0) batchSize = 100;
        if (maxAttempts <= 0) maxAttempts = 5;
        if (claimTimeout == null) claimTimeout =Duration.ofMinutes(5);
    }
}
