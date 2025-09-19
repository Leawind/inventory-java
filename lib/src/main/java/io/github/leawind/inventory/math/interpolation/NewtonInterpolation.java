package io.github.leawind.inventory.math.interpolation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class NewtonInterpolation implements Interpolation<Double> {
  // Sorted points by x, low to high
  private final List<Point<Double>> points = new ArrayList<>();
  // Divided differences
  private double[][] dd;

  public NewtonInterpolation() {}

  public NewtonInterpolation(double y0, double y1) {
    this(List.of(new Point<>(0, y0), new Point<>(1, y1)));
  }

  public NewtonInterpolation(Collection<Point<Double>> points) {
    this.points.addAll(points);
    this.points.sort(Comparator.comparingDouble(Point::x));
    computeDividedDifferences();
  }

  @Override
  public NewtonInterpolation add(double x, Double y) {
    var newPoint = new Point<>(x, y);

    var it = points.listIterator();
    while (it.hasNext()) {
      var point = it.next();

      if (point.x() == x) {
        it.set(newPoint);
        computeDividedDifferences();
        return this;
      }

      if (point.x() > x) {
        it.previous();
        break;
      }
    }
    it.add(newPoint);
    computeDividedDifferences();
    return this;
  }

  private void computeDividedDifferences() {
    int n = points.size();
    dd = new double[n][n];

    for (int i = 0; i < n; i++) {
      dd[i][0] = points.get(i).y();
    }

    for (int j = 1; j < n; j++) {
      for (int i = 0; i < n - j; i++) {
        dd[i][j] = (dd[i + 1][j - 1] - dd[i][j - 1]) / (points.get(i + j).x() - points.get(i).x());
      }
    }
  }

  public Double interpolate(double x) {
    if (points.isEmpty()) {
      throw new IllegalStateException("At least one point is required");
    }

    double result = dd[0][0];
    double term = 1.0;

    for (int i = 1; i < points.size(); i++) {
      term *= (x - points.get(i - 1).x());
      result += term * dd[0][i];
    }

    return result;
  }
}
