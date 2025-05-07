package io.github.leawind.inventory.just;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.leawind.inventory.just.enums.Option;
import io.github.leawind.inventory.tuple.Tuple.Tuple2;
import org.junit.jupiter.api.Test;

public class OptionTest {

  @Test
  public void testCreation() {
    // Test some()
    Option<String> someStr = Option.some("test");
    assertTrue(someStr.isSome());
    assertEquals("test", someStr.unwrap());

    // Test none()
    Option<String> noneStr = Option.none();
    assertTrue(noneStr.isNone());

    // Test from()
    Option<Integer> fromInt = Option.from(42);
    assertTrue(fromInt.isSome());
    assertEquals(42, (int) fromInt.unwrap());
  }

  @Test
  public void testQueryMethods() {
    Option<String> some = Option.some("hello");
    Option<String> none = Option.none();

    // Test isSome()
    assertTrue(some.isSome());
    assertFalse(none.isSome());

    // Test isNone()
    assertFalse(some.isNone());
    assertTrue(none.isNone());

    // Test isSomeAnd()
    assertTrue(some.isSomeAnd(s -> s.length() == 5));
    assertFalse(some.isSomeAnd(s -> s.length() > 10));

    // Test isNoneOr()
    assertTrue(none.isNoneOr(s -> s.length() > 10));
    assertTrue(some.isNoneOr(s -> s.length() == 5));
  }

  @Test()
  public void testExpect() {
    assertThrows(
        RuntimeException.class,
        () -> {
          Option<String> none = Option.none();
          none.expect("This should throw");
        });
  }

  @Test
  public void testUnwrapVariants() {
    Option<Integer> some = Option.some(10);
    Option<Integer> none = Option.none();

    // Test unwrap()
    assertEquals(10, (int) some.unwrap());

    // Test unwrapOr()
    assertEquals(10, (int) some.unwrapOr(20));
    assertEquals(20, (int) none.unwrapOr(20));

    // Test unwrapOrElse()
    assertEquals(10, (int) some.unwrapOrElse(() -> 30));
    assertEquals(30, (int) none.unwrapOrElse(() -> 30));
  }

  @Test
  public void testTransformationMethods() {
    Option<Integer> some = Option.some(5);
    Option<Integer> none = Option.none();

    // Test map()
    Option<String> mappedSome = some.map(i -> "Number: " + i);
    assertTrue(mappedSome.isSome());
    assertEquals("Number: 5", mappedSome.unwrap());

    Option<String> mappedNone = none.map(i -> "Number: " + i);
    assertTrue(mappedNone.isNone());

    // Test inspect()
    StringBuilder sb = new StringBuilder();
    some.inspect(i -> sb.append("Inspected: ").append(i));
    assertEquals("Inspected: 5", sb.toString());

    // Test mapOr()
    assertEquals("Number: 5", some.mapOr("Default", i -> "Number: " + i));
    assertEquals("Default", none.mapOr("Default", i -> "Number: " + i));

    // Test okOr()
    assertTrue(some.okOr("Error").isOk());
    assertTrue(none.okOr("Error").isErr());
  }

  @Test
  public void testBooleanOperations() {
    Option<Integer> some5 = Option.some(5);
    Option<Integer> none = Option.none();
    Option<Integer> some10 = Option.some(10);

    // Test and()
    assertTrue(some5.and(some10).isSome());
    assertEquals(10, some5.and(some10).unwrap());
    assertTrue(none.and(some10).isNone());

    // Test andThen()
    Option<String> andThenResult = some5.andThen(i -> Option.some("Value: " + i));
    assertTrue(andThenResult.isSome());
    assertEquals("Value: 5", andThenResult.unwrap());

    // Test filter()
    assertTrue(some5.filter(i -> i == 5).isSome());
    assertTrue(some5.filter(i -> i != 5).isNone());

    // Test or()
    assertTrue(some5.or(some10).isSome());
    assertEquals(5, (int) some5.or(some10).unwrap());
    assertEquals(10, (int) none.or(some10).unwrap());

    // Test xor()
    assertTrue(some5.xor(none).isSome());
    assertTrue(none.xor(some5).isSome());

    assertTrue(some5.xor(some10).isNone());
    assertTrue(none.xor(none).isNone());
  }

