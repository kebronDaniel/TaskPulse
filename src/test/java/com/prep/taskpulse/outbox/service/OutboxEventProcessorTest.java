package com.prep.taskpulse.outbox.service;

import static org.mockito.Mockito.*;

import com.prep.taskpulse.outbox.OutboxMessage;
import com.prep.taskpulse.outbox.publisher.EventPublisher;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OutboxEventProcessorTest {

  @Mock private OutboxStatusService outboxStatusService;

  @Mock private EventPublisher eventPublisher;

  @InjectMocks private OutboxEventProcessor eventProcessor;

  @Test
  void processEvent_whenKafkaIsUp_publishesEventAndMarksEventAsPublished() {
    OutboxMessage message =
        new OutboxMessage(
            UUID.fromString("123e4567-e89b-12d3-a456-426614174111"),
            "123e4567-e89b-12d3-a456-426614174222",
            "Payload");

    when(eventPublisher.publish(message.partitionKey(), message.payload()))
        .thenReturn(CompletableFuture.completedFuture(null));

    eventProcessor.process(message);

    verify(outboxStatusService).markPublished(message.eventId());
    verifyNoMoreInteractions(outboxStatusService);
  }

  @Test
  void process_whenCircuitIsOpen_defersEventWithoutRecordingFailure() {

    OutboxMessage message =
        new OutboxMessage(
            UUID.fromString("123e4567-e89b-12d3-a456-426614174111"),
            "123e4567-e89b-12d3-a456-426614174222",
            "Payload");

    CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("kafkaPublisher");
    CallNotPermittedException exception =
        CallNotPermittedException.createCallNotPermittedException(circuitBreaker);
    when(eventPublisher.publish(message.partitionKey(), message.payload()))
        .thenReturn(CompletableFuture.failedFuture(exception));

    eventProcessor.process(message);

    verify(outboxStatusService).defer(eq(message.eventId()), contains("\'kafkaPublisher\'"));
    verify(outboxStatusService, never()).recordFailure(any(), anyString());
    verifyNoMoreInteractions(outboxStatusService);
  }

  @Test
  void process_whenPublishFails_recordsFailureWithoutDeferring() {
    OutboxMessage message =
        new OutboxMessage(
            UUID.fromString("123e4567-e89b-12d3-a456-426614174111"),
            "123e4567-e89b-12d3-a456-426614174222",
            "Payload");
    RuntimeException exception = new RuntimeException("Connection timeout");
    when(eventPublisher.publish(message.partitionKey(), message.payload()))
        .thenReturn(CompletableFuture.failedFuture(exception));
    eventProcessor.process(message);

    // the matchers need to either do exact matching or use wild cards.
    verify(outboxStatusService)
        .recordFailure(eq(message.eventId()), contains("Connection timeout"));
    verify(outboxStatusService, never()).defer(eq(message.eventId()), anyString());
    verifyNoMoreInteractions(outboxStatusService);
  }
}
