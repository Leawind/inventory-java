package io.github.leawind.inventory.math.zone;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class ZoneTest {

  @Test
  void testConstructor() {
    Zone zone = new Zone(1.0, 5.0);

    assertThat(zone.min, is(1.0));
    assertThat(zone.max, is(5.0));
    assertThat(zone, is(Zone.of(1, 5)));

    assertThrows(IllegalArgumentException.class, () -> new Zone(5.0, 1.0));
  }

  @Test
  void testRadius() {
    assertThat(Zone.of(1.0, 5.0).radius(), is(2.0));
  }

  @Test
  void testToString() {
    assertThat(Zone.of(1.0, 5.0).toString(), is("Zone[1.0, 5.0]"));
  }

  @Test
  void testScale() {
    assertThat(Zone.of(1.0, 5.0).scale(2.0), is(Zone.of(-1, 7)));
  }

  @Test
  void testCenter() {
    assertThat(Zone.of(1.0, 5.0).center(), is(3.0));
  }

  @Test
  void testLength() {
    assertThat(Zone.of(1.0, 5.0).length(), is(4.0));
  }

  @Test
  void testIntersection() {
    Zone zone = Zone.of(1.0, 5.0);
    assertThat(zone.intersection(Zone.of(3.0, 7.0)), is(Zone.of(3.0, 5.0)));
    assertThat(zone.intersection(Zone.of(6.0, 8.0)), nullValue());
  }

  @Test
  void testUnion() {
    Zone zone = Zone.of(1.0, 5.0);
    assertThat(zone.union(Zone.of(3.0, 7.0)), is(Zone.of(1.0, 7.0)));
    assertThat(zone.union(Zone.of(6.0, 8.0)), is(nullValue()));
  }

  @Test
  void testExpand_D() {
    Zone zone = Zone.of(1.0, 5.0);

    assertThat(zone.expand(2), is(Zone.of(-1, 7.0)));
    assertThat(zone.expand(-2), is(Zone.of(3, 3)));

    assertThrows(IllegalArgumentException.class, () -> zone.expand(-3.0));
  }

  @Test
  void testExpand_DD() {
    Zone zone = Zone.of(1.0, 5.0);

    assertThat(zone.expand(1, 2), is(Zone.of(0, 7)));
    assertThrows(IllegalArgumentException.class, () -> zone.expand(-3.0, 2.0));
    assertThrows(IllegalArgumentException.class, () -> zone.expand(1.0, -3.0));
  }

  @Test
  void testSqueeze() {
    Zone zone = Zone.of(1.0, 5.0);

    assertThat(zone.squeeze(0), is(Zone.of(1.0, 5.0)));
    assertThat(zone.squeeze(1), is(Zone.of(2, 4)));
    assertThrows(IllegalArgumentException.class, () -> zone.squeeze(3.0));
  }

  @Test
  void testSqueezeSafely() {
    Zone zone = Zone.of(1.0, 5.0);
    assertThat(zone.squeezeSafely(3), is(Zone.of(3, 3)));
  }

  @Test
  void testHasIntersection() {
    Zone zone1 = Zone.of(1.0, 5.0);
    Zone zone2 = Zone.of(3.0, 7.0);

    assertTrue(zone1.hasIntersection(zone2));
    assertFalse(zone1.hasIntersection(Zone.of(6.0, 8.0)));
  }

  @Test
  void testMove() {
    assertThat(Zone.of(1.0, 5.0).move(2), is(Zone.of(3, 7)));
  }

  @Test
  void testIsBefore() {
    Zone zone = Zone.of(1.0, 5.0);
    assertTrue(zone.isBefore(Zone.of(6.0, 8.0)));
    assertFalse(zone.isBefore(Zone.of(3.0, 7.0)));
  }

  @Test
  void testIsAfter() {
    Zone zone = Zone.of(6.0, 8.0);
    assertTrue(zone.isAfter(Zone.of(1.0, 5.0)));
    assertFalse(zone.isAfter(Zone.of(3.0, 7.0)));
  }

  @Test
  void testLessNeighbor() {
    Zone zone = Zone.of(1.0, 5.0);

    assertThat(zone.lessNeighbor(2), is(Zone.of(-1, 1)));
    assertThrows(IllegalArgumentException.class, () -> zone.lessNeighbor(-1.0));
  }

  @Test
  void testGreaterNeighbor() {
    Zone zone = Zone.of(1.0, 5.0);

    assertThat(zone.greaterNeighbor(2), is(Zone.of(5, 7)));
    assertThrows(IllegalArgumentException.class, () -> zone.greaterNeighbor(-1.0));
  }

  @Test
  void testNearest() {
    Zone zone = Zone.of(1.0, 5.0);
    assertThat(zone.nearest(0.0), is(1.0));
    assertThat(zone.nearest(3.0), is(3.0));
    assertThat(zone.nearest(6.0), is(5.0));
  }

  @Test
  void testFurthest() {
    Zone zone = Zone.of(1.0, 5.0);
    assertThat(zone.furthest(3.0), is(5.0));
    assertThat(zone.furthest(0.0), is(5.0));
    assertThat(zone.furthest(6.0), is(1.0));
  }

  @Test
  void testDistance_D() {
    Zone zone = Zone.of(1.0, 5.0);
    assertThat(zone.distance(0.0), is(1.0));
    assertThat(zone.distance(3.0), is(0.0));
    assertThat(zone.distance(6.0), is(1.0));
  }

  @Test
  void testDistance_Z() {
    Zone zone = Zone.of(1, 5);
    assertThat(zone.distance(Zone.of(-1, 0)), is(1d));
    assertThat(zone.distance(Zone.of(0, 1)), is(0d));
    assertThat(zone.distance(Zone.of(0, 2)), is(0d));
    assertThat(zone.distance(Zone.of(2, 4)), is(0d));
    assertThat(zone.distance(Zone.of(4, 6)), is(0d));
    assertThat(zone.distance(Zone.of(5, 6)), is(0d));
    assertThat(zone.distance(Zone.of(6, 7)), is(1d));
    assertThat(zone.distance(Zone.of(0, 7)), is(0d));
  }

  @Test
  void testIsInside() {
    Zone zone1 = Zone.of(1.0, 5.0);
    Zone zone2 = Zone.of(2.0, 4.0);
    assertTrue(zone2.isInside(zone1));
    assertFalse(zone1.isInside(zone2));
  }

  @Test
  void testContainsZone() {
    Zone zone1 = Zone.of(1.0, 5.0);
    Zone zone2 = Zone.of(2.0, 4.0);
    assertTrue(zone1.contains(zone2));
    assertFalse(zone2.contains(zone1));
  }

  @Test
  void testContainsValue() {
    Zone zone = Zone.of(1.0, 5.0);
    assertTrue(zone.contains(3.0));
    assertFalse(zone.contains(0.0));
    assertFalse(zone.contains(6.0));
  }

  @Test
  void testNewWithMax() {
    Zone zone = Zone.of(1.0, 5.0);

    assertThat(zone.newWithMax(7), is(Zone.of(1, 7)));
    assertThrows(IllegalArgumentException.class, () -> zone.newWithMax(0.0));
  }

  @Test
  void testNewWithMin() {
    Zone zone = Zone.of(1.0, 5.0);

    assertThat(zone.newWithMin(-1), is(Zone.of(-1, 5)));
    assertThrows(IllegalArgumentException.class, () -> zone.newWithMin(6.0));
  }

  @Test
  void testOf() {
    Zone zone1 = Zone.of(1.0, 5.0);
    assertThat(zone1.min, is(1.0));
    assertThat(zone1.max, is(5.0));

    Zone zone2 = Zone.of(5.0, 1.0);
    assertThat(zone2.min, is(1.0));
    assertThat(zone2.max, is(5.0));
  }

  @Test
  void testOfRadius() {
    assertThat(Zone.ofRadius(3, 2), is(Zone.of(1, 5)));
    assertThrows(IllegalArgumentException.class, () -> Zone.ofRadius(3.0, -1.0));
  }

  @Test
  void testOfLength() {
    assertThat(Zone.ofLength(3, 4), is(Zone.of(1, 5)));
    assertThrows(IllegalArgumentException.class, () -> Zone.ofLength(3.0, -1.0));
  }
}
