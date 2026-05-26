package io.github.leawind.inventory.event;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SimpleEventEmitterTest {
  private SimpleEventEmitter<String> eventEmitter;

  @BeforeEach
  void setUp() {
    eventEmitter = new SimpleEventEmitter<>();
  }

  @Test
  void testOn_and_emit() {
    StringBuilder s = new StringBuilder();

    eventEmitter.on(s::append);
    eventEmitter.emit("A");
    eventEmitter.emit("B");
    eventEmitter.emit("C");

    assertEquals("ABC", s.toString());
  }

  @Test
  void testMultipleListeners_allShouldBeTriggered() {
    StringBuilder s = new StringBuilder();

    eventEmitter.on(e -> s.append("1"));
    eventEmitter.on(e -> s.append("2"));
    eventEmitter.on(e -> s.append("3"));

    eventEmitter.emit("test");

    assertEquals("123", s.toString());
  }

  @Test
  void testMultipleListeners_orderPreserved() {
    ArrayList<String> result = new ArrayList<String>();

    eventEmitter.on(e -> result.add("First"));
    eventEmitter.on(e -> result.add("Second"));
    eventEmitter.on(e -> result.add("Third"));

    eventEmitter.emit("test");

    assertEquals(java.util.Arrays.asList("First", "Second", "Third"), result);
  }

  @Test
  void testNoArgListener_on() {
    StringBuilder s = new StringBuilder();

    eventEmitter.on(() -> s.append("X"));

    eventEmitter.emit("ignored");
    eventEmitter.emit("ignored");

    assertEquals("XX", s.toString());
  }

  @Test
  void testMixedListenerTypes_bothTriggered() {
    StringBuilder s = new StringBuilder();

    eventEmitter.on(e -> s.append(e));
    eventEmitter.on(() -> s.append("-"));

    eventEmitter.emit("A");
    eventEmitter.emit("B");

    assertEquals("A-B-", s.toString());
  }

  @Test
  void testClear_shouldRemoveAllListeners() {
    StringBuilder s = new StringBuilder();

    eventEmitter.on(s::append);
    eventEmitter.on(() -> s.append("X"));

    eventEmitter.emit("A");
    eventEmitter.clear();
    eventEmitter.emit("B");

    assertEquals("AX", s.toString());
  }

  @Test
  void testConstructor_withExistingListeners() {
    StringBuilder s = new StringBuilder();

    ArrayList<SimpleEventEmitter.Listener<String>> listeners =
        new ArrayList<SimpleEventEmitter.Listener<String>>();
    listeners.add(e -> s.append("1"));
    listeners.add(e -> s.append("2"));

    SimpleEventEmitter<String> customEmitter = new SimpleEventEmitter<>(listeners);
    customEmitter.emit("test");

    assertEquals("12", s.toString());
  }

  @Test
  void testChaining_on() {
    SimpleEventEmitter<String> result = eventEmitter.on(e -> {});
    assertSame(eventEmitter, result);
  }

  @Test
  void testChaining_clear() {
    SimpleEventEmitter<String> result = eventEmitter.clear();
    assertSame(eventEmitter, result);
  }

  @Test
  void testChaining_multipleCalls() {
    StringBuilder s = new StringBuilder();

    eventEmitter.on(e -> s.append("A")).on(e -> s.append("B")).clear().on(e -> s.append("C"));

    eventEmitter.emit("test");

    assertEquals("C", s.toString());
  }

  @Test
  void testEmit_withNullValue() {
    StringBuilder s = new StringBuilder();

    eventEmitter.on(e -> s.append(e == null ? "NULL" : e));

    eventEmitter.emit(null);

    assertEquals("NULL", s.toString());
  }

  @Test
  void testEmit_noArgOverload() {
    StringBuilder s = new StringBuilder();

    eventEmitter.on(() -> s.append("Called"));

    eventEmitter.emit();

    assertEquals("Called", s.toString());
  }

  @Test
  void testEmptyEmitter_emitDoesNothing() {
    // Should not throw exceptions
    eventEmitter.emit("test");
    eventEmitter.emit();
    eventEmitter.clear();
  }

  @Test
  void testAddSameListenerMultipleTimes() {
    int[] counter = new int[] {0};
    SimpleEventEmitter.Listener<String> listener = e -> counter[0]++;

    eventEmitter.on(listener);
    eventEmitter.on(listener);
    eventEmitter.on(listener);

    eventEmitter.emit("test");

    assertEquals(3, counter[0]);
  }
}
