package io.github.leawind.inventory.math.interpolation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public interface Interpolation<T> {
  Interpolation<T> add(double x, T y);

  default Interpolation<T> add(Collection<Point<T>> points) {
    for (var point : points) {
      add(point.x(), point.y());
    }
    return this;
  }

  T interpolate(double t);

  default T interpolate(float t) {
    return interpolate((double) t);
  }

  static <T> boolean hasDuplicatedPoint(List<Point<T>> points) {
    for (int i = 0; i < points.size(); i++) {
      for (int j = i + 1; j < points.size(); j++) {
        if (points.get(i).x() == points.get(j).x()) {
          return true;
        }
      }
    }
    return false;
  }

  static <T> boolean hasDuplicatedPoint(List<Point<T>> points, Point<T> newPoint) {
    for (var point : points) {
      if (point.x() == newPoint.x()) {
        return true;
      }
    }
    return false;
  }

  record Point<T>(double x, T y) {

    public static <T> Point<T>[] asArray(double[] x, T[] y) {
      @SuppressWarnings("unchecked")
      Point<T>[] points = new Point[x.length];
      for (int i = 0; i < x.length; i++) {
        points[i] = new Point<>(x[i], y[i]);
      }
      return points;
    }

    public static <T> List<Point<T>> asList(double[] x, T[] y) {
      var list = new ArrayList<Point<T>>();
      for (int i = 0; i < x.length; i++) {
        list.add(new Point<>(x[i], y[i]));
      }
      return list;
    }

    public static <T> void toArray(Point<T>[] points, double[] x, T[] y) {
      for (int i = 0; i < points.length; i++) {
        x[i] = points[i].x;
        y[i] = points[i].y;
      }
    }

    public static <T> void toArray(List<Point<T>> points, double[] x, T[] y) {
      for (int i = 0; i < points.size(); i++) {
        x[i] = points.get(i).x;
        y[i] = points.get(i).y;
      }
    }
  }
}
