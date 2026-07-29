package com.prep.taskpulse.outbox.service;

import com.prep.taskpulse.outbox.OutboxMessage;
import com.prep.taskpulse.outbox.publisher.EventPublisher;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxEventProcessor {

    private final OutboxStatusService outboxStatusService;
    private final EventPublisher taskEventPublisher;

    // to create a parent wrapper over the processes under this producer.
    @Observed(name = "taskflow.outbox.process", contextualName = "outbox-process")
    @Transactional
    public void process(OutboxMessage message){

        // assign eventId to the thread MDC and close once done.
        try (MDC.MDCCloseable ignored = MDC.putCloseable("eventId", message.eventId().toString())){
            taskEventPublisher.publish(message.partitionKey(), message.payload()).join();
            log.atDebug().addKeyValue("outcome", "published").log("Outbox event published");
            outboxStatusService.markPublished(message.eventId());
        }
        catch (RuntimeException exception){
            Throwable error = rootCause(exception);
            String errorMessage = error.getMessage();
            if (error instanceof CallNotPermittedException){
                log.atError().addKeyValue("outcome","failed")
                        .addKeyValue("reason", errorMessage)
                        .log("Outbox publication deferred because circuit is open");
                outboxStatusService.defer(message.eventId(),errorMessage);
            } else {
                log.atError().addKeyValue("outcome","failed")
                        .addKeyValue("exceptionType", error.getClass().getSimpleName())
                        .setCause(error)
                        .log("Outbox publication failed");
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
