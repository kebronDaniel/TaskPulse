package com.prep.taskpulse.config;


import io.micrometer.core.instrument.MeterRegistry;
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
}
