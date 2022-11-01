package com.example.circuitbreakertrial;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {CircuitBreakerTrialApplication.class})
@SpringBootTest(classes = CircuitBreakerTrialApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Execution(ExecutionMode.SAME_THREAD)
@ActiveProfiles("test")
// option to exclude Repository autoconfiguration
//@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class, DataSourceTransactionManagerAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CustomerServiceCircuitBreakerTest {
  // main autowiring and mocks
  @Autowired
  CircuitBreakerRegistry circuitBreakerRegistry;
  @Autowired
  private CustomerService customerService;
  @MockBean
  CustomerRepository customerRepository;
  private String circuitBreakerInstance = "trialDB";
  public static final String name = "testName";

  static int successfulCalls = 0;
  static int failedCalls = 0;
  static int totalCalls = 0;

  @Value("${resilience4j.circuitbreaker.instances.trialDB.minimumNumberOfCalls:-1}")
  private int propertyValue;
  @Value("${resilience4j.circuitbreaker.instances.trialDB.failureRateThreshold:-1}")
  private int failurerateThreshold;
  // set this based on {@link propertyValue }
  private final int minimumNumberOfCalls = 5;

  // additional mocks if any  as @SpringBootTest will spin up the entire Application context
  // none of these components are used to test the circuit breaker implementation of CustomerService

  @BeforeEach
  public void checkIfTheCircuitBreakerConfigExists() {
    // to make sure minimumNumberOfCalls is configured according to properties defined in the test/resources/application.yml file
    Assumptions.assumeTrue(propertyValue > 0 && propertyValue == minimumNumberOfCalls, "Missing/mismatched circuit breaker properties");
    Assumptions.assumeTrue(failurerateThreshold == 50, "threshold set is not 50%");
  }

  @Test
  @Order(1)
  public void verifyCircuitRegistryToClosedStateInitially() {
    assertEquals(CircuitBreaker.State.CLOSED, circuitBreakerRegistry.circuitBreaker(circuitBreakerInstance).getState());
  }

  // CLOSED to OPEN State
  // execute success for the first call
  // executing failures for minimumNumberOfCalls - 1 to get the failure rate > 50%
  @RepeatedTest(value = minimumNumberOfCalls, name = RepeatedTest.LONG_DISPLAY_NAME)
  @Order(2)
  void repeatedTestForInvokingOpenState(RepetitionInfo repetitionInfo) throws SQLException {
    if(repetitionInfo.getCurrentRepetition() == 1) {
      Mockito.when(customerRepository.findByName(name)).thenReturn(new CustomerEntity());
      Optional<CustomerEntity> entity = customerService.getCustomerByNameCB1(name);
      incrementSuccessfulCalls();
      Assertions.assertTrue(entity.isPresent());
    } else {
      // throwing exceptions to simulate the DB failure
      Mockito.when(customerRepository.findByName(name)).
         thenThrow(new DataAccessResourceFailureException("host not found exception"));
      Optional<CustomerEntity> entity = customerService.getCustomerByNameCB1(name);
      incrementFailedCalls();
      // from the fallback
      Assertions.assertFalse(entity.isPresent());
    }

    Assertions.assertTrue(circuitBreakerRegistry.circuitBreaker(circuitBreakerInstance).getMetrics().getNumberOfFailedCalls() == failedCalls);
    Assertions.assertTrue(circuitBreakerRegistry.circuitBreaker(circuitBreakerInstance).getMetrics().getNumberOfSuccessfulCalls() == successfulCalls);
    if(totalCalls < minimumNumberOfCalls) {
      // closed until minimumNumberOfCalls are recorded
      assertEquals(CircuitBreaker.State.CLOSED, circuitBreakerRegistry.circuitBreaker(circuitBreakerInstance).getState());
    } else {
      // circuit calculates the threshhold when the minimumNumberOfCalls are recorded and OPENS
      Assertions.assertTrue(circuitBreakerRegistry.circuitBreaker(circuitBreakerInstance).getMetrics().getFailureRate() >= failurerateThreshold);
      assertEquals(CircuitBreaker.State.OPEN, circuitBreakerRegistry.circuitBreaker(circuitBreakerInstance).getState());
    }
    Mockito.verify(customerRepository).findByName(name);
  }

  // OPEN State
  //in open state - calls should not invoke customerRepository
  @Test
  @Order(3)
  void testInOpenState() throws SQLException {
    Assumptions.assumeTrue(CircuitBreaker.State.OPEN.equals(circuitBreakerRegistry.circuitBreaker(circuitBreakerInstance).getState()));
    Optional<CustomerEntity> entity = customerService.getCustomerByNameCB1(name);
    Assertions.assertFalse(entity.isPresent());
    Assumptions.assumeTrue(CircuitBreaker.State.OPEN.equals(circuitBreakerRegistry.circuitBreaker(circuitBreakerInstance).getState()));
    assertEquals(CircuitBreaker.State.OPEN, circuitBreakerRegistry.circuitBreaker(circuitBreakerInstance).getState());
    // no resource interaction
    Mockito.verifyNoInteractions(customerRepository);
  }

  // HALF_OPEN state
  @Test
  @Order(4)
  void changeStateToHalfOpen() {
    circuitBreakerRegistry.circuitBreaker(circuitBreakerInstance).transitionToHalfOpenState();
    assertEquals(CircuitBreaker.State.HALF_OPEN, circuitBreakerRegistry.circuitBreaker(circuitBreakerInstance).getState());
    // reset counts
    totalCalls = 0;
    successfulCalls = 0;
    failedCalls = 0;
  }

  // HALF_OPEN  to CLOSED
  // executing all success events to change the state back to CLOSED from HALF OPEN
  @RepeatedTest(value = minimumNumberOfCalls, name = RepeatedTest.LONG_DISPLAY_NAME)
  @Order(5)
  public void testCaseForSuccessCosedStateInHalfOpen(RepetitionInfo repetitionInfo) throws SQLException {
    Mockito.when(customerRepository.findByName(name)).thenReturn(new CustomerEntity());
    Optional<CustomerEntity> entity = customerService.getCustomerByNameCB1(name);
    Assertions.assertTrue(entity.isPresent());
    incrementSuccessfulCalls();
    if(totalCalls < minimumNumberOfCalls) {
      // till the minimumNumberOfCalls is reached the sate remains as HALF_OPEN
      assertEquals(CircuitBreaker.State.HALF_OPEN, circuitBreakerRegistry.circuitBreaker(circuitBreakerInstance).getState());
    } else {
      // circuit calculates the threshhold when the minimumNumberOfCalls are recorded and changes to CLOSED
      assertEquals(CircuitBreaker.State.CLOSED, circuitBreakerRegistry.circuitBreaker(circuitBreakerInstance).getState());
    }
    Mockito.verify(customerRepository).findByName(name);
  }

  private void incrementSuccessfulCalls() {
    successfulCalls++;
    totalCalls++;
  }

  private void incrementFailedCalls() {
    failedCalls++;
    totalCalls++;
  }
}
