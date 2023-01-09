package com.example.circuitbreakertrial;

import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.EntryReplacedEvent;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.retry.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RetryConfig {
  Logger logger = LoggerFactory.getLogger(RetryConfig.class);

  @Bean("retryEventConsumer")
  public RegistryEventConsumer<Retry> registryEventConsumer() {

    return new RegistryEventConsumer<Retry>() {
      @Override
      public void onEntryAddedEvent(EntryAddedEvent<Retry> entryAddedEvent) {
        entryAddedEvent.getAddedEntry().getEventPublisher().onEvent(event -> logger.info("Retry  event {}", event.toString()));
      }

      @Override
      public void onEntryRemovedEvent(EntryRemovedEvent<Retry> entryRemoveEvent) {
      }

      @Override
      public void onEntryReplacedEvent(EntryReplacedEvent<Retry> entryReplacedEvent) {
      }
    };
  }
}
