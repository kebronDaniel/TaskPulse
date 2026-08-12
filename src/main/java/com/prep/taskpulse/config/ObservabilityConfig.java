package com.prep.taskpulse.config;

import com.prep.taskpulse.outbox.OutboxStatus;
import com.prep.taskpulse.outbox.repository.OutboxRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
public class ObservabilityConfig {

  @Bean
  public MeterRegistryCustomizer<MeterRegistry> commonMetricTag() {
    // to add tag to every meter which is used by prometheus.
    return registry -> registry.config().commonTags("service", "taskpulse");
  }

  @Bean
  public MeterBinder outboxMetrics(OutboxRepository outboxRepository) {
    return registry -> {
      registerOutboxGauge(registry, outboxRepository, OutboxStatus.PENDING);
      registerOutboxGauge(registry, outboxRepository, OutboxStatus.PROCESSING);
      registerOutboxGauge(registry, outboxRepository, OutboxStatus.FAILED);
      // unnecessary load to the db if we add also the published.
    };
  }

  // creates a child span due @observed annotation in the producer
  // injects the trace and span ID to the child calls.
  // registers them under one parent.
  @Bean
  public InitializingBean enableKafkaTemplateObservation(
      KafkaTemplate<?, ?> kafkaTemplate, ObservationRegistry observationRegistry) {
    return () -> {
      kafkaTemplate.setObservationRegistry(observationRegistry);
      // observation-managed timers instead of its older standalone Micrometer timer behavior
      // this prevents double counting for child spans
      kafkaTemplate.setObservationEnabled(true);
    };
  }

  private void registerOutboxGauge(
      MeterRegistry registry, OutboxRepository repository, OutboxStatus status) {
    Gauge.builder("taskflow.outbox.events", repository, repo -> repo.countByStatus(status))
        .description("Current number of outbox events by status")
        .tag("status", status.name().toLowerCase())
        .register(registry);
  }
}
