package com.prep.taskpulse.outbox.service;

import com.prep.taskpulse.outbox.OutboxEvent;
import com.prep.taskpulse.outbox.OutboxMessage;
import com.prep.taskpulse.outbox.OutboxStatus;
import com.prep.taskpulse.outbox.publisher.EventPublisher;
import com.prep.taskpulse.outbox.repository.OutboxRepository;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.CompletionException;

@Service
@RequiredArgsConstructor
public class OutboxEventProcessor {

    private final OutboxStatusService outboxStatusService;
    private final EventPublisher taskEventPublisher;

    @Transactional
    public void process(OutboxMessage message){

        try {
            taskEventPublisher.publish(message.partitionKey(), message.payload()).join();
            outboxStatusService.markPublished(message.eventId());
        }
        catch (RuntimeException exception){
            Throwable error = rootCause(exception);
            String errorMessage = error.getMessage();
            if (exception instanceof CallNotPermittedException){
                outboxStatusService.defer(message.eventId(),errorMessage);
            } else {
                outboxStatusService.recordFailure(message.eventId(),errorMessage);
            }
        }

    }

    private Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        // its a loop that is to get the bottom of the issue like pilling an onion.
        // the second one part of the condition is to protect against self caused exception.
        // meaning if an error is caused by itself then it would break the loop.
        while (current.getCause() != null && current.getCause() != current){
            current = current.getCause();
        }
        return current;
    }
}
