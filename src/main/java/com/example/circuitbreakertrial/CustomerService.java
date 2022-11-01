package com.example.circuitbreakertrial;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Optional;

@Service
public class CustomerService {

  Logger logger = LoggerFactory.getLogger(CustomerService.class);

  @Autowired
  private CircuitBreakerFactory circuitBreakerFactory;

  @Autowired
  CustomerRepository customerRepository;

  CircuitBreaker circuitBreaker;

  @PostConstruct
  public void initCircuitBreaker() {
    circuitBreaker = circuitBreakerFactory.create("trialDB");
  }

  public Optional<CustomerEntity> getCustomerByNameNormal(String name) {
    return Optional.ofNullable(customerRepository.findByName(name));
  }

  public Optional<CustomerEntity> getCustomerByNameCB1(String name) {

    return Optional.ofNullable(circuitBreaker.run(() -> customerRepository.findByName(name),
       e -> {
         logger.error("exception raised {}", e);
         logger.info("in fall back method of CB1  !!!!");
         return null;
       }));
  }

  @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(name = "trialDB", fallbackMethod = "fallback")
  public Optional<CustomerEntity> getCustomerByNameCB2(String name) {
    return Optional.ofNullable(customerRepository.findByName(name));
  }

  public Optional<CustomerEntity> fallback(String name, Throwable e) {
    logger.info("in fall back method of CB2  !!!!");
    return Optional.ofNullable(null);
  }
}
