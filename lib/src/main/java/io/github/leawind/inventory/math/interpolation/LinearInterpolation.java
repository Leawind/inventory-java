package io.github.leawind.inventory.math.interpolation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;

public class LinearInterpolation implements Interpolation<Double> {
  // Sorted points by x, low to high
  private final List<Point<Double>> points = new ArrayList<>();

  public LinearInterpolation() {}

  /**
   * @param y0 Value of y at x == 0
   * @param y1 Value of y at x == 1
   */
  public LinearInterpolation(double y0, double y1) {
    this(Arrays.asList(new Point<>(0, y0), new Point<>(1, y1)));
  }

  public LinearInterpolation(Collection<Point<Double>> points) {
    this.points.addAll(points);
    this.points.sort(Comparator.comparingDouble(Point::x));
  }

  @Override
  public LinearInterpolation add(double x, Double y) {
    Point<Double> newPoint = new Point<>(x, y);

    ListIterator<Point<Double>> it = points.listIterator();
    while (it.hasNext()) {
      Point<Double> point = it.next();

      if (point.x() == x) {
        it.set(newPoint);
        return this;
      }

      if (point.x() > x) {
        it.previous();
        break;
      }
    }
    it.add(newPoint);
    return this;
  }

  public Double interpolate(double x) {
    if (points.isEmpty()) {
      throw new IllegalStateException("At least one point is required");
    }
    if (points.size() == 1) {
      return points.get(0).y();
    }
    if (x <= points.get(0).x()) {
      return points.get(0).y();
    }
    if (x >= points.get(points.size() - 1).x()) {
      return points.get(points.size() - 1).y();
    }
    for (int i = 0; i < points.size() - 1; i++) {
      Point<Double> point = points.get(i);
      if (point.x() <= x && x <= points.get(i + 1).x()) {
        Double a = point.y();
        Double b = points.get(i + 1).y();
        double t = (x - point.x()) / (points.get(i + 1).x() - point.x());
        return a + (b - a) * t;
      }
    }
    return 0d;
  }
}
