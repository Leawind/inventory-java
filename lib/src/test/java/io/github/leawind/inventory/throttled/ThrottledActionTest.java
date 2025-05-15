package io.github.leawind.inventory.throttled;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ThrottledActionTest {
  private ThrottledAction<String> throttledAction;
  private AtomicInteger actionCounter;

  @BeforeEach
  void setUp() {
    actionCounter = new AtomicInteger(0);
    throttledAction =
        new ThrottledAction<>(
            () -> {
              actionCounter.incrementAndGet();
              return "result-" + actionCounter.get();
            },
            100);
  }

  @AfterEach
  void shutdown() {
    throttledAction.shutdown();
  }

  @Test
  void testExecuteImmediately() {
    String result1 = throttledAction.executeImmediately();
    assertEquals(1, actionCounter.get());
    assertEquals("result-1", result1);
    assertTrue(throttledAction.sinceLastExecute() >= 0);

    String result2 = throttledAction.executeImmediately();
    assertEquals(2, actionCounter.get());
    assertEquals("result-2", result2);
  }

  @Test
  void testUrge_FirstTime() {
    CompletableFuture<String> future = throttledAction.urge();
    // Should execute immediately
    assertFalse(throttledAction.isScheduled());
    assertEquals("result-1", future.join());
    assertEquals(1, actionCounter.get());
  }

  @Test
  void testUrge_WithinInterval() throws InterruptedException, ExecutionException, TimeoutException {
    // First execution
    throttledAction.executeImmediately();
    assertEquals(1, actionCounter.get());

    // Urge within interval
    CompletableFuture<String> future = throttledAction.urge();
    assertTrue(throttledAction.isScheduled());

    // Wait for scheduled execution
    String result = future.get();
    assertEquals("result-2", result);
    assertEquals(2, actionCounter.get());
    assertFalse(throttledAction.isScheduled());
  }

  @Test
  void testUrge_MultipleUrgesWithinInterval() {
    // First execution
    throttledAction.executeImmediately();
    assertEquals(1, actionCounter.get());

    // First urge (should schedule)
    CompletableFuture<String> future1 = throttledAction.urge();
    assertTrue(throttledAction.isScheduled());

    // Second urge (should return same future)
    CompletableFuture<String> future2 = throttledAction.urge();
    assertSame(future1, future2);

    // Wait and verify only one execution happened
    String result = future1.join();
    assertEquals("result-2", result);
    assertEquals(2, actionCounter.get());
  }

  @Test
  void testUrge_AfterInterval() throws InterruptedException {
    throttledAction.executeImmediately();
    assertEquals(1, actionCounter.get());

    // Longer than 100 ms
    Thread.sleep(150);

    // Urge after interval - should execute immediately
    CompletableFuture<String> future = throttledAction.urge();
    assertFalse(throttledAction.isScheduled());
    assertEquals("result-2", future.join());
    assertEquals(2, actionCounter.get());
  }
}
