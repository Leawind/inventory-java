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
  void testAddAndRemoveListener() {
    var s = new StringBuilder();

    delegate.addListener(e -> s.append("A"));
    delegate.addListener(e -> s.append("B"), 4);

    delegate.addListener("alice", e -> s.append("C"));
    delegate.addListener("bob", e -> s.append("D"));
    delegate.addListener("alice", e -> s.append("E"));

    delegate.broadcast(null);
    assertEquals("BADE", s.toString());
  }

  @Test
  void testRemoveListener() {
    var s = new StringBuilder();

    var listener = delegate.listener(e -> s.append(e.data));

    delegate
        .addListener(e -> s.append('A'))
        .addListener(listener)
        .addListener(e -> s.append('B'))
        .removeListener(listener)
        .addListener(e -> s.append('C'));

    delegate.broadcast("test");
    assertEquals("ABC", s.toString());
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
  void addListener_withNullKey_shouldThrowException() {
    assertThrows(IllegalArgumentException.class, () -> delegate.addListener(null, e -> {}, 0));
  }

  @Test
  void addListener_withNewKey_shouldAddHandler() {
    delegate.addListener("testKey", e -> {}, 1);
    assertTrue(delegate.containsListener("testKey"));
    assertFalse(delegate.containsListener("no such key"));
  }

  @Test
  void addListener_withExistingKey_shouldUpdateListener() {
    var s = new StringBuilder();

    delegate.addListener("testKey", e -> s.append("A")).addListener("testKey", e -> s.append("B"));

    delegate.broadcast(null);
    assertEquals("B", s.toString());
  }
}
