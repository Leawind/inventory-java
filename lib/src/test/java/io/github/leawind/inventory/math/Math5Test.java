package io.github.leawind.inventory.math;

import static io.github.leawind.inventory.math.Math5.lerp;
import static io.github.leawind.inventory.math.Math5.lerpDegrees;
import static io.github.leawind.inventory.math.Math5.lerpRadians;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

public class Math5Test {
  static final double DELTA = 1e-5;

  @Test
  void testLerp() {
    assertThat(lerp(0, 100, 0.5), equalTo(50));
    assertThat(lerp(0L, 100L, 0.5f), equalTo(50L));
    assertThat(lerp(0f, 1f, 0.5), equalTo(0.5f));
    assertThat(lerp(0d, 1d, 0.5), equalTo(0.5d));
  }

  @Test
  void testLerpDegrees() {
    for (int i = -10; i < 10; i++) {
      var ofs = i * 360;

      assertThat(lerpDegrees(0, 45 + ofs, 0.5), closeTo(22.5, DELTA));
      assertThat(lerpDegrees(0, 90 + ofs, 0.5), closeTo(45, DELTA));

      assertThat(lerpDegrees(0, ofs, 0.5), closeTo(0, DELTA));
      assertThat(lerpDegrees(0, 360 + ofs, 0.5), closeTo(0, DELTA));

      assertThat(lerpDegrees(0, 180 + ofs, 0.5), anyOf(closeTo(90, DELTA), closeTo(270, DELTA)));

      assertThat(lerpDegrees(0, 225 + ofs, 0.5), closeTo(292.5, DELTA));
      assertThat(lerpDegrees(0, 270 + ofs, 0.5), closeTo(315, DELTA));
    }
  }

  @Test
  public void testLerpRadians() {

    for (int i = -10; i < 10; i++) {
      double ofs = i * 2 * Math.PI;

      assertThat(lerpRadians(0, Math.PI / 4 + ofs, 0.5), closeTo(Math.PI / 8, DELTA));
      assertThat(lerpRadians(0, Math.PI / 2 + ofs, 0.5), closeTo(Math.PI / 4, DELTA));

      assertThat(lerpRadians(0, ofs, 0.5), closeTo(0, DELTA));
      assertThat(lerpRadians(0, Math.PI * 2 + ofs, 0.5), closeTo(0, DELTA));

      assertThat(
          lerpRadians(0, Math.PI + ofs, 0.5),
          anyOf(closeTo(Math.PI / 2, DELTA), closeTo(Math.PI * 3 / 2, DELTA)));

      assertThat(lerpRadians(0, Math.PI * 5 / 4 + ofs, 0.5), closeTo(Math.PI * 13 / 8, DELTA));
      assertThat(lerpRadians(0, Math.PI * 3 / 2 + ofs, 0.5), closeTo(Math.PI * 7 / 4, DELTA));
    }
  }
}
