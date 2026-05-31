package io.github.leawind.inventory.math.interpolation;

import org.junit.jupiter.api.Test;

public class LinearInterpolationTest {
  @Test
  void test() {
    var it = new LinearInterpolation(0, 1);
    assert it.interpolate(-1) == 0;
    assert it.interpolate(0) == 0;
    assert it.interpolate(0.25) == 0.25;
    assert it.interpolate(0.5) == 0.5;
    assert it.interpolate(0.75) == 0.75;
    assert it.interpolate(1) == 1;
    assert it.interpolate(2) == 1;
  }

  @Test
  void test2() {
    var it =
        new LinearInterpolation() //
            .add(0, 0d)
            .add(-2, -1d)
            .add(2, -7d)
            .add(2, 2d);

    assert it.interpolate(-3) == -1;
    assert it.interpolate(-2) == -1;
    assert it.interpolate(-1) == -0.5;
    assert it.interpolate(0) == 0;
    assert it.interpolate(1) == 1;
    assert it.interpolate(2) == 2;
    assert it.interpolate(4) == 2;
  }
}
