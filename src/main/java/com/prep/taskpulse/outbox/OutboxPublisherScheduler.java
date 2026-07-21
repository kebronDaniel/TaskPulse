package com.prep.taskpulse.outbox;

import com.prep.taskpulse.outbox.config.OutboxProperties;
import com.prep.taskpulse.outbox.repository.OutboxRepository;
import com.prep.taskpulse.outbox.service.OutboxClaimService;
import com.prep.taskpulse.outbox.service.OutboxEventProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OutboxPublisherScheduler {

    private final OutboxClaimService outboxClaimService;
    private final OutboxEventProcessor outboxEventProcessor;
    private final OutboxProperties outboxProperties;


    @Scheduled(fixedDelayString = "${taskflow.outbox.publisher-delay-ms:5000}")
    public void publishPendingEvents(){
        List<OutboxMessage> claimBatch = outboxClaimService.claimBatch(outboxProperties.batchSize());
        claimBatch.forEach(outboxEventProcessor::process);
    }
}
