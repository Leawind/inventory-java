package io.github.leawind.inventory.math.monolist;

import java.util.function.Function;

/**
 * Monotonic List
 *
 * <p>The data in the list is either monotonically increasing or decreasing.
 */
public abstract class MonoList {

  ////////////////////////////////////////////////////////////////
  /// Basic
  ////////////////////////////////////////////////////////////////

  /** Get the value at the specified index */
  public abstract double get(int i);

  /**
   * @return Returns 1 if the list is monotonically increasing, -1 if monotonically decreasing
   */
  public abstract int sign();

  /**
   * @return Length of the list
   */
  public abstract int length();

  ////////////////////////////////////////////////////////////////
  /// Advanced
  ////////////////////////////////////////////////////////////////

  /**
   * Get the index of the closest value
   *
   * <p>Example:
   *
   * <pre>{@code
   *              result=1
   *                ↓
   * Index: | 0   | 1   | 2   | 3   | 4   |
   * Values:| 1.0 | 2.0 | 3.0 | 4.0 | 5.0 |
   *                  ↑
   *              value=2.4≈2.0
   * }</pre>
   */
  public int nearestIndex(double value) {
    int ileft = 0;
    int iright = length() - 1;
    int icenter = length() / 2;

    while (true) {
      double vcenter = get(icenter);
      if (vcenter < value) {
        ileft = icenter;
      } else if (vcenter > value) {
        iright = icenter;
      } else {
        return icenter;
      }
      if (iright - ileft == 1) {
        break;
      }
      icenter = (ileft + iright) / 2;
    }

    double leftGap = value - get(ileft);
    double rightGap = get(iright) - value;
    if (leftGap < rightGap) {
      return ileft;
    } else if (leftGap > rightGap) {
      return iright;
    } else if (sign() >= 0) {
      return iright;
    } else {
      return ileft;
    }
  }

  /**
   * Find the nearest value
   *
   * <p>Example:
   *
   * <pre>{@code
   * Index: | 0   | 1   | 2   | 3   | 4   |
   * Values:| 1.0 | 2.0 | 3.0 | 4.0 | 5.0 |
   *               ↑  ↑
   *               | value=2.4
   *             result=2.0
   * }</pre>
   */
  public double nearestValue(double value) {
    return get(nearestIndex(value));
  }

  /**
   * Find the index corresponding to the current value, then offset it and get the value at that
   * index in the list
   *
   * <p>If the offset index is out of range, take the edge value (first or last value)
   *
   * <p>Example:
   *
   * <pre>{@code
   * value ≈ B
   * offset = 2
   *
   * Index: | 0 | 1 | 2 | 3 | 4 |
   * Values:| A | B | C | D | E |
   *              ↑       ↑
   *            value     |
   *                      |
   *             index(value)+offset
   * }</pre>
   *
   * @param value The value
   * @param offset The offset amount
   */
  public double offsetValue(double value, int offset) {
    int i = nearestIndex(value) + offset * sign();
    return get(clampIndex(i));
  }

  /**
   * Get the next value of the specified value
   *
   * <p>If the value is already at the last interval, return the last value directly
   */
  public double nextValue(double value) {
    return offsetValue(value, 1);
  }

  /**
   * Get the previous value of the specified value
   *
   * <p>If the value is already at the first interval, return the first value directly
   */
  public double previousValue(double value) {
    return offsetValue(value, -1);
  }

  ////////////////////////////////////////////////////////////////
  /// Others
  ////////////////////////////////////////////////////////////////

  /** Convert to an array */
  public double[] toArray() {
    double[] list = new double[length()];
    for (int i = 0; i < length(); i++) {
      list[i] = get(i);
    }
    return list;
  }

  ////////////////////////////////////////////////////////////////
  /// Unpublic
  ////////////////////////////////////////////////////////////////

  /**
   * Clamp index to range [0, length() - 1]
   *
   * @param i index
   * @return clamped index
   */
  protected int clampIndex(int i) {
    return Math.max(0, Math.min(i, length() - 1));
  }

  /**
   * Check if array is monotonically increasing or decreasing
   *
   * @param sign sign of the array, 1 for increasing, -1 for decreasing
   * @param array array to check
   * @return if array is monotonically increasing or decreasing
   */
  protected static boolean isMono(int sign, double[] array) {
    for (int i = 1; i < array.length; i++) {
      int s = (int) Math.signum(array[i] - array[i - 1]);
      if (s != 0 && s != sign) {
        return false;
      }
    }
    return true;
  }

  /**
   * @param array array to check
   * @return 1 if mono increasing, -1 if mono decreasing, 0 otherwise
   */
  protected static int findSign(double[] array) {
    for (int i = 1; i < array.length; i++) {
      int s = (int) Math.signum(array[i] - array[i - 1]);
      if (s != 0) {
        return s;
      }
    }
    return 0;
  }

  /**
   * @param length length of the list
   * @param getter getter function
   * @return 1 if mono increasing, -1 if mono decreasing, 0 otherwise
   */
  protected static int findSign(int length, Function<Integer, Double> getter) {
    for (int i = 1; i < length; i++) {
      int s = (int) Math.signum(getter.apply(i) - getter.apply(i - 1));
      if (s != 0) {
        return s;
      }
    }
    return 0;
  }
}
