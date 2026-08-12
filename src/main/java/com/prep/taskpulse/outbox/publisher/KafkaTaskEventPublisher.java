package com.prep.taskpulse.outbox.publisher;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaTaskEventPublisher implements EventPublisher {

  private final KafkaTemplate<String, String> kafkaTemplate;

  @Value("${taskflow.kafka.topics.task-events}")
  private String topics;

  @Override
  @CircuitBreaker(name = "kafkaPublisher")
  public CompletableFuture<Void> publish(String key, String payload) {
    return kafkaTemplate.send(topics, key, payload).thenApply(result -> null);
  }
}
