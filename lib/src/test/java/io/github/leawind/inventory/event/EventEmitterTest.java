package io.github.leawind.inventory.event;

import static org.junit.jupiter.api.Assertions.*;

import io.github.leawind.inventory.dep.CyclicDependencyException;
import io.github.leawind.inventory.dep.Dependencies;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class EventEmitterTest {

  @Test
  void shouldCreateInstanceViaFactory() {
    EventEmitter.Owned<String, String> emitter = EventEmitter.create();
    assertNotNull(emitter);
  }

  @Test
  void shouldDispatchEventToRegisteredListeners() {
    EventEmitter.Owned<String, String> emitter = EventEmitter.create();
    List<String> received = new ArrayList<>();

    emitter.register("A", (event, ctrl) -> received.add("A:" + event));
    emitter.register("B", (event, ctrl) -> received.add("B:" + event));

    emitter.emit("hello");

    assertEquals(List.of("A:hello", "B:hello"), received);
  }

  @Test
  void shouldDispatchInDependencyOrder() {
    EventEmitter.Owned<String, String> emitter = EventEmitter.create();
    List<String> order = new ArrayList<>();

    emitter.register("A", (e, c) -> order.add("A"), Dependencies.<String>create().after("B"));
    emitter.register("B", (e, c) -> order.add("B"));
    emitter.register("C", (e, c) -> order.add("C"), Dependencies.<String>create().after("A"));

    emitter.emit("x");

    assertEquals(List.of("B", "A", "C"), order);
  }

  @Test
  void shouldStopPropagationWhenControlStopCalled() {
    EventEmitter.Owned<String, String> emitter = EventEmitter.create();
    List<String> received = new ArrayList<>();

    emitter.register(
        "A",
        (event, ctrl) -> {
          received.add("A");
          ctrl.stop();
        });
    emitter.register("B", (event, ctrl) -> received.add("B"));

    emitter.emit("x");

    assertEquals(List.of("A"), received);
  }

  @Test
  void shouldUnregisterListenerWhenControlUnregisterCalled() {
    EventEmitter.Owned<String, String> emitter = EventEmitter.create();
    List<String> received = new ArrayList<>();

    emitter.register(
        "A",
        (event, ctrl) -> {
          received.add("A");
          ctrl.unregister();
        });
    emitter.register("B", (event, ctrl) -> received.add("B"));

    emitter.emit("1");
    assertEquals(List.of("A", "B"), received);

    received.clear();
    emitter.emit("2");
    assertEquals(List.of("B"), received);
  }

  @Test
  void shouldAllowMultipleDispatches() {
    EventEmitter.Owned<String, String> emitter = EventEmitter.create();
    List<String> received = new ArrayList<>();

    emitter.register("A", (event, ctrl) -> received.add(event));

    emitter.emit("1");
    emitter.emit("2");
    emitter.emit("3");

    assertEquals(List.of("1", "2", "3"), received);
  }

  @Test
  void shouldNotThrowOnDuplicateKey() {
    EventEmitter.Owned<String, String> emitter = EventEmitter.create();
    emitter.register("A", (event, ctrl) -> {});
    emitter.register("A", (event, ctrl) -> {});
  }

  @Test
  void shouldStreamListenersInOrder() {
    EventEmitter.Owned<String, String> emitter = EventEmitter.create();

    emitter.register("A", (e, c) -> {});
    emitter.register("B", (e, c) -> {}, Dependencies.<String>create().after("A"));

    List<BiListener<String, EventEmitter.Control>> listeners = emitter.stream().toList();

    assertEquals(2, listeners.size());
  }

  @Test
  void shouldPassEventToAllListeners() {
    EventEmitter.Owned<Integer, String> emitter = EventEmitter.create();
    List<Integer> values = new ArrayList<>();

    emitter.register("A", (e, c) -> values.add(e * 1));
    emitter.register("B", (e, c) -> values.add(e * 2));
    emitter.register("C", (e, c) -> values.add(e * 3));

    emitter.emit(5);

    assertEquals(List.of(5, 10, 15), values);
  }

  @Test
  void shouldHandleStopAndUnregisterTogether() {
    EventEmitter.Owned<String, String> emitter = EventEmitter.create();
    List<String> received = new ArrayList<>();

    emitter.register(
        "A",
        (event, ctrl) -> {
          received.add("A");
          ctrl.stop();
          ctrl.unregister();
        });
    emitter.register("B", (event, ctrl) -> received.add("B"));

    emitter.emit("1");
    assertEquals(List.of("A"), received);

    received.clear();
    emitter.emit("2");
    assertEquals(List.of("B"), received);
  }

  @Test
  void shouldResolveComplexDependencies() {
    EventEmitter.Owned<String, String> emitter = EventEmitter.create();
    List<String> order = new ArrayList<>();

    emitter.register("C", (e, c) -> order.add("C"), Dependencies.<String>create().after("B"));
    emitter.register("A", (e, c) -> order.add("A"), Dependencies.<String>create().before("B"));
    emitter.register("B", (e, c) -> order.add("B"));
    emitter.register(
        "D", (e, c) -> order.add("D"), Dependencies.<String>create().after("A").before("C"));

    emitter.emit("x");

    int indexA = order.indexOf("A");
    int indexB = order.indexOf("B");
    int indexC = order.indexOf("C");
    int indexD = order.indexOf("D");

    assertTrue(indexA < indexB);
    assertTrue(indexB < indexC);
    assertTrue(indexA < indexD);
    assertTrue(indexD < indexC);
  }

  @Test
  void shouldUnregisterMultipleListenersIndependently() {
    EventEmitter.Owned<String, String> emitter = EventEmitter.create();
    List<String> received = new ArrayList<>();

    emitter.register(
        "A",
        (event, ctrl) -> {
          received.add("A");
          ctrl.unregister();
        });
    emitter.register("B", (event, ctrl) -> received.add("B"));
    emitter.register(
        "C",
        (event, ctrl) -> {
          received.add("C");
          ctrl.unregister();
        });

    emitter.emit("1");
    assertEquals(List.of("A", "B", "C"), received);

    received.clear();
    emitter.emit("2");
    assertEquals(List.of("B"), received);
  }

  @Test
  void shouldHandleEmptyDispatcher() {
    EventEmitter.Owned<String, String> emitter = EventEmitter.create();

    assertDoesNotThrow(() -> emitter.emit("x"));
  }

  // region on

  @Test
  void onShouldChainMultipleRegistrations() {
    EventEmitter.Owned<String, String> emitter = EventEmitter.create();
    List<String> received = new ArrayList<>();

    emitter.on("A", (e, c) -> received.add("A"));
    emitter.on("B", (e, c) -> received.add("B"));
    emitter.on("C", (e, c) -> received.add("C"));

    emitter.emit("x");

    assertEquals(List.of("A", "B", "C"), received);
  }

  @Test
  void onWithOneArgShouldReceiveEventOnly() {
    EventEmitter.Owned<String, String> emitter = EventEmitter.create();
    List<String> received = new ArrayList<>();

    emitter.on("A", (String e) -> received.add(e));

    emitter.emit("hello");

    assertEquals(List.of("hello"), received);
  }

  @Test
  void onWithNoArgShouldReceiveNoParameters() {
    EventEmitter.Owned<String, String> emitter = EventEmitter.create();
    List<String> received = new ArrayList<>();

    emitter.on("A", () -> received.add("called"));

    emitter.emit("hello");

    assertEquals(List.of("called"), received);
  }

  // endregion

  // region once

  @Test
  void shouldFireOnceThenAutoUnregister() {
    EventEmitter.Owned<String, String> emitter = EventEmitter.create();
    List<String> received = new ArrayList<>();

    emitter.once("A", (e, c) -> received.add(e));

    emitter.emit("1");
    emitter.emit("2");
    emitter.emit("3");

    assertEquals(List.of("1"), received);
  }

  @Test
  void onceWithNoArgShouldFireOnce() {
    EventEmitter.Owned<String, String> emitter = EventEmitter.create();
    int[] count = {0};

    emitter.once("A", () -> count[0]++);

    emitter.emit("x");
    emitter.emit("y");

    assertEquals(1, count[0]);
  }

  @Test
  void onceWithOneArgShouldFireOnce() {
    EventEmitter.Owned<String, String> emitter = EventEmitter.create();
    List<String> received = new ArrayList<>();

    emitter.once("A", (String e) -> received.add(e));

    emitter.emit("1");
    emitter.emit("2");

    assertEquals(List.of("1"), received);
  }

  @Test
  void onceShouldNotAffectOtherListeners() {
    EventEmitter.Owned<String, String> emitter = EventEmitter.create();
    List<String> received = new ArrayList<>();

    emitter.once("A", (e, c) -> received.add("A"));
    emitter.register("B", (e, c) -> received.add("B"));

    emitter.emit("1");
    emitter.emit("2");

    assertEquals(List.of("A", "B", "B"), received);
  }

  @Test
  void shouldStopOnlyPropagationNotUnregister() {
    EventEmitter.Owned<String, String> emitter = EventEmitter.create();
    List<String> received = new ArrayList<>();
    boolean[] stopped = {false};

    emitter.register(
        "A",
        (e, c) -> {
          received.add("A");
          if (!stopped[0]) {
            stopped[0] = true;
            c.stop();
          }
        });
    emitter.register("B", (e, c) -> received.add("B"));

    emitter.emit("1");
    assertEquals(List.of("A"), received);

    received.clear();
    emitter.emit("2");
    assertEquals(List.of("A", "B"), received);
  }

  @Test
  void shouldUnregisterWithoutStopping() {
    EventEmitter.Owned<String, String> emitter = EventEmitter.create();
    List<String> received = new ArrayList<>();

    emitter.register(
        "A",
        (e, c) -> {
          received.add("A");
          c.unregister();
        });
    emitter.register("B", (e, c) -> received.add("B"));

    emitter.emit("1");
    assertEquals(List.of("A", "B"), received);

    received.clear();
    emitter.emit("2");
    assertEquals(List.of("B"), received);
  }

  @Test
  void onceWithStopShouldStopAndAutoUnregister() {
    EventEmitter.Owned<String, String> emitter = EventEmitter.create();
    List<String> received = new ArrayList<>();

    emitter.once(
        "A",
        (e, c) -> {
          received.add("A");
          c.stop();
        });
    emitter.register("B", (e, c) -> received.add("B"));

    emitter.emit("1");
    assertEquals(List.of("A"), received);

    received.clear();
    emitter.emit("2");
    assertEquals(List.of("B"), received);
  }

  @Test
  void stopInMiddleShouldPreventLaterListeners() {
    EventEmitter.Owned<String, String> emitter = EventEmitter.create();
    List<String> received = new ArrayList<>();

    emitter.register("A", (e, c) -> received.add("A"));
    emitter.register(
        "B",
        (e, c) -> {
          received.add("B");
          c.stop();
        });
    emitter.register("C", (e, c) -> received.add("C"));

    emitter.emit("x");

    assertEquals(List.of("A", "B"), received);
  }

  // endregion

  // region sort

  @Test
  void shouldDetectCyclicDependencyOnSort() {
    EventEmitter.Owned<String, String> emitter = EventEmitter.create();
    emitter.register("A", (e, c) -> {}, Dependencies.<String>create().after("B"));
    emitter.register("B", (e, c) -> {}, Dependencies.<String>create().after("A"));

    assertThrows(CyclicDependencyException.class, emitter::sort);
  }

  // endregion
}
