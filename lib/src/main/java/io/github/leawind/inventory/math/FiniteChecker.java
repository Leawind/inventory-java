package io.github.leawind.inventory.math;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class FiniteChecker {
  private final Map<Class<?>, Predicate<?>> customTypes = new HashMap<>();

  private final Consumer<NonFiniteException> reporter;

  private boolean alreadyReported;

  public FiniteChecker(Consumer<NonFiniteException> reporter) {
    this.reporter = reporter;
  }

  public <T> FiniteChecker adapt(Class<T> clazz, Predicate<T> isFinite) {
    customTypes.put(clazz, isFinite);
    return this;
  }

  public void reset() {
    alreadyReported = false;
  }

  public void checkOnce(double obj) {
    if (!alreadyReported) {
      check(obj);
    }
  }

  public void checkOnce(float f) {
    if (!alreadyReported) {
      check(f);
    }
  }

  public void checkOnce(double a, double b) {
    if (!alreadyReported) {
      check(a, b);
    }
  }

  public void checkOnce(float a, float b) {
    if (!alreadyReported) {
      check(a, b);
    }
  }

  public void checkOnce(double a, double b, double c) {
    if (!alreadyReported) {
      check(a, b, c);
    }
  }

  public void checkOnce(float a, float b, float c) {
    if (!alreadyReported) {
      check(a, b, c);
    }
  }

  public void checkOnce(double a, double b, double c, double d) {
    if (!alreadyReported) {
      check(a, b, c, d);
    }
  }

  public void checkOnce(float a, float b, float c, float d) {
    if (!alreadyReported) {
      check(a, b, c, d);
    }
  }

  public void checkOnce(Object... objects) {
    if (!alreadyReported) {
      check(objects);
    }
  }

  public void check(double d) {
    if (!Double.isFinite(d)) {
      reporter.accept(new NonFiniteException(d));
      alreadyReported = true;
    }
  }

  public void check(float f) {
    if (!Float.isFinite(f)) { // Fixed the bug: was if (!notFinite(f))
      reporter.accept(new NonFiniteException(f));
      alreadyReported = true;
    }
  }

  public void check(double a, double b) {
    if (!Double.isFinite(a) || !Double.isFinite(b)) {
      reporter.accept(new NonFiniteException(a, b));
      alreadyReported = true;
    }
  }

  public void check(float a, float b) {
    if (!Float.isFinite(a) || !Float.isFinite(b)) {
      reporter.accept(new NonFiniteException(a, b));
      alreadyReported = true;
    }
  }

  public void check(double a, double b, double c) {
    if (!Double.isFinite(a) || !Double.isFinite(b) || !Double.isFinite(c)) {
      reporter.accept(new NonFiniteException(a, b, c));
      alreadyReported = true;
    }
  }

  public void check(float a, float b, float c) {
    if (!Float.isFinite(a) || !Float.isFinite(b) || !Float.isFinite(c)) {
      reporter.accept(new NonFiniteException(a, b, c));
      alreadyReported = true;
    }
  }

  public void check(double a, double b, double c, double d) {
    if (!Double.isFinite(a) || !Double.isFinite(b) || !Double.isFinite(c) || !Double.isFinite(d)) {
      reporter.accept(new NonFiniteException(a, b, c, d));
      alreadyReported = true;
    }
  }

  public void check(float a, float b, float c, float d) {
    if (!Float.isFinite(a) || !Float.isFinite(b) || !Float.isFinite(c) || !Float.isFinite(d)) {
      reporter.accept(new NonFiniteException(a, b, c, d));
      alreadyReported = true;
    }
  }

  public void check(Object... objects) {
    for (Object obj : objects) {
      if (isFinite(obj)) {
        continue;
      }
      reporter.accept(new NonFiniteException(objects));
      alreadyReported = true;
      break;
    }
  }

  @SuppressWarnings("unchecked")
  public <T> boolean isFinite(Object obj) {
    if (obj instanceof Integer i) {
      return true;
    }
    if (obj instanceof Float f) {
      return Float.isFinite(f);
    }
    if (obj instanceof Double d) {
      return Double.isFinite(d);
    }

    Class<T> clazz = (Class<T>) obj.getClass();
    Predicate<T> predicate = (Predicate<T>) customTypes.get(clazz);
    return predicate != null && predicate.test(clazz.cast(obj));
  }

  public static final class NonFiniteException extends RuntimeException {
    public final Object[] objects;

    public NonFiniteException(Object... objects) {
      super("At least one of them is not finite: " + Arrays.toString(objects));
      this.objects = objects;
    }
  }
}
