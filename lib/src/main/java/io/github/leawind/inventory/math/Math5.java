package io.github.leawind.inventory.math;

public class Math5 {

  /**
   * Computes the positive remainder of dividing {@code f1} by {@code f2}.
   *
   * @param f1 the dividend
   * @param f2 the divisor
   * @return the positive remainder when {@code f1} is divided by {@code f2}
   */
  public static double remainder(double f1, double f2) {
    var result = Math.IEEEremainder(f1, f2);
    return result >= 0 ? result : result + f2;
  }

  /**
   * Linearly interpolates between two integer values based on a parameter {@code t}.
   *
   * @param from the starting value
   * @param to the ending value
   * @param t the interpolation factor (between 0 and 1)
   * @return the interpolated value as an integer
   */
  public static int lerp(int from, int to, double t) {
    return from + (int) Math.round(t * (to - from));
  }

  /**
   * Linearly interpolates between two long values based on a parameter {@code t}.
   *
   * @param from the starting value
   * @param to the ending value
   * @param t the interpolation factor (between 0 and 1)
   * @return the interpolated value as a long
   */
  public static long lerp(long from, long to, double t) {
    return from + Math.round(t * (to - from));
  }

  /**
   * Linearly interpolates between two float values based on a parameter {@code t}.
   *
   * @param from the starting value
   * @param to the ending value
   * @param t the interpolation factor (between 0 and 1)
   * @return the interpolated value as a float
   */
  public static float lerp(float from, float to, double t) {
    return (float) (from + t * (to - from));
  }

  /**
   * Linearly interpolates between two double values based on a parameter {@code t}.
   *
   * @param from the starting value
   * @param to the ending value
   * @param t the interpolation factor (between 0 and 1)
   * @return the interpolated value as a double
   */
  public static double lerp(double from, double to, double t) {
    return from + t * (to - from);
  }

  /**
   * Linearly interpolates between two values within a specified period, handling wrap-around
   * behavior.
   *
   * @param period the period over which the interpolation occurs
   * @param from the starting value
   * @param to the ending value
   * @param t the interpolation factor (between 0 and 1)
   * @return the interpolated value within the range [0, period)
   */
  public static double lerpLoop(double period, double from, double to, double t) {
    from = remainder(from, period);
    to = remainder(to, period);
    if (Math.abs(to - from) > period / 2) {
      if (to < from) {
        to += period;
      } else {
        from += period;
      }
    }
    return remainder(lerp(from, to, t), period);
  }

  /**
   * Linearly interpolates between two angles in degrees, handling wrap-around behavior at 360
   * degrees.
   *
   * @param from the starting angle in degrees
   * @param to the ending angle in degrees
   * @param t the interpolation factor (between 0 and 1)
   * @return the interpolated angle in degrees within the range [0, 360)
   */
  public static double lerpDegrees(double from, double to, double t) {
    return lerpLoop(360, from, to, t);
  }

  /**
   * Linearly interpolates between two angles in radians, handling wrap-around behavior at 2π
   * radians.
   *
   * @param from the starting angle in radians
   * @param to the ending angle in radians
   * @param t the interpolation factor (between 0 and 1)
   * @return the interpolated angle in radians within the range [0, 2π)
   */
  public static double lerpRadians(double from, double to, double t) {
    return lerpLoop(2 * Math.PI, from, to, t);
  }
}
