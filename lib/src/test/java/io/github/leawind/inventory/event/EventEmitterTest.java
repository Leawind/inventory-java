package io.github.leawind.inventory.event;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class EventEmitterTest {
  private EventEmitter<Object> eventEmitter;

  @BeforeEach
  void setUp() {
    eventEmitter = new EventEmitter<>();
  }

  // region Core Functional Tests

  @Test
  void testOnce() {
    StringBuilder s = new StringBuilder();

    eventEmitter
        .on(e -> s.append("A"))
        .once(() -> s.append("B"))
        .once(e -> s.append("C"))
        .once("a key", e -> s.append("D"))
        .once("a key", e -> s.append("E"));

    eventEmitter.emit(null);
    eventEmitter.emit(null);

    assertEquals("ABCEA", s.toString());
  }

  @Test
  void testPriority() {
    StringBuilder s = new StringBuilder();

    eventEmitter
        .on(e -> s.append('A'), 1)
        .on(() -> s.append('B'), 2)
        .on(e -> s.append('C'), 2)
        .on(() -> s.append('D'), 1);

    eventEmitter.emit(null);
    assertEquals("BCAD", s.toString());
  }

  @Test
  void testOff() {
    StringBuilder s = new StringBuilder();

    EventEmitter.Listener<Object> listenerA = eventEmitter.listener(e -> s.append("A"));
    EventEmitter.Listener<Object> listenerB = eventEmitter.listener(e -> s.append("B"));

    eventEmitter.on(listenerA);
    eventEmitter.on(listenerB, 4);

    eventEmitter.on("alice", e -> s.append("C"));
    eventEmitter.on("bob", e -> s.append("D"));
    eventEmitter.on("alice", e -> s.append("E"));

    eventEmitter.emit(null);
    assertEquals("BADE", s.toString());

    s.delete(0, s.length());

    eventEmitter.off(listenerA);
    eventEmitter.off("bob");

    eventEmitter.emit(null);
    assertEquals("BE", s.toString());
  }

  @Test
  void testStopPropagation() {
    StringBuilder s = new StringBuilder();

    eventEmitter
        .on(
            (e, ctrl) -> {
              s.append("A");
              ctrl.stop();
            })
        .on(e -> s.append("B"));

    eventEmitter.emit(null);
    assertEquals("A", s.toString());
  }

  @Test
  void testRemoveSelf() {
    StringBuilder s = new StringBuilder();

    eventEmitter
        .on(
            (e, ctrl) -> {
              s.append("A");
              ctrl.unsubscribe();
            })
        .on(e -> s.append("B"));

    eventEmitter.emit("test");
    assertEquals("AB", s.toString());

    s.setLength(0);
    eventEmitter.emit("test");
    assertEquals("B", s.toString());
  }

  @Test
  void testClear() {
    StringBuilder s = new StringBuilder();

    eventEmitter.on(e -> s.append("A")).on(e -> s.append("B")).clear().on(e -> s.append("C"));

    eventEmitter.emit("test");
    assertEquals("C", s.toString());
  }

  @Test
  void on_withNullKey_shouldThrowException() {
    assertThrows(IllegalArgumentException.class, () -> eventEmitter.on(null, e -> {}, 0));
  }

  @Test
  void on_withNewKey_shouldSubscribe() {
    eventEmitter.on("testKey", e -> {}, 1);
    assertTrue(eventEmitter.hasKey("testKey"));
    assertFalse(eventEmitter.hasKey("no such key"));
  }

  @Test
  void on_withExistingKey_shouldReplace() {
    StringBuilder s = new StringBuilder();

    eventEmitter.on("testKey", e -> s.append("A")).on("testKey", e -> s.append("B"));

    eventEmitter.emit(null);
    assertEquals("B", s.toString());
  }

  // endregion

  // region Memory Management & Weak Reference Tests

  @Disabled
  @Test
  void testWeakKeyGarbageCollection() throws InterruptedException {
    AtomicInteger executionCount = new AtomicInteger(0);

    Object key = new Object();
    eventEmitter.on(key, e -> executionCount.incrementAndGet());

    java.lang.ref.WeakReference<Object> keyTracker = new java.lang.ref.WeakReference<>(key);

    key = null;

    long startTime = System.currentTimeMillis();
    while (keyTracker.get() != null) {
      System.gc();
      Thread.sleep(10);
      if (System.currentTimeMillis() - startTime > 2000) {
        fail("Key was not garbage collected within the timeout period.");
      }
    }

    eventEmitter.emit(null);
    assertEquals(0, executionCount.get());

    assertFalse(eventEmitter.hasKey(keyTracker.get()));
  }

  @Test
  void testWeakKeySurvivesWhenStronglyReferenced() {
    AtomicInteger executionCount = new AtomicInteger(0);

    Object key = new Object();
    eventEmitter.on(key, e -> executionCount.incrementAndGet());

    System.gc();

    eventEmitter.emit(null);
    assertEquals(1, executionCount.get());
  }

  // endregion

  // region Custom Map Injection Tests

  @Test
  void constructor_withPrePopulatedMap_shouldThrowException() {
    Map<Object, Object> prePopulatedMap = new HashMap<>();
    prePopulatedMap.put("existingKey", "someValue");

    assertThrows(IllegalArgumentException.class, () -> new EventEmitter<>(prePopulatedMap));
  }

  @Test
  void constructor_withCustomEmptyMap_shouldFunctionCorrectly() {
    Map<Object, ?> customMap = new LinkedHashMap<>();
    EventEmitter<Object> customEmitter = new EventEmitter<>(customMap);

    AtomicInteger count = new AtomicInteger(0);
    customEmitter.on("customKey", e -> count.incrementAndGet());

    customEmitter.emit(null);
    assertEquals(1, count.get());
    assertTrue(customEmitter.hasKey("customKey"));
  }

  // endregion

  // region Edge Cases & Defensive Programming Tests

  @Test
  void off_withNonExistentKey_shouldNotThrow() {
    assertDoesNotThrow(() -> eventEmitter.off("nonExistentKey"));
  }

  @Test
  void off_withNonExistentListener_shouldNotThrow() {
    EventEmitter.Listener.NoArg<Object> unregisteredListener = () -> {};
    assertDoesNotThrow(() -> eventEmitter.off(unregisteredListener));
  }

  @Test
  void getListener_withExistingKey_shouldReturnListener() {
    EventEmitter.Listener.NoArg<Object> listener = () -> {};
    eventEmitter.on("myKey", listener);

    assertEquals(listener, eventEmitter.getListener("myKey"));
  }

  @Test
  void getListener_withNonExistentKey_shouldReturnNull() {
    assertNull(eventEmitter.getListener("nonExistentKey"));
  }

  @Test
  void getListener_withNullKey_shouldReturnNull() {
    assertNull(eventEmitter.getListener(null));
  }

  @Test
  void emit_withNoListeners_shouldNotThrow() {
    assertDoesNotThrow(() -> eventEmitter.emit("payload"));
  }

  @Test
  void emit_withPayload_shouldPassPayloadToListener() {
    AtomicInteger receivedPayload = new AtomicInteger(0);

    eventEmitter.on((e) -> receivedPayload.set((Integer) e));

    @SuppressWarnings("unchecked")
    EventEmitter<Integer> intEmitter = (EventEmitter<Integer>) (EventEmitter<?>) eventEmitter;

    intEmitter.emit(42);
    assertEquals(42, receivedPayload.get());
  }

  // endregion
}
