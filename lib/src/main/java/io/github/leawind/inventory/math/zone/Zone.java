package io.github.leawind.inventory.math.zone;

import java.util.Arrays;
import javax.annotation.Nullable;

/**
 * Zone represents a range of values defined by a minimum and maximum value.
 *
 * <p>{@code [min, max]}
 *
 * <pre>
 *   min    center    max
 *    |________|_______|
 * </pre>
 */
public class Zone {
  /** The minimum value of the zone. */
  public final double min;

  /** The maximum value of the zone. */
  public final double max;

  /**
   * Constructs a new Zone with the specified minimum and maximum values.
   *
   * @param min the minimum value of the zone
   * @param max the maximum value of the zone
   * @throws IllegalArgumentException if the minimum value is greater than the maximum value
   */
  public Zone(double min, double max) throws IllegalArgumentException {
    if (min > max) {
      throw new IllegalArgumentException(
          "Minimum cannot be greater than maximum: " + min + " > " + max);
    }
    this.min = min;
    this.max = max;
  }

  /**
   * Returns the radius of the zone, which is half the length of the zone.
   *
   * @return the radius of the zone
   */
  public double radius() {
    return (max - min) / 2;
  }

  /**
   * Scales the zone around its center by the specified scale factor.
   *
   * @param scale the scale factor
   * @return a new Zone scaled around its center
   */
  public Zone scale(double scale) {
    return Zone.ofLength(center(), length() * scale);
  }

  /**
   * Returns the center of the zone.
   *
   * @return the center of the zone
   */
  public double center() {
    return (min + max) / 2;
  }

  /**
   * Returns the length of the zone, which is the difference between the maximum and minimum values.
   *
   * @return the length of the zone
   */
  public double length() {
    return max - min;
  }

  /**
   * Calculates the intersection of this zone with another zone.
   *
   * <pre>
   * 	this:   |--------|
   * 	that:         |---------|
   * 	intersection:        |--|
   * </pre>
   *
   * @param zone the other zone to intersect with
   * @return a new Zone representing the intersection, or null if there is no intersection
   */
  public @Nullable Zone intersection(Zone zone) {
    double min = Math.max(this.min, zone.min);
    double max = Math.min(this.max, zone.max);
    if (min > max) {
      return null;
    }
    return new Zone(min, max);
  }

  /**
   * Calculates the union of this zone with another zone.
   *
   * <pre>
   * 	this:   |--------|
   * 	that:         |---------|
   * 	union:  |---------------|
   * </pre>
   *
   * @param zone the other zone to form the union with
   * @return a new Zone representing the union, or null if the zones do not intersect
   */
  public @Nullable Zone union(Zone zone) {
    if (!hasIntersection(zone)) {
      return null;
    }
    return new Zone(Math.min(min, zone.min), Math.max(max, zone.max));
  }

  public Zone expand(double delta) throws IllegalArgumentException {
    final var limit = -radius();
    if (delta < limit) {
      throw new IllegalArgumentException("Expanded radius must be no less than " + limit);
    }
    if (delta == limit) {
      return Zone.ofRadius(center(), 0);
    }
    return new Zone(min - delta, max + delta);
  }

  public Zone expand(double leftDelta, double rightDelta) throws IllegalArgumentException {
    final var limit = -radius();
    if (leftDelta < limit) {
      throw new IllegalArgumentException("leftDelta < - radius: " + leftDelta + "<" + limit);
    }
    if (rightDelta < limit) {
      throw new IllegalArgumentException("rightDelta < - radius: " + rightDelta + "<" + limit);
    }
    return new Zone(min - leftDelta, max + rightDelta);
  }

  public Zone squeeze(double d) throws IllegalArgumentException {
    return expand(-d);
  }

  /**
   * Safely squeezes the zone by reducing the specified amount from both sides, but does not allow
   * the zone to collapse.
   *
   * @param d the amount to squeeze from each side
   * @return a new Zone safely squeezed by the specified amount
   */
  public Zone squeezeSafely(double d) {
    double r = Math.min(d, radius());
    return Zone.of(min + r, max - r);
  }

  /**
   * Checks if this zone intersects with another zone.
   *
   * @param zone the other zone to check for intersection
   * @return true if the zones intersect, false otherwise
   */
  public boolean hasIntersection(Zone zone) {
    return min <= zone.max && zone.min <= max;
  }

  /**
   * Moves the zone by the specified offset.
   *
   * @param offset the amount to move the zone
   * @return a new Zone moved by the specified offset
   */
  public Zone move(double offset) {
    return new Zone(min + offset, max + offset);
  }

  /**
   * Checks if this zone is before another zone.
   *
   * @param zone the other zone to compare
   * @return true if this zone ends before the other zone starts, false otherwise
   */
  public boolean isBefore(Zone zone) {
    return max <= zone.min;
  }

  /**
   * Checks if this zone is after another zone.
   *
   * @param zone the other zone to compare
   * @return true if this zone starts after the other zone ends, false otherwise
   */
  public boolean isAfter(Zone zone) {
    return min >= zone.max;
  }

