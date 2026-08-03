package com.prep.taskpulse.service;

import com.prep.taskpulse.outbox.OutboxEvent;
import com.prep.taskpulse.outbox.OutboxStatus;
import com.prep.taskpulse.outbox.config.OutboxProperties;
import com.prep.taskpulse.outbox.repository.OutboxRepository;
import com.prep.taskpulse.outbox.service.OutboxService;
import com.prep.taskpulse.outbox.service.OutboxStatusService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
public class OutboxStatusServiceTest {

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private OutboxProperties outboxProperties;

    @Mock
    private Clock clock;

    @InjectMocks
    private OutboxStatusService outboxStatusService;


    @Test
    void markPublished_whenEventIsProcessing_marksItPublished(){
        Instant claimedAt = Instant.parse("2026-08-03T10:00:00Z");
        Instant publishedAt = Instant.parse("2026-08-03T10:01:00Z");

        OutboxEvent event = OutboxEvent.create("Task", UUID.randomUUID(),"Task Created"
                ,UUID.randomUUID().toString(),"{\"taskId\":\"123\"}",Instant.parse("2026-08-03T10:00:00Z"));

        when(outboxRepository.findById(event.getId())).thenReturn(Optional.of(event));
        when(clock.instant()).thenReturn(publishedAt);

        event.markProcessing(claimedAt);
        outboxStatusService.markPublished(event.getId());

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(event.getPublishedAt()).isEqualTo(publishedAt);

        verify(outboxRepository).findById(event.getId());
        verify(clock).instant();
        verifyNoMoreInteractions(outboxRepository, clock);

    }

    @Test
    void recordFailure_whenAttemptsRemain_returnsEventToPending() {
        OutboxEvent event = OutboxEvent.create("Task", UUID.randomUUID(),"Task Created"
                ,UUID.randomUUID().toString(),"{\"taskId\":\"123\"}",Instant.parse("2026-08-03T10:00:00Z"));
        event.markProcessing(Instant.parse("2026-08-03T10:00:00Z"));

        when(outboxRepository.findById(event.getId())).thenReturn(Optional.of(event));
        when(outboxProperties.maxAttempts()).thenReturn(3);

        outboxStatusService.recordFailure(event.getId(), "Kafka acknowledgement timed out");

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(event.getAttemptCount()).isEqualTo(1);
        assertThat(event.getLastError()).isEqualTo("Kafka acknowledgement timed out");
        assertThat(event.getClaimedAt()).isNull();

        verify(outboxRepository).findById(event.getId());
        verify(outboxProperties).maxAttempts();
        verifyNoMoreInteractions(outboxRepository, outboxProperties);
    }


    @Test
    void recordFailure_whenMaxAttemptsReached_marksEventFailed() {
        OutboxEvent event = OutboxEvent.create("Task", UUID.randomUUID(),"Task Created"
                ,UUID.randomUUID().toString(),"{\"taskId\":\"123\"}",Instant.parse("2026-08-03T10:00:00Z"));

        event.markProcessing(Instant.parse("2026-08-03T10:00:00Z"));
        event.recordFailure("First failure", 2);

        event.markProcessing(Instant.parse("2026-08-03T10:01:00Z"));

        when(outboxRepository.findById(event.getId())).thenReturn(Optional.of(event));
        when(outboxProperties.maxAttempts()).thenReturn(2);

        outboxStatusService.recordFailure(event.getId(), "Second failure");

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(event.getAttemptCount()).isEqualTo(2);
        assertThat(event.getLastError()).isEqualTo("Second failure");
        assertThat(event.getClaimedAt()).isNull();

        verify(outboxRepository).findById(event.getId());
        verify(outboxProperties).maxAttempts();
    }

    @Test
    void defer_whenEventIsProcessing_returnsToPendingWithoutAttemptPenalty() {
        OutboxEvent event = OutboxEvent.create("Task", UUID.randomUUID(),"Task Created"
                ,UUID.randomUUID().toString(),"{\"taskId\":\"123\"}",Instant.parse("2026-08-03T10:00:00Z"));
        event.markProcessing(Instant.parse("2026-08-03T10:00:00Z"));

        when(outboxRepository.findById(event.getId())).thenReturn(Optional.of(event));

        outboxStatusService.defer(event.getId(),"Kafka circuit breaker is open");

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(event.getAttemptCount()).isZero();
        assertThat(event.getLastError())
                .isEqualTo("Kafka circuit breaker is open");
        assertThat(event.getClaimedAt()).isNull();

        verify(outboxRepository).findById(event.getId());
        verifyNoInteractions(outboxProperties, clock);
    }

    @Test
    void statusUpdates_whenEventIsNotProcessing_areIgnored() {
        OutboxEvent pendingEvent = OutboxEvent.create("Task", UUID.randomUUID(),"Task Created"
                ,UUID.randomUUID().toString(),"{\"taskId\":\"123\"}",Instant.parse("2026-08-03T10:00:00Z"));

        when(outboxRepository.findById(pendingEvent.getId()))
                .thenReturn(Optional.of(pendingEvent));

        outboxStatusService.markPublished(pendingEvent.getId());
        outboxStatusService.recordFailure(pendingEvent.getId(), "Late failure");
        outboxStatusService.defer(pendingEvent.getId(), "Late deferral");

        assertThat(pendingEvent.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(pendingEvent.getAttemptCount()).isZero();
        assertThat(pendingEvent.getPublishedAt()).isNull();
        assertThat(pendingEvent.getLastError()).isNull();

        verify(outboxRepository, times(3)).findById(pendingEvent.getId());
        verifyNoInteractions(clock, outboxProperties);
    }

    @Test
    void markPublished_whenEventDoesNotExist_throwsException() {
        UUID missingEventId = UUID.randomUUID();

        when(outboxRepository.findById(missingEventId)).thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> outboxStatusService.markPublished(missingEventId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Outbox event not found: " + missingEventId);

        verify(outboxRepository).findById(missingEventId);
        verifyNoInteractions(clock, outboxProperties);
    }
}
