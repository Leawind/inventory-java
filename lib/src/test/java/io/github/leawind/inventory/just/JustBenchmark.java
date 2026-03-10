package io.github.leawind.inventory.just;

import java.util.ArrayList;
import java.util.List;
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
public class JustBenchmark {
  private final Random random = new Random(12138);

  @Param({"16"})
  private int log2Size;

  @Param({"0", "0.25", "0.5", "1.0"})
  private float errorRate;

  private float[] array;

  @Setup
  public void setup() {
    array = new float[1 << log2Size];
    for (int i = 0; i < array.length; i++) {
      array[i] = random.nextFloat();
    }
  }

  @Benchmark
  public void benchmarkJust(Blackhole bh) {
    var summer = new Summer();
    justProcess(summer, array);
    bh.consume(summer);
  }

  @Benchmark
  public void benchmarkTryCatch(Blackhole bh) {
    var summer = new Summer();
    tryCatchProcess(summer, array);
    bh.consume(summer);
  }

  Result<Float, Float> justProcess(float value) {
    if (value < errorRate) {
      return Result.ok(value);
    } else {
      return Result.err(value);
    }
  }

  void justProcess(Summer summer, float[] array) {
    for (float v : array) {
      switch (justProcess(v)) {
        case Result.Ok<Float, ?> ok -> summer.sum += ok.unwrap();
        case Result.Err<?, Float> err -> summer.errors.add(err.unwrapErr());
      }
    }
  }

  float tryCatchProcess(float value) throws CustomException {
    if (value < errorRate) {
      return value;
    }
    throw new CustomException(value);
  }

  void tryCatchProcess(Summer summer, float[] array) {
    for (float v : array) {
      try {
        summer.sum += tryCatchProcess(v);
      } catch (CustomException e) {
        summer.errors.add(e.value);
      }
    }
  }

  static class CustomException extends Exception {
    float value;

    CustomException(float value) {
      super();
      this.value = value;
    }
  }

  static final class Summer {
    double sum = 0;
    List<Float> errors = new ArrayList<>();
  }

  public static void main(String[] args) throws InterruptedException {
    int size = 22;
    for (int i = 0; i < 16; i++) {
      var b1 = new JustBenchmark();
      b1.log2Size = size;
      b1.errorRate = 0.5f;
      b1.setup();
      b1.justProcess(new Summer(), b1.array);
      Thread.sleep(500);

      var b2 = new JustBenchmark();
      b2.log2Size = size;
      b2.errorRate = 0.5f;
      b2.setup();
      b2.tryCatchProcess(new Summer(), b2.array);
      Thread.sleep(1000);
    }
  }
}
