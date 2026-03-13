package io.github.leawind.inventory.event;

import static org.junit.jupiter.api.Assertions.*;

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
    var s = new StringBuilder();

    eventEmitter.on(s::append);
    eventEmitter.emit("A");
    eventEmitter.emit("B");
    eventEmitter.emit("C");

    assertEquals("ABC", s.toString());
  }

  @Test
  void testOnce_shouldRemoveAfterFirstEmit() {
    var s = new StringBuilder();

    eventEmitter.once(s::append);

    eventEmitter.emit("A");
    eventEmitter.emit("B");
    eventEmitter.emit("C");

    assertEquals("A", s.toString());
  }

  @Test
  void testNoArgListener_on() {
    var s = new StringBuilder();

    eventEmitter.on(() -> s.append("X"));

    eventEmitter.emit("ignored");
    eventEmitter.emit("ignored");

    assertEquals("XX", s.toString());
  }

  @Test
  void testNoArgListener_once() {
    var s = new StringBuilder();

    eventEmitter.once(() -> s.append("Y"));

    eventEmitter.emit("ignored");
    eventEmitter.emit("ignored");

    assertEquals("Y", s.toString());
  }

  @Test
  void testOff_shouldRemoveListener() {
    var s = new StringBuilder();

    eventEmitter.on(s::append);

    eventEmitter.emit("A");
    eventEmitter.off();
    eventEmitter.emit("B");

    assertEquals("A", s.toString());
  }

  @Test
  void testClear_shouldRemoveListener() {
    var s = new StringBuilder();

    eventEmitter.on(s::append);

    eventEmitter.emit("A");
    eventEmitter.clear();
    eventEmitter.emit("B");

    assertEquals("A", s.toString());
  }

  @Test
  void testGetListener_shouldReturnCorrectListener() {
    SimpleEventEmitter.Listener<String> listener = (e) -> {};

    eventEmitter.on(listener);

    assertSame(listener, eventEmitter.getListener());
  }

  @Test
  void testGetListener_afterClear_shouldReturnNull() {
    SimpleEventEmitter.Listener<String> listener = (e) -> {};

    eventEmitter.on(listener);
    eventEmitter.clear();

    assertNull(eventEmitter.getListener());
  }

  @Test
  void testGetListener_afterOff_shouldReturnNull() {
    SimpleEventEmitter.Listener<String> listener = (e) -> {};

    eventEmitter.on(listener);
    eventEmitter.off();

    assertNull(eventEmitter.getListener());
  }

  @Test
  void testGetListener_afterOnceAutoRemove_shouldReturnNull() {
    SimpleEventEmitter.Listener<String> listener = (e) -> {};

    eventEmitter.once(listener);
    eventEmitter.emit("test");

    assertNull(eventEmitter.getListener());
  }

  @Test
  void testChaining_on() {
    var result = eventEmitter.on(e -> {});

    assertSame(eventEmitter, result);
  }

  @Test
  void testChaining_once() {
    var result = eventEmitter.once(e -> {});

    assertSame(eventEmitter, result);
  }

  @Test
  void testChaining_off() {
    var result = eventEmitter.off();

    assertSame(eventEmitter, result);
  }

  @Test
  void testChaining_clear() {
    var result = eventEmitter.clear();

    assertSame(eventEmitter, result);
  }

  @Test
  void testReplaceListener_byCallingOnAgain() {
    var s = new StringBuilder();

    eventEmitter.on(e -> s.append("A"));
    eventEmitter.on(e -> s.append("B"));

    eventEmitter.emit("test");

    assertEquals("B", s.toString());
  }

  @Test
  void testReplaceListener_byCallingOnceAgain() {
    var s = new StringBuilder();

    eventEmitter.once(e -> s.append("A"));
    eventEmitter.once(e -> s.append("B"));

    eventEmitter.emit("test");

    assertEquals("B", s.toString());
  }

  @Test
  void testEmit_withNullValue() {
    var s = new StringBuilder();

    eventEmitter.on(e -> s.append(e == null ? "NULL" : e));

    eventEmitter.emit(null);

    assertEquals("NULL", s.toString());
  }

  @Test
  void testMultipleOnce_callsShouldOnlyKeepLast() {
    var s = new StringBuilder();

    eventEmitter.once(e -> s.append("1")).once(e -> s.append("2")).once(e -> s.append("3"));

    eventEmitter.emit("test");

    assertEquals("3", s.toString());
  }

  @Test
  void testSwitch_fromOnceToOn() {
    var s = new StringBuilder();

    eventEmitter.once(e -> s.append("1"));
    eventEmitter.on(e -> s.append("2"));

    eventEmitter.emit("A");
    eventEmitter.emit("B");

    assertEquals("22", s.toString());
  }

  @Test
  void testSwitch_fromOnToOnce() {
    var s = new StringBuilder();

    eventEmitter.on(e -> s.append("1"));
    eventEmitter.once(e -> s.append("2"));

    eventEmitter.emit("A");
    eventEmitter.emit("B");

    assertEquals("2", s.toString());
  }

  @Test
  void testListenerHelper_method() {
    SimpleEventEmitter.Listener<String> listener = eventEmitter.listener(() -> {});

    assertNotNull(listener);
  }
}
