package io.github.leawind.inventory.math.interpolation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class LinearInterpolation implements Interpolation<Double> {
  // Sorted points by x, low to high
  private final List<Point<Double>> points = new ArrayList<>();

  public LinearInterpolation() {}

  /**
   * @param y0 Value of y at x == 0
   * @param y1 Value of y at x == 1
   */
  public LinearInterpolation(double y0, double y1) {
    this(List.of(new Point<>(0, y0), new Point<>(1, y1)));
  }

  public LinearInterpolation(Collection<Point<Double>> points) {
    this.points.addAll(points);
    this.points.sort(Comparator.comparingDouble(Point::x));
  }

  @Override
  public LinearInterpolation add(double x, Double y) {
    var newPoint = new Point<>(x, y);

    var it = points.listIterator();
    while (it.hasNext()) {
      var point = it.next();

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
      return points.getFirst().y();
    }
    if (x <= points.getFirst().x()) {
      return points.getFirst().y();
    }
    if (x >= points.getLast().x()) {
      return points.getLast().y();
    }
    for (int i = 0; i < points.size() - 1; i++) {
      var point = points.get(i);
      if (point.x() <= x && x <= points.get(i + 1).x()) {
        var a = point.y();
        var b = points.get(i + 1).y();
        var t = (x - point.x()) / (points.get(i + 1).x() - point.x());
        return a + (b - a) * t;
      }
    }
    return 0d;
  }
}
