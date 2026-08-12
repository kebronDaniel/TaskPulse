package com.prep.taskpulse.outbox.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prep.taskpulse.domain.task.TaskEvent;
import com.prep.taskpulse.exception.OutboxSerializationException;
import com.prep.taskpulse.outbox.OutboxEvent;
import com.prep.taskpulse.outbox.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OutboxService {

  private final OutboxRepository outboxRepository;
  private final ObjectMapper objectMapper;

  public void save(TaskEvent event) {
    try {
      String payload = objectMapper.writeValueAsString(event);
      OutboxEvent outboxEvent =
          OutboxEvent.create(
              "TASK",
              event.taskId(),
              event.type().name(),
              event.workspaceId().toString(),
              payload,
              event.occurredAt());
      outboxRepository.save(outboxEvent);
    } catch (JsonProcessingException exception) {
      throw new OutboxSerializationException(event.eventId(), exception);
    }
  }
}
