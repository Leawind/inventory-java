package io.github.leawind.inventory.objectpool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class ObjectPoolTest {

  interface PoolFactory<T> {
    ObjectPool<T> create(int capacity);
  }

  static Stream<Arguments> providePools() {
    return Stream.of(
        Arguments.of((PoolFactory<Cat>) capacity -> new DequeObjectPool<>(Cat::new, capacity)),
        Arguments.of(
            (PoolFactory<Cat>)
                capacity -> StackObjectPool.builder(Cat::new).capacity(capacity).build()));
  }

  @ParameterizedTest
  @MethodSource("providePools")
  void testIdleCountInitiallyZero(PoolFactory<Cat> poolFactory) {
    ObjectPool<Cat> pool = poolFactory.create(8); // default capacity
    assertEquals(0, pool.idleCount());
  }

  @ParameterizedTest
  @MethodSource("providePools")
  void testIdleCountAfterMultipleRecycles(PoolFactory<Cat> poolFactory) {
    ObjectPool<Cat> pool = poolFactory.create(3);
    pool.release(new Cat());
    pool.release(new Cat());
    pool.release(new Cat());
    assertEquals(3, pool.idleCount());
  }

  @ParameterizedTest
  @MethodSource("providePools")
  void testAcquireWhenIdleCreatesNewObject(PoolFactory<Cat> poolFactory) {
    ObjectPool<Cat> pool = poolFactory.create(8); // default capacity
    Cat cat = pool.acquire();
    assertNotNull(cat);
    assertEquals(-1, cat.id);
  }

  @ParameterizedTest
  @MethodSource("providePools")
  void testAcquireWhenPoolNotEmpty(PoolFactory<Cat> poolFactory) {
    ObjectPool<Cat> pool = poolFactory.create(8).ensureIdle(2); // default capacity
    Cat cat = pool.acquire();
    assertNotNull(cat);
    assertEquals(1, pool.idleCount());
  }

  @ParameterizedTest
  @MethodSource("providePools")
  void testBorrowReleaseAcquireSequence(PoolFactory<Cat> poolFactory) {
    ObjectPool<Cat> pool = poolFactory.create(8); // default capacity

    Cat cat = pool.acquire();
    pool.release(cat);

    pool.acquire();
    assertEquals(0, pool.idleCount());
  }

  @ParameterizedTest
  @MethodSource("providePools")
  void testConcurrentAcquireAndRelease(PoolFactory<Cat> poolFactory) {
    ObjectPool<Cat> pool = poolFactory.create(8); // default capacity

    Cat cat1 = pool.acquire();
    assertNotNull(cat1);

    Cat cat2 = pool.acquire();
    assertNotNull(cat2);

    pool.release(cat1);
    pool.release(cat2);

    pool.acquire();
    assertEquals(1, pool.idleCount());
  }

  @ParameterizedTest
  @MethodSource("providePools")
  void testPoolWithCustomSizeConstructor(PoolFactory<Cat> poolFactory) {
    ObjectPool<Cat> pool = poolFactory.create(5);
    assertEquals(0, pool.idleCount());

    // Fill pool
    for (int i = 0; i < 5; i++) {
      pool.release(new Cat());
    }
    assertEquals(5, pool.idleCount());

    // Borrow all
    for (int i = 0; i < 5; i++) {
      pool.acquire();
    }
    assertEquals(0, pool.idleCount());

    // Recycle all
    for (int i = 0; i < 5; i++) {
      pool.release(new Cat());
    }
    assertEquals(5, pool.idleCount());
  }
}
