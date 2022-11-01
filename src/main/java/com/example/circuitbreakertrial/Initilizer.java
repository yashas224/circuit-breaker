package com.example.circuitbreakertrial;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class Initilizer implements CommandLineRunner {
  @Autowired
  CustomerRepository customerRepository;

  @Override
  public void run(String... args) throws Exception {
    if(customerRepository.findAll().size() == 0) {
      customerRepository.save(CustomerEntity.builder().name("yashas").build());
    }
  }
}
