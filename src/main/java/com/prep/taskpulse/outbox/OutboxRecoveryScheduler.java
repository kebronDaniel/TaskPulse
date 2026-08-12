package com.prep.taskpulse.outbox;

import com.prep.taskpulse.outbox.service.OutboxRecoveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxRecoveryScheduler {

  private final OutboxRecoveryService outboxRecoveryService;

  @Scheduled(fixedDelayString = "${taskflow.outbox.recovery-delay-ms:60000}")
  public void recoverStaleClaim() {
    outboxRecoveryService.releaseClaims();
  }
}
