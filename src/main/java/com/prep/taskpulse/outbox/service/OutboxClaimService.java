package com.prep.taskpulse.outbox.service;

import com.prep.taskpulse.outbox.OutboxEvent;
import com.prep.taskpulse.outbox.OutboxMessage;
import com.prep.taskpulse.outbox.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OutboxClaimService {

    private final OutboxRepository outboxRepository;
    private final Clock clock;

    @Transactional
    public List<OutboxMessage> claimBatch(int batchSize){
        List<OutboxEvent> events = outboxRepository.findPendingEventsForUpdate(batchSize);

        Instant claimedAt = clock.instant();
        events.forEach(event -> event.markProcessing(claimedAt));
        return events.stream()
                .map(event -> new OutboxMessage(event.getId(),event.getPartitionKey(),event.getPayload()))
                .toList();
    }
}