  @Test
  public void testInsertOperations() {
    Option<String> opt = Option.none();

    // Test insert()
    String inserted = opt.insert("new");
    assertEquals("new", inserted);
    assertTrue(opt.isSome());
    assertEquals("new", opt.unwrap());

    // Test getOrInsert()
    String existing = opt.getOrInsert("another");
    assertEquals("new", existing);
    assertEquals("new", opt.unwrap());

    // Test getOrInsertWith()
    Option<String> empty = Option.none();
    String withDefault = empty.getOrInsertWith(() -> "default");
    assertEquals("default", withDefault);
    assertTrue(empty.isSome());
  }

  @Test
  public void testMiscOperations() {
    Option<String> original = Option.some("original");

    // Test take()
    Option<String> taken = original.take();
    assertTrue(taken.isSome());
    assertEquals("original", taken.unwrap());
    assertTrue(original.isNone());

    // Test replace()
    Option<String> old = original.replace("new");
    assertTrue(old.isNone());
    assertTrue(original.isSome());
    assertEquals("new", original.unwrap());

    // Test takeIf()
    Option<String> toTake = Option.some("test");
    Option<String> takenIf = toTake.takeIf(s -> s.startsWith("t"));
    assertTrue(takenIf.isSome());
    assertEquals("test", takenIf.unwrap());
    assertTrue(toTake.isNone());
  }

  @Test
  public void testZipOperations() {
    Option<Integer> someInt = Option.some(5);
    Option<String> someStr = Option.some("hello");
    Option<Integer> noneInt = Option.none();

    // Test zip()
    Option<Tuple2<Integer, String>> zipped = someInt.zip(someStr);
    assertTrue(zipped.isSome());
    Tuple2<Integer, String> tuple = zipped.unwrap();
    assertEquals(5, (int) tuple.v1());
    assertEquals("hello", tuple.v2());

    assertTrue(someInt.zip(noneInt).isNone());
    assertTrue(noneInt.zip(someStr).isNone());

    // Test zipWith()
    Option<String> zippedWith = someInt.zipWith(someStr, (i, s) -> i + ": " + s);
    assertTrue(zippedWith.isSome());
    assertEquals("5: hello", zippedWith.unwrap());
  }

  @Test
  public void testFlatten() {
    Option<Option<String>> nestedSome = Option.some(Option.some("nested"));
    Option<Option<String>> nestedNone = Option.some(Option.none());
    Option<Option<String>> none = Option.none();

    // Test flatten()
    Option<String> flattenedSome = nestedSome.flatten();
    assertTrue(flattenedSome.isSome());
    assertEquals("nested", flattenedSome.unwrap());

    Option<String> flattenedNone = nestedNone.flatten();
    assertTrue(flattenedNone.isNone());

    Option<String> flattenedOuterNone = none.flatten();
    assertTrue(flattenedOuterNone.isNone());

    // Test flatten(Class)
    Option<String> typedFlatten = nestedSome.flatten(String.class);
    assertTrue(typedFlatten.isSome());
    assertEquals("nested", typedFlatten.unwrap());
  }

  @Test()
  public void testFlattenWithWrongType() {
    assertThrows(
        ClassCastException.class,
        () -> {
          Option<Option<Integer>> nested = Option.some(Option.some(42));
          nested.flatten(String.class);
        });
  }

  @Test
  public void testEquals() {
    Option<String> some1 = Option.some("test");
    Option<String> some2 = Option.some("test");
    Option<String> someDiff = Option.some("different");
    Option<String> none1 = Option.none();
    Option<String> none2 = Option.none();

    assertEquals(some1, some2);
    assertNotEquals(some1, someDiff);
    assertNotEquals(some1, none1);
    assertEquals(none1, none2);
    assertNotEquals("not an option", some1.unwrap());
  }
}
