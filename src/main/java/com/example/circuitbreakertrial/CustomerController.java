package com.example.circuitbreakertrial;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
public class CustomerController {

  @Autowired
  CustomerRepository customerRepository;

  @Autowired
  CustomerService customerService;

  @Autowired
  CircuitBreakerRegistry circuitBreakerRegistry;

  Logger logger = LoggerFactory.getLogger(CustomerController.class.getName());

  @GetMapping("/ping")
  public String ping() {
    logger.info("INVOKED : {}", "/ping");
    return "pong";
  }

  @GetMapping("/customers")
  List<CustomerEntity> getCustomers() {
    logger.info("INVOKED : {}", "/customers");
    return customerRepository.findAll();
  }

  @GetMapping("/customers/transform")
  public List getCustomersTransform()
     throws InterruptedException {
    List customers = customerRepository.findAll();
    logger.info("INVOKED : {}", "/customers/transform");

    Thread.sleep(5000); //slow operation

    return customers;
  }

  @GetMapping("/customer/{name}")
  public ResponseEntity getCustomerName(@PathVariable(name = "name") String name, @RequestParam(name = "cb") int cb) {
    logger.info("INVOKED : {}", "/customer/{name}");
//    logger.info("CB value : {}", circuitBreakerRegistry.circuitBreaker("trialDB").getCircuitBreakerConfig().toString());

    ;
    Optional<CustomerEntity> optionalEntity = null;
    if(cb == 1) {
      // using fuctional Programming
      optionalEntity = customerService.getCustomerByNameCB1(name);
    } else if(cb == 2) {
      optionalEntity = customerService.getCustomerByNameCB2(name);
    }else if(cb==3){
      optionalEntity=customerService.getCustomerByNameCB3(name);
    }else {
      optionalEntity = customerService.getCustomerByNameNormal(name);
    }

    return optionalEntity.isPresent() ? ResponseEntity.ok(optionalEntity.get()) : ResponseEntity.status(HttpStatus.NOT_FOUND).build();
  }
}
