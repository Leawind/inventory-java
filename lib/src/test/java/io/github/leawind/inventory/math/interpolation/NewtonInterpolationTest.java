package io.github.leawind.inventory.math.interpolation;

import org.junit.jupiter.api.Test;

public class NewtonInterpolationTest {
  @Test
  void test() {
    var it =
        new NewtonInterpolation() //
            .add(0, 0d)
            .add(0.25, 0.1d)
            .add(0.5, 0.5d)
            .add(0.75, 0.9d)
            .add(1, 1d);

    InterpolationTest.showFigureAndAwait(0, 1, 1024, it::interpolate);
  }
}
