package io.github.leawind.inventory.math.monolist;

import java.util.Arrays;
import java.util.function.Function;

public class StaticMonoList extends MonoList {
  private final int sign;
  private final double[] array;

  protected StaticMonoList(double[] array) {
    this.array = Arrays.copyOf(array, array.length);
    sign = findSign(array);

    if (!isMono(sign, array)) {
      throw new IllegalArgumentException("Invalid array");
    }
  }

  @Override
  public double get(int i) {
    return array[i];
  }

  @Override
  public int sign() {
    return sign;
  }

  @Override
  public int length() {
    return array.length;
  }

  public static StaticMonoList linear(int length) {
    return of(length, i -> (double) i);
  }

  public static StaticMonoList exp(int length) {
    return of(length, Math::exp);
  }

  public static StaticMonoList squared(int length) {
    return of(length, i -> (double) (i * i));
  }

  public static StaticMonoList of(int length, Function<Integer, Double> getter) {
    double[] list = new double[length];
    for (int i = 0; i < length; i++) {
      list[i] = getter.apply(i);
    }
    return of(list);
  }

  public static StaticMonoList of(double[] list) {
    return new StaticMonoList(list);
  }

  public static StaticMonoList of(
      int length,
      double min,
      double max,
      Function<Double, Double> f,
      Function<Double, Double> fInv) {
    final double xmin = fInv.apply(min);
    final double xstep = (fInv.apply(max) - xmin) / length;
    return of(length, i -> f.apply(i * xstep + xmin));
  }
}
