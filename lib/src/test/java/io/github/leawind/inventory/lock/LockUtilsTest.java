package io.github.leawind.inventory.lock;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class LockUtilsTest {
  private Map<String, Integer> map;
  private ReadWriteLock readWriteLock;
  private Lock plainLock;

  @BeforeEach
  void setup() {
    map = new HashMap<>();
    readWriteLock = new ReentrantReadWriteLock();
    plainLock = new ReentrantLock();
  }

  @Test
  void testPlainLock() {
    try (var ignored = LockUtils.lock(plainLock)) {
      assertTrue(plainLock.tryLock());
    }
    assertTrue(plainLock.tryLock());
    plainLock.unlock();
  }

  @Test
  void testWriteLock() {
    try (var ignored = LockUtils.writeLock(readWriteLock)) {
      map.put("key", 123);
    }
    assertEquals(123, map.get("key"));
  }

  @Test
  void testReadLock() {
    map.put("key", 456);
    try (var ignored = LockUtils.readLock(readWriteLock)) {
      assertEquals(456, map.get("key"));
    }
  }

  @Test
  void testLockReleasedAfterException() {
    ReentrantLock lock = new ReentrantLock();
    assertThrows(RuntimeException.class, () -> {
      try (var ignored = LockUtils.lock(lock)) {
        throw new RuntimeException("test exception");
      }
    });
    assertTrue(lock.tryLock());
    lock.unlock();
  }

  @Test
  void testNestedLocks() {
    ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    try (var ignored = LockUtils.writeLock(rwLock)) {
      map.put("val", 789);
      try (var ignored2 = LockUtils.readLock(rwLock)) {
        assertEquals(789, map.get("val"));
      }
    }
    assertTrue(rwLock.writeLock().tryLock());
    rwLock.writeLock().unlock();
  }

  @Test
  void testConcurrentReaders() throws InterruptedException {
    int readerCount = 5;
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch endLatch = new CountDownLatch(readerCount);

    map.put("shared", 999);

    for (int i = 0; i < readerCount; i++) {
      new Thread(() -> {
        try {
          startLatch.await();
          try (var ignored = LockUtils.readLock(readWriteLock)) {
            assertEquals(999, map.get("shared"));
            Thread.sleep(50);
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        } finally {
          endLatch.countDown();
        }
      }).start();
    }

    startLatch.countDown();
    endLatch.await();
  }

  @Test
  void testWriteLockExclusion() throws InterruptedException {
    CountDownLatch writeHeld = new CountDownLatch(1);
    CountDownLatch readAttempted = new CountDownLatch(1);
    boolean[] readAcquired = {false};

    Thread writer = new Thread(() -> {
      try (var ignored = LockUtils.writeLock(readWriteLock)) {
        map.put("data", 111);
        writeHeld.countDown();
        Thread.sleep(200);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    });

    Thread reader = new Thread(() -> {
      try {
        writeHeld.await();
        readAcquired[0] = readWriteLock.readLock().tryLock();
        if (readAcquired[0]) readWriteLock.readLock().unlock();
        readAttempted.countDown();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    });

    writer.start();
    reader.start();

    readAttempted.await();
    assertFalse(readAcquired[0]);
    writer.join();

    assertTrue(readWriteLock.readLock().tryLock());
    readWriteLock.readLock().unlock();
  }
}
