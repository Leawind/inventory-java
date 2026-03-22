package io.github.leawind.inventory.objectpool;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

@SuppressWarnings("unused")
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(value = 1)
@Warmup(iterations = 2, time = 2)
@Measurement(iterations = 3, time = 2)
public class ObjectPoolBenchmark {
  private final Random random = new Random(9527);

  @Param({"2", "8", "32", "128", "512"})
  private int poolSize;

  private ObjectPool<Cat> dequePool;
  private ObjectPool<Cat> stackPool;

  @Param({"32", "1024"})
  private int operationPairs;

  /** true means acquire, false means release */
  private boolean[] operations;

  private Queue<Cat> borrowed;

  @Setup
  public void setup() {
    dequePool = new DequeObjectPool<>(Cat::new).ensureIdle(poolSize);
    stackPool = new StackObjectPool<>(Cat::new).ensureIdle(poolSize);

    operations = new boolean[operationPairs * 2];
    borrowed = new ArrayDeque<>(operationPairs);

    generateValidSequence(operations, random);
  }

  @Benchmark
  public void benchmarkDequePool(Blackhole bh) {
    simulateOperations(bh, dequePool);
  }

  @Benchmark
  public void benchmarkStackPool(Blackhole bh) {
    simulateOperations(bh, stackPool);
  }

  private void simulateOperations(Blackhole bh, ObjectPool<Cat> pool) {
    for (boolean operation : operations) {
      if (operation) {
        borrowed.add(pool.acquire());
      } else {
        pool.release(borrowed.poll());
      }
    }
  }

  private static void generateValidSequence(boolean[] arr, Random random) {
    if (arr.length % 2 != 0) {
      throw new IllegalArgumentException("Array length must be even");
    }

    int pairs = arr.length / 2;

    // Remaining borrow and return operations
    int borrowLeft = pairs;
    int returnLeft = pairs;

    // How many times borrow is used more than return in the prefix of the sequence
    // Must be >= 0 at all times
    int balance = 0;

    for (int i = 0; i < arr.length; i++) {
      boolean canBorrow = borrowLeft > 0;
      boolean canReturn = returnLeft > 0 && balance > 0;

      if (canBorrow && canReturn) {
        if (random.nextBoolean()) {
          // borrow
          arr[i] = true;
          borrowLeft--;
          balance++;
        } else {
          // return
          arr[i] = false;
          returnLeft--;
          balance--;
        }
      } else if (canBorrow) {
        arr[i] = true;
        borrowLeft--;
        balance++;
      } else if (canReturn) {
        arr[i] = false;
        returnLeft--;
        balance--;
      }
    }
  }
}
