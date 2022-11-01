package com.example.circuitbreakertrial;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.configure.CircuitBreakerConfigurationProperties;
import io.github.resilience4j.circuitbreaker.monitoring.health.CircuitBreakersHealthIndicator;
import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.EntryReplacedEvent;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.StatusAggregator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class CircuitBreakerConfig {
  Logger logger = LoggerFactory.getLogger(CircuitBreakerConfig.class);

  @Bean
  public RegistryEventConsumer<CircuitBreaker> registryEventConsumer() {

    return new RegistryEventConsumer<CircuitBreaker>() {
      @Override
      public void onEntryAddedEvent(EntryAddedEvent<CircuitBreaker> entryAddedEvent) {
        entryAddedEvent.getAddedEntry().getEventPublisher().onEvent(event -> logger.info("Circuit Breaker event {}", event.toString()));
      }

      @Override
      public void onEntryRemovedEvent(EntryRemovedEvent<CircuitBreaker> entryRemoveEvent) {
      }

      @Override
      public void onEntryReplacedEvent(EntryReplacedEvent<CircuitBreaker> entryReplacedEvent) {
      }
    };
  }

  // this is requried as the spring cloud circuit breaker that is a wraper around resilience4j
  // implemenntaion uses TimeLimiter and CircuitBreaker modules together when an instance is created
  // set to 1 hour to bypass/ exclude the timeLimiter.
  @Bean
  public Customizer<Resilience4JCircuitBreakerFactory> defaultCustomizer(
     @Value("${spring.datasource.hikari.connectionTimeout:5000}") long durationMilli) {
    return factory ->
       factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
          .timeLimiterConfig(TimeLimiterConfig.custom().timeoutDuration(Duration.ofHours(1)).build())
          .build());
  }
}
