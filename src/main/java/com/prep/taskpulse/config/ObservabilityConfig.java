package com.prep.taskpulse.config;


import com.prep.taskpulse.outbox.OutboxStatus;
import com.prep.taskpulse.outbox.repository.OutboxRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ObservabilityConfig {

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> commonMetricTag(){
        // to add tag to every meter which is used by prometheus.
        return registry -> registry.config().commonTags("service","taskpulse");
    }

    @Bean
    public MeterBinder outboxMetrics(OutboxRepository outboxRepository){
        return  registry -> {
            registerOutboxGauge(registry,outboxRepository,OutboxStatus.PENDING);
            registerOutboxGauge(registry,outboxRepository,OutboxStatus.PROCESSING);
            registerOutboxGauge(registry,outboxRepository,OutboxStatus.FAILED);
            // unnecessary load to the db if we add also the published.
        };
    }

    private void registerOutboxGauge(MeterRegistry registry, OutboxRepository repository, OutboxStatus status){
        Gauge.builder(
                "taskflow.outbox.events",repository, repo -> repo.countByStatus(status))
                .description("Current number of outbox events by status")
                .tag("status", status.name().toLowerCase()).register(registry);
    }
}
