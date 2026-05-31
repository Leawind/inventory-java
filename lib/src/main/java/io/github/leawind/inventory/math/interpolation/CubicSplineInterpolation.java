package io.github.leawind.inventory.math.interpolation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CubicSplineInterpolation implements Interpolation<Double> {
  // Sorted points by x, low to high
  private final List<Point<Double>> points = new ArrayList<>();
  private double[] a;
  private double[] b;
  private double[] c;
  private double[] d;

  public CubicSplineInterpolation() {}

  /**
   * @param y0 Value of y at x == 0
   * @param y1 Value of y at x == 1
   */
  public CubicSplineInterpolation(double y0, double y1) {
    this(List.of(new Point<>(0, y0), new Point<>(1, y1)));
  }

  public CubicSplineInterpolation(List<Point<Double>> points) {
    this.points.addAll(points);
    this.points.sort(Comparator.comparingDouble(Point::x));
    computeCoefficients();
  }

  @Override
  public CubicSplineInterpolation add(double x, Double y) {
    var newPoint = new Point<>(x, y);

    var it = points.listIterator();
    while (it.hasNext()) {
      var point = it.next();

      if (point.x() == x) {
        it.set(newPoint);
        computeCoefficients();
        return this;
      }

      if (point.x() > x) {
        it.previous();
        break;
      }
    }
    it.add(newPoint);
    computeCoefficients();
    return this;
  }

  private void computeCoefficients() {
    int n = points.size();
    if (n < 2) {
      return;
    }

    double[] h = new double[n - 1];
    double[] alpha = new double[n - 1];
    double[] l = new double[n];
    double[] mu = new double[n];
    double[] z = new double[n];

    a = new double[n];
    b = new double[n];
    c = new double[n];
    d = new double[n];

    for (int i = 0; i < n - 1; i++) {
      h[i] = points.get(i + 1).x() - points.get(i).x();
      if (h[i] == 0) {
        throw new IllegalArgumentException("Duplicate x values in points.");
      }
    }

    for (int i = 1; i < n - 1; i++) {
      alpha[i] =
          (3 / h[i]) * (points.get(i + 1).y() - points.get(i).y())
              - (3 / h[i - 1]) * (points.get(i).y() - points.get(i - 1).y());
    }

    l[0] = 1;
    mu[0] = 0;
    z[0] = 0;

    for (int i = 1; i < n - 1; i++) {
      l[i] = 2 * (points.get(i + 1).x() - points.get(i - 1).x()) - h[i - 1] * mu[i - 1];
      mu[i] = h[i] / l[i];
      z[i] = (alpha[i] - h[i - 1] * z[i - 1]) / l[i];
    }

    l[n - 1] = 1;
    z[n - 1] = 0;
    c[n - 1] = 0;

    for (int j = n - 2; j >= 0; j--) {
      c[j] = z[j] - mu[j] * c[j + 1];
      b[j] = (points.get(j + 1).y() - points.get(j).y()) / h[j] - h[j] * (c[j + 1] + 2 * c[j]) / 3;
      d[j] = (c[j + 1] - c[j]) / (3 * h[j]);
    }

    for (int i = 0; i < n; i++) {
      a[i] = points.get(i).y();
    }
  }

  public Double interpolate(double x) {
    if (points.size() < 2) {
      throw new IllegalArgumentException(
          "At least two points are required for spline interpolation.");
    }

    int n = points.size();
    int i = 0;
    if (x == points.get(n - 1).x()) {
      i = n - 2;
    } else {
      for (i = 0; i < n - 1; i++) {
        if (x < points.get(i + 1).x()) {
          break;
        }
      }
    }

    double dx = x - points.get(i).x();
    return a[i] + b[i] * dx + c[i] * Math.pow(dx, 2) + d[i] * Math.pow(dx, 3);
  }
}
