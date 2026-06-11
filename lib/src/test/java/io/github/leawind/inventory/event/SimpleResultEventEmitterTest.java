package io.github.leawind.inventory.event;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SimpleResultEventEmitterTest {
  private SimpleResultEventEmitter<String, String> emitter;

  @BeforeEach
  void setUp() {
    emitter = new SimpleResultEventEmitter<>();
  }

  /** Helper: consume the lazy stream so listeners execute for side-effect-only tests. */
  private static <T> void drain(Stream<T> stream) {
    stream.forEach(x -> {});
  }

  @Test
  void testOn_and_emit() {
    var s = new StringBuilder();

    emitter.on(
        e -> {
          s.append(e);
          return e;
        });
    drain(emitter.emit("A"));
    drain(emitter.emit("B"));
    drain(emitter.emit("C"));

    assertEquals("ABC", s.toString());
  }

  @Test
  void testMultipleListeners_allShouldBeTriggered() {
    var s = new StringBuilder();

    emitter.on(
        e -> {
          s.append("1");
          return "1";
        });
    emitter.on(
        e -> {
          s.append("2");
          return "2";
        });
    emitter.on(
        e -> {
          s.append("3");
          return "3";
        });

    drain(emitter.emit("test"));

    assertEquals("123", s.toString());
  }

  @Test
  void testMultipleListeners_orderPreserved() {
    var result = new ArrayList<String>();

    emitter.on(
        e -> {
          result.add("First");
          return "First";
        });
    emitter.on(
        e -> {
          result.add("Second");
          return "Second";
        });
    emitter.on(
        e -> {
          result.add("Third");
          return "Third";
        });

    drain(emitter.emit("test"));

    assertEquals(List.of("First", "Second", "Third"), result);
  }

  @Test
  void testNoArgListener_on() {
    var s = new StringBuilder();

    emitter.on(
        () -> {
          s.append("X");
          return "X";
        });

    drain(emitter.emit("ignored"));
    drain(emitter.emit("ignored"));

    assertEquals("XX", s.toString());
  }

  @Test
  void testMixedListenerTypes_bothTriggered() {
    var s = new StringBuilder();

    emitter.on(
        e -> {
          s.append(e);
          return e;
        });
    emitter.on(
        () -> {
          s.append("-");
          return "-";
        });

    drain(emitter.emit("A"));
    drain(emitter.emit("B"));

    assertEquals("A-B-", s.toString());
  }

  @Test
  void testClear_shouldRemoveAllListeners() {
    var s = new StringBuilder();

    emitter.on(
        e -> {
          s.append(e);
          return e;
        });
    emitter.on(
        () -> {
          s.append("X");
          return "X";
        });

    drain(emitter.emit("A"));
    emitter.clear();
    drain(emitter.emit("B"));

    assertEquals("AX", s.toString());
  }

  @Test
  void testConstructor_withExistingListeners() {
    var s = new StringBuilder();

    var listeners = new ArrayList<SimpleResultEventEmitter.Listener<String, String>>();
    listeners.add(
        e -> {
          s.append("1");
          return "1";
        });
    listeners.add(
        e -> {
          s.append("2");
          return "2";
        });

    var customEmitter = new SimpleResultEventEmitter<>(listeners);
    drain(customEmitter.emit("test"));

    assertEquals("12", s.toString());
  }

  @Test
  void testChaining_on() {
    var result = emitter.on(e -> "ignored");
    assertSame(emitter, result);
  }

  @Test
  void testChaining_onNoArg() {
    var result = emitter.on(() -> "ignored");
    assertSame(emitter, result);
  }

  @Test
  void testChaining_clear() {
    var result = emitter.clear();
    assertSame(emitter, result);
  }

  @Test
  void testChaining_multipleCalls() {
    var s = new StringBuilder();

    emitter
        .on(
            e -> {
              s.append("A");
              return "A";
            })
        .on(
            e -> {
              s.append("B");
              return "B";
            })
        .clear()
        .on(
            e -> {
              s.append("C");
              return "C";
            });

    drain(emitter.emit("test"));

    assertEquals("C", s.toString());
  }

  @Test
  void testEmit_withNullValue() {
    var s = new StringBuilder();

    emitter.on(
        e -> {
          s.append(e == null ? "NULL" : e);
          return e == null ? "NULL" : e;
        });

    drain(emitter.emit(null));

    assertEquals("NULL", s.toString());
  }

  @Test
  void testEmit_noArgOverload_emitsNull() {
    var s = new StringBuilder();

    emitter.on(
        e -> {
          s.append(e == null ? "NULL" : e);
          return e == null ? "NULL" : e;
        });

    // emit() delegates to emit(null), but the returned Stream is discarded (void).
    // This means no listeners fire when using the no-arg emit().
    emitter.emit();

    assertEquals("", s.toString(), "emit() discards the lazy stream, so listeners do not execute");
  }

  @Test
  void testEmptyEmitter_emitDoesNothing() {
    // Should not throw exceptions
    drain(emitter.emit("test"));
    emitter.emit();
    emitter.clear();
  }

  @Test
  void testEmptyEmitter_emitReturnsEmptyStream() {
    var results = emitter.emit("test").toList();
    assertTrue(results.isEmpty());
  }

  @Test
  void testAddSameListenerMultipleTimes() {
    var counter = new int[] {0};
    SimpleResultEventEmitter.Listener<String, String> listener =
        e -> {
          counter[0]++;
          return String.valueOf(counter[0]);
        };

    emitter.on(listener);
    emitter.on(listener);
    emitter.on(listener);

    drain(emitter.emit("test"));

    assertEquals(3, counter[0]);
  }

  @Test
  void testEmit_returnsStreamOfResults() {
    emitter.on(e -> "Result1");
    emitter.on(e -> "Result2");
    emitter.on(e -> "Result3");

    var results = emitter.emit("test").toList();

    assertEquals(List.of("Result1", "Result2", "Result3"), results);
  }

  @Test
  void testEmit_returnsResultsInRegistrationOrder() {
    emitter.on(e -> "First");
    emitter.on(e -> "Second");
    emitter.on(e -> "Third");

    var results = emitter.emit("test").toList();

    assertEquals(List.of("First", "Second", "Third"), results);
  }

  @Test
  void testListener_receivesCorrectEventValue() {
    var captured = new ArrayList<String>();

    emitter.on(
        e -> {
          captured.add(e);
          return e;
        });

    drain(emitter.emit("Hello"));
    drain(emitter.emit("World"));

    assertEquals(List.of("Hello", "World"), captured);
  }

  @Test
  void testReturnTypeCanBeDifferent() {
    var emitterInt = new SimpleResultEventEmitter<String, Integer>();

    emitterInt.on(String::length);
    emitterInt.on(e -> e.length() * 2);

    var results = emitterInt.emit("Hi").toList();

    assertEquals(List.of(2, 4), results);
  }

  @Test
  void testNoArgListener_defaultOnEventDelegatesToOn() {
    var invoked = new boolean[] {false};

    // Create a NoArg listener that only overrides on(); the default on(event) delegates to on()
    emitter.on(
        () -> {
          invoked[0] = true;
          return "no-arg";
        });

    drain(emitter.emit("any-event"));

    assertTrue(invoked[0], "NoArg.on(event) should delegate to on()");
  }

  @Test
  void testNoArgListener_overrideBothOnAndOnEvent() {
    var receivedEvent = new ArrayList<String>();

    emitter.on(
        new SimpleResultEventEmitter.Listener.NoArg<>() {
          @Override
          public String on() {
            return "no-arg";
          }

          @Override
          public String on(String event) {
            receivedEvent.add(event);
            return "with-arg";
          }
        });

    drain(emitter.emit("test-event"));

    assertEquals(
        List.of("test-event"),
        receivedEvent,
        "Overridden on(String) takes precedence over default on(String)");
  }

  @Test
  void testReturnTypeIsVoid() {
    var emitterVoid = new SimpleResultEventEmitter<String, Void>();
    var executed = new boolean[] {false};

    emitterVoid.on(
        e -> {
          executed[0] = true;
          return null;
        });
    emitterVoid.on(
        () -> {
          executed[0] = true;
          return null;
        });

    var results = emitterVoid.emit("test").toList();

    assertEquals(Arrays.asList(null, null), results);
    assertTrue(executed[0]);
  }

  @Test
  void testEmit_streamCanBeUsedLazily() {
    var executed = new boolean[] {false};

    emitter.on(
        e -> {
          executed[0] = true;
          return "Executed";
        });

    emitter.emit("test");
    assertFalse(executed[0], "Stream should be lazy, not executed yet");
  }

  @Test
  void testEmit_streamCanBeUsedLazily2() {
    var executed = new boolean[] {false};

    emitter.on(
        e -> {
          executed[0] = true;
          return "Executed";
        });

    var results = emitter.emit("test").toList();

    assertEquals(List.of("Executed"), results);
    assertTrue(executed[0], "Stream should execute when terminal operation called");
  }
}
