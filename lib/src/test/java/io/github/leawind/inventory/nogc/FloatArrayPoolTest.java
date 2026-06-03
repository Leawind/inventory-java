package io.github.leawind.inventory.nogc;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FloatArrayPoolTest {

  private static final int POOL_CAPACITY = 10;

  private float[][] arrays;

  @BeforeEach
  void setUp() {
    arrays =
        new float[][] {
          new float[3], // index 0
          new float[4], // index 1
          new float[4], // index 2
          new float[5], // index 3
          new float[8], // index 4
          new float[9], // index 5
          new float[10], // index 6
          null,
          null,
          null
        };
  }

  @Nested
  class FindPopPositionTests {

    @Test
    void emptyPool_returnsMinusOne() {
      FloatArrayPool pool = new FloatArrayPool(arrays, 0);
      assertEquals(-1, pool.findPopPosition(1));
      assertEquals(-1, pool.findPopPosition(3));
      assertEquals(-1, pool.findPopPosition(100));
    }

    @Test
    void singleElementPool_targetWithinRange_returnsZero() {
      FloatArrayPool pool = new FloatArrayPool(arrays, 1);
      assertEquals(0, pool.findPopPosition(1));
      assertEquals(0, pool.findPopPosition(2));
      assertEquals(0, pool.findPopPosition(3));
    }

    @Test
    void singleElementPool_targetExceeds_returnsMinusOne() {
      FloatArrayPool pool = new FloatArrayPool(arrays, 1);
      assertEquals(-1, pool.findPopPosition(4));
      assertEquals(-1, pool.findPopPosition(5));
    }

    @Test
    void fullPool_variousTargets() {
      FloatArrayPool pool = new FloatArrayPool(arrays);
      assertEquals(0, pool.findPopPosition(1));
      assertEquals(0, pool.findPopPosition(2));
      assertEquals(0, pool.findPopPosition(3));

      // Lower bound: first index with length >= 4 is index 1
      assertEquals(1, pool.findPopPosition(4));

      assertEquals(3, pool.findPopPosition(5));
      assertEquals(4, pool.findPopPosition(6));
      assertEquals(4, pool.findPopPosition(7));
      assertEquals(4, pool.findPopPosition(8));
      assertEquals(5, pool.findPopPosition(9));
      assertEquals(6, pool.findPopPosition(10));
      assertEquals(-1, pool.findPopPosition(11));
    }
  }

  @Nested
  class FindInsertPositionTests {

    @Test
    void emptyPool_alwaysInsertAtZero() {
      FloatArrayPool pool = new FloatArrayPool(arrays, 0);
      assertEquals(0, pool.findInsertPosition(1));
      assertEquals(0, pool.findInsertPosition(5));
      assertEquals(0, pool.findInsertPosition(10));
    }

    @Test
    void singleElementPool_insertPositions() {
      FloatArrayPool pool = new FloatArrayPool(arrays, 1);
      assertEquals(0, pool.findInsertPosition(1));
      assertEquals(0, pool.findInsertPosition(2));
      assertEquals(1, pool.findInsertPosition(3));
      assertEquals(1, pool.findInsertPosition(4));
      assertEquals(1, pool.findInsertPosition(5));
    }

    @Test
    void fullPool_maintainAscendingOrder() {
      FloatArrayPool pool = new FloatArrayPool(arrays);
      assertEquals(0, pool.findInsertPosition(1));
      assertEquals(0, pool.findInsertPosition(2));
      assertEquals(1, pool.findInsertPosition(3));
      // Upper bound: insert after existing [4,4], so position 3
      assertEquals(3, pool.findInsertPosition(4));
      assertEquals(4, pool.findInsertPosition(5));
      assertEquals(4, pool.findInsertPosition(6));
      assertEquals(4, pool.findInsertPosition(7));
      assertEquals(5, pool.findInsertPosition(8));
      assertEquals(6, pool.findInsertPosition(9));
      assertEquals(7, pool.findInsertPosition(10));
      assertEquals(7, pool.findInsertPosition(11));
    }
  }

  @Nested
  class AcquireTests {

    @Test
    void acquire_exactMatch() {
      FloatArrayPool pool = new FloatArrayPool(arrays);
      float[] result = pool.acquire(3);
      assertNotNull(result);
      assertEquals(3, result.length);
    }

    @Test
    void acquire_bestFit() {
      FloatArrayPool pool = new FloatArrayPool(arrays);
      float[] result = pool.acquire(4);
      assertNotNull(result);
      assertEquals(4, result.length);
    }

    @Test
    void acquire_largerThanAvailable_returnsNewArray() {
      FloatArrayPool pool = new FloatArrayPool(arrays);
      float[] result = pool.acquire(15);
      assertNotNull(result);
      assertEquals(15, result.length);
    }

    @Test
    void acquire_sequenceMaintainsOrder() {
      FloatArrayPool pool = new FloatArrayPool(arrays);
      float[] first = pool.acquire(1);
      float[] second = pool.acquire(4);
      float[] third = pool.acquire(5);

      assertEquals(3, first.length);
      assertEquals(4, second.length);
      assertEquals(5, third.length);
    }

    @Test
    void acquire_multipleSameSize_differentInstances() {
      FloatArrayPool pool = new FloatArrayPool(arrays);
      float[] first = pool.acquire(4);
      float[] second = pool.acquire(4);

      assertEquals(4, first.length);
      assertEquals(4, second.length);
      assertNotSame(first, second);
    }
  }

  @Nested
  class ReleaseTests {

    @Test
    void release_thenAcquire_returnsSameInstance() {
      FloatArrayPool pool = new FloatArrayPool(new float[5][], 0);
      float[] original = new float[10];

      pool.release(original);
      float[] reused = pool.acquire(10);

      assertSame(original, reused);
    }

    @Test
    void release_multipleArrays_maintainsOrder() {
      FloatArrayPool pool = new FloatArrayPool(arrays, 5);
      float[] size6 = new float[6];
      float[] size7 = new float[7];

      pool.release(size7);
      pool.release(size6);

      assertEquals(6, pool.acquire(6).length);
      assertEquals(7, pool.acquire(7).length);
    }

    @Test
    void release_whenPoolFull_isIgnored() {
      FloatArrayPool pool = new FloatArrayPool(arrays);
      for (int i = 7; i < POOL_CAPACITY; i++) {
        pool.release(new float[i + 3]);
      }

      float[] extra = new float[100];
      pool.release(extra);

      float[] result = pool.acquire(50);
      assertEquals(50, result.length);
      assertNotSame(extra, result);
    }

    @Test
    void release_null_throwsException() {
      FloatArrayPool pool = new FloatArrayPool(arrays);
      assertThrows(NullPointerException.class, () -> pool.release(null));
    }
  }

  @Nested
  class LifecycleTests {

    @Test
    void lifecycle_acquireReleaseCycle() {
      FloatArrayPool pool = new FloatArrayPool(new float[5][], 0);
      float[] array = new float[5];

      pool.release(array);
      float[] acquired1 = pool.acquire(5);
      assertSame(array, acquired1);

      pool.release(acquired1);
      float[] acquired2 = pool.acquire(5);
      assertSame(array, acquired2);
    }

    @Test
    void lifecycle_exhaustThenRelease() {
      FloatArrayPool pool = new FloatArrayPool(arrays);

      List<float[]> acquired = new ArrayList<>();
      for (int size = 1; size <= 10; size++) {
        acquired.add(pool.acquire(size));
      }

      float[] newSize = pool.acquire(5);
      assertEquals(5, newSize.length);

      for (float[] orig : arrays) {
        assertNotSame(orig, newSize);
      }

      pool.release(newSize);
      float[] reused = pool.acquire(5);
      assertSame(newSize, reused);
    }
  }

  @Nested
  class ConcurrencyTests {

    @Test
    void concurrent_acquireRelease_noRaceCondition() throws InterruptedException {
      FloatArrayPool pool = new FloatArrayPool(100);
      int threadCount = 10;
      int iterations = 100;

      ExecutorService executor = Executors.newFixedThreadPool(threadCount);
      CountDownLatch latch = new CountDownLatch(threadCount);

      for (int t = 0; t < threadCount; t++) {
        executor.submit(
            () -> {
              try {
                for (int i = 0; i < iterations; i++) {
                  int size = (i % 10) + 1;
                  float[] arr = pool.acquire(size);
                  assertNotNull(arr);
                  assertTrue(arr.length >= size);
                  arr[0] = size;
                  pool.release(arr);
                }
              } finally {
                latch.countDown();
              }
            });
      }

      assertTrue(latch.await(10, TimeUnit.SECONDS), "Test timed out");
      executor.shutdown();
      assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
    }

    @Test
    void concurrent_stressTest() throws InterruptedException {
      FloatArrayPool pool = new FloatArrayPool(50);
      int threadCount = 20;

      ExecutorService executor = Executors.newFixedThreadPool(threadCount);
      CountDownLatch latch = new CountDownLatch(threadCount);

      for (int t = 0; t < threadCount; t++) {
        final int threadId = t;
        executor.submit(
            () -> {
              try {
                for (int i = 0; i < 50; i++) {
                  int size = (threadId + i) % 15 + 1;
                  float[] arr = pool.acquire(size);
                  assertNotNull(arr);
                  Arrays.fill(arr, threadId);
                  pool.release(arr);
                }
              } finally {
                latch.countDown();
              }
            });
      }

      assertTrue(latch.await(15, TimeUnit.SECONDS), "Stress test timed out");
      executor.shutdownNow();
    }
  }

  @Nested
  class EdgeCaseTests {

    @Test
    void acquire_zeroSize() {
      FloatArrayPool pool = new FloatArrayPool(arrays);
      float[] result = pool.acquire(0);
      assertNotNull(result);
      assertEquals(3, result.length);
    }

    @Test
    void acquire_negativeSize() {
      FloatArrayPool pool = new FloatArrayPool(arrays);
      float[] result = pool.acquire(-1);
      assertNotNull(result);
      assertTrue(true);
    }

    @Test
    void acquire_fromEmptyPool() {
      FloatArrayPool pool = new FloatArrayPool(new float[10][], 0);
      float[] result = pool.acquire(5);
      assertNotNull(result);
      assertEquals(5, result.length);
    }

    @Test
    void acquire_fromAllNullPool() {
      FloatArrayPool pool = new FloatArrayPool(new float[10][]);
      float[] result = pool.acquire(3);
      assertNotNull(result);
      assertEquals(3, result.length);
    }
  }
}
