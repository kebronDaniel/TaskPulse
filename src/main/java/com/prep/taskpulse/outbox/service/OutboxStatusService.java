package com.prep.taskpulse.outbox.service;

import com.prep.taskpulse.outbox.OutboxEvent;
import com.prep.taskpulse.outbox.OutboxStatus;
import com.prep.taskpulse.outbox.config.OutboxProperties;
import com.prep.taskpulse.outbox.repository.OutboxRepository;
import java.time.Clock;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OutboxStatusService {

  private final OutboxRepository outboxRepository;
  private final OutboxProperties outboxProperties;
  private final Clock clock;

  @Transactional
  public void markPublished(UUID eventId) {
    OutboxEvent event = getEvent(eventId);
    if (event.getStatus() != OutboxStatus.PROCESSING) return;
    event.markPublished(clock.instant());
  }

  @Transactional
  public void recordFailure(UUID eventId, String error) {
    OutboxEvent event = getEvent(eventId);

    if (event.getStatus() != OutboxStatus.PROCESSING) return;
    event.recordFailure(error, outboxProperties.maxAttempts());
  }

  @Transactional
  public void defer(UUID eventId, String error) {
    OutboxEvent event = getEvent(eventId);
    if (event.getStatus() != OutboxStatus.PROCESSING) return;
    event.defer(error);
  }

  private OutboxEvent getEvent(UUID eventId) {
    return outboxRepository
        .findById(eventId)
        .orElseThrow(() -> new IllegalArgumentException("Outbox event not found: " + eventId));
  }
}
