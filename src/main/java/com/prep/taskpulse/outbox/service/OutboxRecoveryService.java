package com.prep.taskpulse.outbox.service;

import com.prep.taskpulse.outbox.OutboxEvent;
import com.prep.taskpulse.outbox.OutboxStatus;
import com.prep.taskpulse.outbox.config.OutboxProperties;
import com.prep.taskpulse.outbox.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OutboxRecoveryService {

    private final OutboxRepository outboxRepository;
    private final OutboxProperties outboxProperties;
    private final Clock clock;

    @Transactional
    public int releaseClaims(){
        Instant claimedBefore = clock.instant().minus(outboxProperties.claimTimeout());
        List<OutboxEvent> staleEvents = outboxRepository.findByStatusAndClaimedAtBefore(OutboxStatus.PROCESSING,claimedBefore);
        staleEvents.forEach(OutboxEvent::releaseStaleClaim);
        return staleEvents.size();
    }
}
