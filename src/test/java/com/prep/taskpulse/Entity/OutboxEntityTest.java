package com.prep.taskpulse.Entity;

import com.prep.taskpulse.domain.task.enums.TaskEventType;
import com.prep.taskpulse.outbox.OutboxEvent;
import com.prep.taskpulse.outbox.OutboxStatus;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

class OutboxEntityTest {

    // check if state transitions are correct.

    @Test
    void recordFailure_whenMaximumAttemptsReached_marksEventAsFailed(){

        Instant occurredAt = Instant.parse("2026-07-21T10:00:00Z");
        Instant firstClaim = occurredAt.plusSeconds(2);
        OutboxEvent event = OutboxEvent.create("TASK", UUID.randomUUID(),
                TaskEventType.CREATED.name(),"981842c5-4377-4220-b330-ecd719f1d462",
                "payload",occurredAt);

        event.markProcessing(firstClaim);
        event.recordFailure("first error",3);

        event.markProcessing(firstClaim.plus(Duration.ofSeconds(5)));
        event.recordFailure("second error",3);

        event.markProcessing(firstClaim.plus(Duration.ofSeconds(15)));
        event.recordFailure("kafka failed",3);

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(event.getClaimedAt()).isNull();
        assertThat(event.getAttemptCount()).isEqualTo(3);
        assertThat(event.getLastError()).isEqualTo("kafka failed");

    }

    @Test
    void recordFailure_whenEventIsPending_throwsException(){
        Instant occurredAt = Instant.parse("2026-07-21T10:00:00Z");
        Instant firstClaim = occurredAt.plusSeconds(2);
        OutboxEvent event = OutboxEvent.create("TASK", UUID.randomUUID(),
                TaskEventType.CREATED.name(),"981842c5-4377-4220-b330-ecd719f1d462",
                "payload",occurredAt);

        assertThatThrownBy(() -> event.recordFailure("first error",3))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void retry_whenEventIsFailed_markAsPending(){
        Instant occurredAt = Instant.parse("2026-07-21T10:00:00Z");
        Instant firstClaim = occurredAt.plusSeconds(2);
        OutboxEvent event = OutboxEvent.create("TASK", UUID.randomUUID(),
                TaskEventType.CREATED.name(),"981842c5-4377-4220-b330-ecd719f1d462",
                "payload",occurredAt);

        event.markProcessing(firstClaim);
        event.recordFailure("first error",2);

        event.markProcessing(firstClaim.plus(Duration.ofSeconds(5)));
        event.recordFailure("kafka failed",2);

        event.retry();
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(event.getClaimedAt()).isNull();

    }


}
