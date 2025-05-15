package io.github.leawind.inventory;

import static org.junit.jupiter.api.Assertions.*;

import io.github.leawind.inventory.delegate.Delegate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DelegateTest {
  private Delegate<Object> delegate;

  @BeforeEach
  void setUp() {
    delegate = new Delegate<>();
  }

  @Test
  void testDelegateName() {
    var delegate = new Delegate<>("TestDelegate");
    assertEquals("TestDelegate", delegate.name);

    var unnamedDelegate = new Delegate<>();
    assertEquals("Unnamed", unnamedDelegate.name);
  }

  @Test
  void testOnce() {
    var s = new StringBuilder();

    delegate
        .addListener(e -> s.append("A"))
        .addOnce(e -> s.append("B"))
        .addOnce(e -> s.append("C"))
        .setOnce("a key", e -> s.append("D"))
        .setOnce("a key", e -> s.append("E"));

    delegate.broadcast(null);
    delegate.broadcast(null);

    assertEquals("ABCEA", s.toString());
  }

  @Test
  void testPriority() {
    var s = new StringBuilder();

    delegate
        .addListener(e -> s.append('A'), 1)
        .addListener(e -> s.append('B'), 2)
        .addListener(e -> s.append('C'), 2)
        .addListener(e -> s.append('D'), 1);

    delegate.broadcast(null);
    assertEquals("BCAD", s.toString());
  }

  @Test
  void testRemoveListener() {
    var s = new StringBuilder();

    var listenerA = delegate.listener(e -> s.append("A"));
    var listenerB = delegate.listener(e -> s.append("B"));

    delegate.addListener(listenerA);
    delegate.addListener(listenerB, 4);

    delegate.setListener("alice", e -> s.append("C"));
    delegate.setListener("bob", e -> s.append("D"));
    delegate.setListener("alice", e -> s.append("E"));

    delegate.broadcast(null);
    assertEquals("BADE", s.toString());

    s.delete(0, s.length());

    delegate.removeListener(listenerA);
    delegate.removeListener("bob");

    delegate.broadcast(null);
    assertEquals("BE", s.toString());
  }

  @Test
  void testStopPropagation() {
    var s = new StringBuilder();

    delegate
        .addListener(
            e -> {
              s.append("A");
              e.stop();
            })
        .addListener(e -> s.append("B"));

    delegate.broadcast(null);
    assertEquals("A", s.toString());
  }

  @Test
  void testRemoveSelf() {
    var s = new StringBuilder();

    delegate
        .addListener(
            e -> {
              s.append("A");
              e.removeSelf();
            })
        .addListener(e -> s.append("B"));

    // First broadcast: both listeners execute, A removes itself
    delegate.broadcast("test");
    assertEquals("AB", s.toString());

    // Second broadcast: only B remains
    s.setLength(0);
    delegate.broadcast("test");
    assertEquals("B", s.toString());
  }

  @Test
  void testClear() {
    var s = new StringBuilder();

    delegate
        .addListener(e -> s.append("A"))
        .addListener(e -> s.append("B"))
        .clear()
        .addListener(e -> s.append("C"));

    delegate.broadcast("test");
    assertEquals("C", s.toString());
  }

  @Test
  void setListener_withNullKey_shouldThrowException() {
    assertThrows(IllegalArgumentException.class, () -> delegate.setListener(null, e -> {}, 0));
  }

  @Test
  void setListener_withNewKey_shouldAddHandler() {
    delegate.setListener("testKey", e -> {}, 1);
    assertTrue(delegate.containsListener("testKey"));
    assertFalse(delegate.containsListener("no such key"));
  }

  @Test
  void setListener_withExistingKey_shouldUpdateListener() {
    var s = new StringBuilder();

    delegate.setListener("testKey", e -> s.append("A")).setListener("testKey", e -> s.append("B"));

    delegate.broadcast(null);
    assertEquals("B", s.toString());
  }
}
