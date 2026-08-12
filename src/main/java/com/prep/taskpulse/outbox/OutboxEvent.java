package com.prep.taskpulse.outbox;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "outbox_events")
public class OutboxEvent {

  @Id private UUID id;

  @Column(name = "aggregate_type", nullable = false, length = 50)
  private String aggregateType;

  @Column(name = "aggregate_id", nullable = false)
  private UUID aggregateId;

  @Column(name = "event_type", nullable = false, length = 100)
  private String eventType;

  @Column(name = "partition_key", nullable = false)
  private String partitionKey;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false, columnDefinition = "jsonb")
  private String payload;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private OutboxStatus status;

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

  @Column(name = "published_at")
  private Instant publishedAt;

  @Column(name = "claimed_at")
  private Instant claimedAt;

  @Column(name = "attempt_count", nullable = false)
  private int attemptCount;

  @Column(name = "last_error")
  private String lastError;

  private OutboxEvent(
      UUID id,
      String aggregateType,
      UUID aggregateId,
      String eventType,
      String partitionKey,
      String payload,
      Instant occurredAt) {
    this.id = id;
    this.aggregateType = aggregateType;
    this.aggregateId = aggregateId;
    this.eventType = eventType;
    this.partitionKey = partitionKey;
    this.payload = payload;
    this.status = OutboxStatus.PENDING;
    this.occurredAt = occurredAt;
    this.attemptCount = 0;
  }

  public static OutboxEvent create(
      String aggregateType,
      UUID aggregateId,
      String eventType,
      String partitionKey,
      String payload,
      Instant occurredAt) {

    return new OutboxEvent(
        UUID.randomUUID(),
        aggregateType,
        aggregateId,
        eventType,
        partitionKey,
        payload,
        occurredAt);
  }

  public void markProcessing(Instant claimedAt) {
    if (this.status != OutboxStatus.PENDING)
      throw new IllegalStateException("Only PENDING processes can be claimed");
    this.status = OutboxStatus.PROCESSING;
    this.claimedAt = claimedAt;
  }

  public void markPublished(Instant publishedAt) {
    if (this.status != OutboxStatus.PROCESSING)
      throw new IllegalStateException("Only PROCESSING events can be PUBLISHED");
    this.status = OutboxStatus.PUBLISHED;
    this.publishedAt = publishedAt;
    this.lastError = null;
  }

  public void recordFailure(String error, int maxAttempts) {
    if (this.status != OutboxStatus.PROCESSING)
      throw new IllegalStateException("Only PROCESSING events can be recorded as FAILED.");
    this.attemptCount++;
    this.lastError = error;
    if (attemptCount >= maxAttempts) this.status = OutboxStatus.FAILED;
    else this.status = OutboxStatus.PENDING;
    this.claimedAt = null;
  }

  public void releaseStaleClaim() {
    if (this.status != OutboxStatus.PROCESSING)
      throw new IllegalStateException("Only PROCESSING events can be released");
    this.status = OutboxStatus.PENDING;
  }

  public void retry() {
    this.status = OutboxStatus.PENDING;
  }

  public void defer(String error) {

    if (this.status != OutboxStatus.PROCESSING)
      throw new IllegalStateException("Only PROCESSING events can be deferred");

    this.status = OutboxStatus.PENDING;
    this.claimedAt = null;
    this.lastError = error;
  }
}
