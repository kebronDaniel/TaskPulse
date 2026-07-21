package com.prep.taskpulse.outbox.service;

import com.prep.taskpulse.outbox.OutboxEvent;
import com.prep.taskpulse.outbox.OutboxMessage;
import com.prep.taskpulse.outbox.OutboxStatus;
import com.prep.taskpulse.outbox.publisher.EventPublisher;
import com.prep.taskpulse.outbox.repository.OutboxRepository;
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
        } catch (CompletionException exception){
            outboxStatusService.recordFailure(message.eventId(),rootCauseMessage(exception));
        } catch (RuntimeException exception){
            outboxStatusService.recordFailure(message.eventId(),rootCauseMessage(exception));
        }

    }

    private String rootCauseMessage(Throwable throwable) {
        Throwable cause = throwable.getCause() != null
                ? throwable.getCause()
                : throwable;
        return cause.getMessage();
    }
}
