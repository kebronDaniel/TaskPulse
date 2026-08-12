package com.prep.taskpulse.outbox.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prep.taskpulse.domain.task.TaskEvent;
import com.prep.taskpulse.domain.task.enums.TaskEventType;
import com.prep.taskpulse.exception.OutboxSerializationException;
import com.prep.taskpulse.outbox.OutboxEvent;
import com.prep.taskpulse.outbox.repository.OutboxRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class OutboxServiceTest {

  @Mock private OutboxRepository outboxRepository;

  @Mock private ObjectMapper objectMapper;

  @InjectMocks private OutboxService outboxService;

  private TaskEvent taskEvent;

  @BeforeEach
  void setUp() {
    taskEvent =
        new TaskEvent(
            UUID.randomUUID(),
            TaskEventType.CREATED,
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            Instant.now());
  }

  @Test
  void save_whenSerializationSucceeds_persistsPendingOutboxEvent() {

    outboxService.save(taskEvent);
    ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
    verify(outboxRepository).save(captor.capture());

    OutboxEvent outboxEvent = captor.getValue();

    assertThat(outboxEvent).isNotNull();
    assertThat(outboxEvent.getEventType()).isEqualTo("CREATED");
    assertThat(outboxEvent.getAggregateType()).isEqualTo("TASK");
  }

  @Test
  void save_whenSerializationFails_throwsAndDoesNotPersist() throws Exception {
    JsonProcessingException serializationFailure =
        new JsonProcessingException("Invalid event payload") {};

    when(objectMapper.writeValueAsString(taskEvent)).thenThrow(serializationFailure);

    assertThatThrownBy(() -> outboxService.save(taskEvent))
        .isInstanceOf(OutboxSerializationException.class)
        .hasMessage("Failed to serialize :" + taskEvent.eventId())
        .hasCause(serializationFailure);

    verify(objectMapper).writeValueAsString(taskEvent);
    verifyNoInteractions(outboxRepository);
  }
}
