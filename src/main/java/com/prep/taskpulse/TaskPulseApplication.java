package com.prep.taskpulse;

import com.prep.taskpulse.outbox.config.OutboxProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableConfigurationProperties(OutboxProperties.class)
@SpringBootApplication
public class TaskPulseApplication {
  public static void main(String[] args) {
    SpringApplication.run(TaskPulseApplication.class, args);
  }
}
