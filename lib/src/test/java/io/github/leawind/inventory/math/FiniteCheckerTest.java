package io.github.leawind.inventory.math;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class FiniteCheckerTest {

  static class Vec2 {
    double x, y;

    public Vec2(double x, double y) {
      this.x = x;
      this.y = y;
    }

    @Override
    public String toString() {
      return "Vec2[" + x + ", " + y + "]";
    }

    static boolean isFinite(Vec2 v) {
      return v != null && Double.isFinite(v.x) && Double.isFinite(v.y);
    }
  }

  private List<FiniteChecker.NonFiniteException> exceptionList;
  private FiniteChecker checker;

  @BeforeEach
  void setUp() {
    exceptionList = new ArrayList<>();
    checker = new FiniteChecker(exceptionList::add).adapt(Vec2.class, Vec2::isFinite);
  }

  @Test
  void testCheckFiniteDouble() {
    checker.check(1.0);
    checker.check(0.0);
    checker.check(-1.5);
    checker.check(Double.MIN_VALUE);
    checker.check(Double.MAX_VALUE);
    assertTrue(exceptionList.isEmpty());
  }

  @Test
  void testCheckNonFiniteDouble() {
    checker.check(Double.POSITIVE_INFINITY);
    assertEquals(1, exceptionList.size());
    assertEquals(Double.POSITIVE_INFINITY, exceptionList.get(0).objects[0]);
  }

  @Test
  void testCheckNaNDouble() {
    checker.check(Double.NaN);
    assertEquals(1, exceptionList.size());
    assertEquals(Double.NaN, exceptionList.get(0).objects[0]);
  }

  @Test
  void testCheckFiniteFloat() {
    checker.check(1.0f);
    checker.check(0.0f);
    checker.check(-1.5f);
    checker.check(Float.MIN_VALUE);
    checker.check(Float.MAX_VALUE);
    assertTrue(exceptionList.isEmpty());
  }

  @Test
  void testCheckNonFiniteFloat() {
    checker.check(Float.POSITIVE_INFINITY);
    assertEquals(1, exceptionList.size());
    assertEquals(Float.POSITIVE_INFINITY, exceptionList.get(0).objects[0]);
  }

  @Test
  void testCheckNaNFloat() {
    checker.check(Float.NaN);
    assertEquals(1, exceptionList.size());
    assertEquals(Float.NaN, exceptionList.get(0).objects[0]);
  }

  @Test
  void testCheckMultipleDoublesAllFinite() {
    checker.check(1.0, 2.0);
    checker.check(1.0, 2.0, 3.0);
    checker.check(1.0, 2.0, 3.0, 4.0);
    assertTrue(exceptionList.isEmpty());
  }

  @Test
  void testCheckMultipleDoublesWithNonFinite() {
    checker.check(1.0, Double.POSITIVE_INFINITY);
    assertEquals(1, exceptionList.size());
    assertEquals(2, exceptionList.get(0).objects.length);
    assertEquals(1.0, exceptionList.get(0).objects[0]);
    assertEquals(Double.POSITIVE_INFINITY, exceptionList.get(0).objects[1]);
  }

  @Test
  void testCheckMultipleFloatsAllFinite() {
    checker.check(1.0f, 2.0f);
    checker.check(1.0f, 2.0f, 3.0f);
    checker.check(1.0f, 2.0f, 3.0f, 4.0f);
    assertTrue(exceptionList.isEmpty());
  }

  @Test
  void testCheckMultipleFloatsWithNonFinite() {
    checker.check(1.0f, Float.NEGATIVE_INFINITY);
    assertEquals(1, exceptionList.size());
    assertEquals(2, exceptionList.get(0).objects.length);
    assertEquals(1.0f, exceptionList.get(0).objects[0]);
    assertEquals(Float.NEGATIVE_INFINITY, exceptionList.get(0).objects[1]);
  }

  @Test
  void testCheckObjectsAllFinite() {
    checker.check((Object) 1.0, (Object) 2.0f, (Object) 3);
    assertTrue(exceptionList.isEmpty());
  }

  @Test
  void testCheckObjectsWithNonFinite() {
    checker.check((Object) 1.0, (Object) Float.POSITIVE_INFINITY, (Object) 3);
    assertEquals(1, exceptionList.size());
    assertEquals(3, exceptionList.get(0).objects.length);
    assertEquals(Float.POSITIVE_INFINITY, exceptionList.get(0).objects[1]);
  }

  @Test
  void testCheckOnceOnlyReportsOnce() {
    checker.checkOnce(Double.POSITIVE_INFINITY);
    checker.checkOnce(Double.NEGATIVE_INFINITY);
    checker.checkOnce(Float.NaN);
    assertEquals(1, exceptionList.size());
    assertEquals(Double.POSITIVE_INFINITY, exceptionList.get(0).objects[0]);
  }

  @Test
  void testCheckOnceMultipleParams() {
    checker.checkOnce(1.0, Double.POSITIVE_INFINITY);
    checker.checkOnce(1.0, Double.NEGATIVE_INFINITY);
    assertEquals(1, exceptionList.size());
    assertEquals(1.0, exceptionList.get(0).objects[0]);
    assertEquals(Double.POSITIVE_INFINITY, exceptionList.get(0).objects[1]);
  }

  @Test
  void testCheckOnceObjects() {
    checker.checkOnce((Object) 1.0, (Object) Float.NaN, (Object) 3);
    checker.checkOnce((Object) 2.0, (Object) Double.POSITIVE_INFINITY);
    assertEquals(1, exceptionList.size());
    assertEquals(Float.NaN, exceptionList.get(0).objects[1]);
  }

  @Test
  void testResetAllowsReportingAgain() {
    checker.checkOnce(Double.POSITIVE_INFINITY);
    assertEquals(1, exceptionList.size());

    checker.reset();

    checker.checkOnce(Double.NEGATIVE_INFINITY);
    assertEquals(2, exceptionList.size());
    assertEquals(Double.NEGATIVE_INFINITY, exceptionList.get(1).objects[0]);
  }

  @Test
  void testIsFiniteWithDouble() {
    assertTrue(checker.isFinite(1.0));
    assertFalse(checker.isFinite(Double.POSITIVE_INFINITY));
    assertFalse(checker.isFinite(Double.NEGATIVE_INFINITY));
    assertFalse(checker.isFinite(Double.NaN));
  }

  @Test
  void testIsFiniteWithFloat() {
    assertTrue(checker.isFinite(1.0f));
    assertFalse(checker.isFinite(Float.POSITIVE_INFINITY));
    assertFalse(checker.isFinite(Float.NEGATIVE_INFINITY));
    assertFalse(checker.isFinite(Float.NaN));
  }

  @Test
  void testIsFiniteWithInteger() {
    assertTrue(checker.isFinite(1));
    assertTrue(checker.isFinite(0));
    assertTrue(checker.isFinite(-1));
    assertTrue(checker.isFinite(Integer.MAX_VALUE));
    assertTrue(checker.isFinite(Integer.MIN_VALUE));
  }

  @Test
  void testIsFiniteWithNonNumericObject() {
    assertFalse(checker.isFinite("not a number"));
    assertFalse(checker.isFinite(new Object()));
  }

  @Test
  void testIsFiniteWithNull() {
    assertThrows(NullPointerException.class, () -> checker.isFinite(null));
  }

  @Test
  void testCustomTypeAdapter() {
    Vec2 finiteVec2 = new Vec2(1.0, 2.0);
    Vec2 nonFiniteVec2 = new Vec2(Double.POSITIVE_INFINITY, 2.0);

    assertTrue(checker.isFinite(finiteVec2));
    assertFalse(checker.isFinite(nonFiniteVec2));

    checker.check(finiteVec2);
    assertTrue(exceptionList.isEmpty());

    checker.check(nonFiniteVec2);
    assertEquals(1, exceptionList.size());
    assertEquals(nonFiniteVec2, exceptionList.get(0).objects[0]);
  }

  @Test
  void testCustomTypeWithCheckObjects() {
    Vec2 finiteVec2 = new Vec2(1.0, 2.0);
    Vec2 nonFiniteVec2 = new Vec2(Double.NaN, 2.0);

    checker.check((Object) finiteVec2, (Object) 1.0, (Object) nonFiniteVec2);
    assertEquals(1, exceptionList.size());
    assertEquals(nonFiniteVec2, exceptionList.get(0).objects[2]);
  }

  @Test
  void testMultipleCustomTypeAdapters() {
    class Vec3 {
      double x, y, z;

      Vec3(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
      }
    }

    FiniteChecker multiChecker =
        new FiniteChecker(exceptionList::add)
            .adapt(Vec2.class, v -> v != null && Double.isFinite(v.x) && Double.isFinite(v.y))
            .adapt(
                Vec3.class,
                v ->
                    v != null
                        && Double.isFinite(v.x)
                        && Double.isFinite(v.y)
                        && Double.isFinite(v.z));

    Vec2 finiteVec2 = new Vec2(1.0, 2.0);
    Vec3 finiteVec3 = new Vec3(1.0, 2.0, 3.0);
    Vec3 nonFiniteVec3 = new Vec3(Double.POSITIVE_INFINITY, 2.0, 3.0);

    assertTrue(multiChecker.isFinite(finiteVec2));
    assertTrue(multiChecker.isFinite(finiteVec3));
    assertFalse(multiChecker.isFinite(nonFiniteVec3));

    multiChecker.check(finiteVec2, finiteVec3, nonFiniteVec3);
    assertEquals(1, exceptionList.size());
    assertEquals(nonFiniteVec3, exceptionList.get(0).objects[2]);
  }

  @Test
  void testExceptionMessageFormat() {
    checker.check(Double.POSITIVE_INFINITY);
    assertEquals(
        "At least one of them is not finite: [Infinity]", exceptionList.get(0).getMessage());

    checker.reset();
    checker.check(1.0, Double.NaN);
    assertEquals(
        "At least one of them is not finite: [1.0, NaN]", exceptionList.get(1).getMessage());
  }
}
