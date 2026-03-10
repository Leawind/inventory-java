package io.github.leawind.inventory;

import static org.junit.jupiter.api.Assertions.*;

import io.github.leawind.inventory.event.EventEmitter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class EventEmitterTest {
  private EventEmitter<Object> eventEmitter;

  @BeforeEach
  void setUp() {
    eventEmitter = new EventEmitter<>();
  }

  @Test
  void testOnce() {
    var s = new StringBuilder();

    eventEmitter
        .on(e -> s.append("A"))
        .once(e -> s.append("B"))
        .once(e -> s.append("C"))
        .once("a key", e -> s.append("D"))
        .once("a key", e -> s.append("E"));

    eventEmitter.emit(null);
    eventEmitter.emit(null);

    assertEquals("ABCEA", s.toString());
  }

  @Test
  void testPriority() {
    var s = new StringBuilder();

    eventEmitter
        .on(e -> s.append('A'), 1)
        .on(e -> s.append('B'), 2)
        .on(e -> s.append('C'), 2)
        .on(e -> s.append('D'), 1);

    eventEmitter.emit(null);
    assertEquals("BCAD", s.toString());
  }

  @Test
  void testOff() {
    var s = new StringBuilder();

    var listenerA = eventEmitter.listener(e -> s.append("A"));
    var listenerB = eventEmitter.listener(e -> s.append("B"));

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
    var s = new StringBuilder();

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
    var s = new StringBuilder();

    eventEmitter
        .on(
            (e, ctrl) -> {
              s.append("A");
              ctrl.unsubscribe();
            })
        .on(e -> s.append("B"));

    // First broadcast: both listeners execute, A removes itself
    eventEmitter.emit("test");
    assertEquals("AB", s.toString());

    // Second broadcast: only B remains
    s.setLength(0);
    eventEmitter.emit("test");
    assertEquals("B", s.toString());
  }

  @Test
  void testClear() {
    var s = new StringBuilder();

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
  void on() {
    var s = new StringBuilder();

    eventEmitter.on("testKey", e -> s.append("A")).on("testKey", e -> s.append("B"));

    eventEmitter.emit(null);
    assertEquals("B", s.toString());
  }
}