  /**
   * Creates a new zone with the specified length to the left of this zone.
   *
   * <pre>
   * this:          |----|
   * neighbor:  |---|
   * </pre>
   *
   * @param length the length of the neighbor zone
   * @return a new Zone to the left of this zone
   * @throws IllegalArgumentException if the length is negative
   */
  public Zone lessNeighbor(double length) throws IllegalArgumentException {
    if (length < 0) {
      throw new IllegalArgumentException("Length must be non-negative, not " + length);
    }
    return new Zone(min - length, min);
  }

  /**
   * Creates a new zone with the specified length to the right of this zone.
   *
   * <pre>
   * this:     |----|
   * neighbor:      |---|
   * </pre>
   *
   * @param length the length of the neighbor zone
   * @return a new Zone to the right of this zone
   * @throws IllegalArgumentException if the length is negative
   */
  public Zone greaterNeighbor(double length) throws IllegalArgumentException {
    if (length < 0) {
      throw new IllegalArgumentException("Length must be non-negative, not " + length);
    }
    return new Zone(max, max + length);
  }

  /**
   * Finds the nearest value within the zone to the given value.
   *
   * @param value the value to find the nearest point to
   * @return the nearest value within the zone
   */
  public double nearest(double value) {
    return Math.min(Math.max(value, min), max);
  }

  /**
   * Finds the furthest value within the zone from the given value.
   *
   * @param value the value to find the furthest point from
   * @return the furthest value within the zone
   */
  public double furthest(double value) {
    return value <= center() ? max : min;
  }

  /**
   * Calculates the distance from the given value to the nearest edge of the zone.
   *
   * @param value the value to calculate the distance from
   * @return the distance from the value to the nearest edge of the zone
   */
  public double distance(double value) {
    return (value < min) ? (min - value) : (value > max) ? (value - max) : 0;
  }

  public double distance(Zone zone) {
    if (max <= zone.min) {
      return zone.min - max;
    }
    if (min >= zone.max) {
      return min - zone.max;
    }
    return 0;
  }

  /**
   * Checks if this zone is completely inside another zone.
   *
   * @param zone the other zone to check against
   * @return true if this zone is completely inside the other zone, false otherwise
   */
  public boolean isInside(Zone zone) {
    return zone.min <= min && max <= zone.max;
  }

  /**
   * Checks if this zone completely contains another zone.
   *
   * @param zone the other zone to check against
   * @return true if this zone completely contains the other zone, false otherwise
   */
  public boolean contains(Zone zone) {
    return zone.min >= min && zone.max <= max;
  }

  /**
   * Checks if this zone contains the specified value.
   *
   * @param value the value to check for containment
   * @return true if the value is within the zone, false otherwise
   */
  public boolean contains(double value) {
    return min <= value && value <= max;
  }

  /**
   * Creates a new zone with the same minimum value and the specified maximum value.
   *
   * @param max the new maximum value for the zone
   * @return a new Zone with the updated maximum value
   * @throws IllegalArgumentException if the new maximum value is less than the current minimum
   *     value
   */
  public Zone newWithMax(double max) throws IllegalArgumentException {
    return new Zone(min, max);
  }

  /**
   * Creates a new zone with the specified minimum value and the same maximum value.
   *
   * @param min the new minimum value for the zone
   * @return a new Zone with the updated minimum value
   * @throws IllegalArgumentException if the new minimum value is greater than the current maximum
   *     value
   */
  public Zone newWithMin(double min) throws IllegalArgumentException {
    return new Zone(min, max);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(new double[] {min, max});
  }

  /**
   * Compares this zone to another object for equality.
   *
   * @param obj the object to compare to
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (obj instanceof Zone zone) {
      return min == zone.min && max == zone.max;
    }
    return false;
  }

  /**
   * Returns a string representation of the zone in the format "Zone[min, max]".
   *
   * @return a string representation of the zone
   */
  @Override
  public String toString() {
    return "Zone[" + min + ", " + max + "]";
  }

  /**
   * Creates a new zone with the specified values, ensuring that the minimum is less than or equal
   * to the maximum.
   *
   * @param a one endpoint of the zone
   * @param b the other endpoint of the zone
   * @return a new Zone with the endpoints sorted
   */
  public static Zone of(double a, double b) {
    return a < b ? new Zone(a, b) : new Zone(b, a);
  }

  /**
   * Creates a new zone centered at the specified value with the specified radius.
   *
   * @param center the center value of the zone
   * @param radius the radius of the zone
   * @return a new Zone centered at the specified value with the specified radius
   * @throws IllegalArgumentException if the radius is negative
   */
  public static Zone ofRadius(double center, double radius) throws IllegalArgumentException {
    if (radius < 0) {
      throw new IllegalArgumentException("Radius must be non-negative, got " + radius);
    }
    return new Zone(center - radius, center + radius);
  }

  /**
   * Creates a new zone centered at the specified value with the specified length.
   *
   * @param center the center value of the zone
   * @param length the length of the zone
   * @return a new Zone centered at the specified value with the specified length
   * @throws IllegalArgumentException if the length is negative
   */
  public static Zone ofLength(double center, double length) throws IllegalArgumentException {
    if (length < 0) {
      throw new IllegalArgumentException("Length must be non-negative, got " + length);
    }
    return new Zone(center - length / 2, center + length / 2);
  }
}
