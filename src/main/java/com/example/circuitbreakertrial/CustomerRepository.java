package com.example.circuitbreakertrial;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<CustomerEntity, String> {

  CustomerEntity findByName(String name);
}


