package io.github.leawind.inventory.just;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import org.junit.jupiter.api.Test;

public class OptionTest {
  @Test
  void testSome() {
    Option<String> some = Option.some("test");
    assertTrue(some.isSome());
    assertFalse(some.isNone());
    assertEquals("test", some.unwrap());
    assertEquals("test", some.expect("Should not fail"));
    assertEquals("test", some.unwrapOr("default"));
    assertEquals("test", some.unwrapOrElse(() -> "default"));
  }

  @Test
  void testSomeWithNull() {
    Option<String> someNull = Option.some(null);
    assertTrue(someNull.isSome());
    assertFalse(someNull.isNone());
    assertNull(someNull.unwrap());
    assertNull(someNull.expect("Should not fail"));
    assertNull(someNull.unwrapOr("default"));
    assertNull(someNull.unwrapOrElse(() -> "default"));
  }

  @Test
  void testNone() {
    Option<String> none = Option.none();
    assertFalse(none.isSome());
    assertTrue(none.isNone());
    assertThrows(JustError.class, none::unwrap);
    assertThrows(
	    JustError.class, () -> none.expect("Expected failure"));
    assertEquals("default", none.unwrapOr("default"));
    assertEquals("default", none.unwrapOrElse(() -> "default"));
  }

  @Test
  void testMap() {
    Option<String> some = Option.some("test");
    Option<Integer> mapped = some.map(String::length);
    assertTrue(mapped.isSome());
    assertEquals(4, mapped.unwrap());

    Option<String> none = Option.none();
    Option<Integer> mappedNone = none.map(String::length);
    assertTrue(mappedNone.isNone());
  }

  @Test
  void testAnd() {
    Option<String> some = Option.some("test");
    Option<Integer> other = Option.some(42);
    Option<Integer> result = some.and(other);
    assertTrue(result.isSome());
    assertEquals(42, result.unwrap());

    Option<String> none = Option.none();
    Option<Integer> resultNone = none.and(other);
    assertTrue(resultNone.isNone());
  }

  @Test
  void testAndThen() {
    Option<String> some = Option.some("test");
    Option<Integer> result = some.andThen(s -> Option.some(s.length()));
    assertTrue(result.isSome());
    assertEquals(4, result.unwrap());

    Option<String> none = Option.none();
    Option<Integer> resultNone = none.andThen(s -> Option.some(s.length()));
    assertTrue(resultNone.isNone());
  }

  @Test
  void testOr() {
    Option<String> some = Option.some("test");
    Option<String> other = Option.some("other");
    Option<String> result = some.or(other);
    assertTrue(result.isSome());
    assertEquals("test", result.unwrap());

    Option<String> none = Option.none();
    Option<String> resultNone = none.or(other);
    assertTrue(resultNone.isSome());
    assertEquals("other", resultNone.unwrap());
  }

  @Test
  void testOrElse() {
    Option<String> some = Option.some("test");
    Option<String> result = some.orElse(() -> Option.some("other"));
    assertTrue(result.isSome());
    assertEquals("test", result.unwrap());

    Option<String> none = Option.none();
    Option<String> resultNone = none.orElse(() -> Option.some("other"));
    assertTrue(resultNone.isSome());
    assertEquals("other", resultNone.unwrap());
  }

  @Test
  void testFilter() {
    Option<Integer> some = Option.some(5);
    Option<Integer> filtered = some.filter(n -> n > 3);
    assertTrue(filtered.isSome());
    assertEquals(5, filtered.unwrap());

    Option<Integer> filteredNone = some.filter(n -> n > 10);
    assertTrue(filteredNone.isNone());

    Option<Integer> none = Option.none();
    Option<Integer> noneFiltered = none.filter(n -> n > 3);
    assertTrue(noneFiltered.isNone());
  }

  @Test
  void testXor() {
    Option<String> some1 = Option.some("test1");
    Option<String> some2 = Option.some("test2");
    Option<String> none = Option.none();

    Option<String> result1 = some1.xor(none);
    assertTrue(result1.isSome());
    assertEquals("test1", result1.unwrap());

    Option<String> result2 = none.xor(some1);
    assertTrue(result2.isSome());
    assertEquals("test1", result2.unwrap());

    Option<String> result3 = some1.xor(some2);
    assertTrue(result3.isNone());

    Option<String> result4 = none.xor(none);
    assertTrue(result4.isNone());
  }

  @Test
  void testToOptional() {
    Option<String> some = Option.some("test");
    Optional<String> optional = some.toOptional();
    assertTrue(optional.isPresent());
    assertEquals("test", optional.get());

    Option<String> none = Option.none();
    Optional<String> optionalNone = none.toOptional();
    assertFalse(optionalNone.isPresent());
  }

  @Test
  void testInspect() {
    StringBuilder sb = new StringBuilder();
    Option<String> some = Option.some("test");
    Option<String> inspected = some.inspect(s -> sb.append(s));
    assertEquals("test", sb.toString());
    assertTrue(inspected.isSome());
    assertEquals("test", inspected.unwrap());

    Option<String> none = Option.none();
    Option<String> inspectedNone = none.inspect(s -> sb.append("should not be called"));
    assertEquals("test", sb.toString()); // Should not have changed
    assertTrue(inspectedNone.isNone());
  }

  @Test
  void testMapOr() {
    Option<String> some = Option.some("test");
    int result = some.mapOr(0, String::length);
    assertEquals(4, result);

    Option<String> none = Option.none();
    int resultNone = none.mapOr(0, String::length);
    assertEquals(0, resultNone);
  }

  @Test
  void testMapOrElse() {
    Option<String> some = Option.some("test");
    int result = some.mapOrElse(() -> 0, String::length);
    assertEquals(4, result);

    Option<String> none = Option.none();
    int resultNone = none.mapOrElse(() -> 0, String::length);
    assertEquals(0, resultNone);
  }

  @Test
  void testOkOr() {
    Option<String> some = Option.some("test");
    var result = some.okOr("error");
    assertTrue(result.isOk());
    assertEquals("test", result.unwrap());

    Option<String> none = Option.none();
    var resultNone = none.okOr("error");
    assertTrue(resultNone.isErr());
    assertEquals("error", resultNone.unwrapErr());
  }

  @Test
  void testOkOrElse() {
    Option<String> some = Option.some("test");
    var result = some.okOrElse(() -> "error");
    assertTrue(result.isOk());
    assertEquals("test", result.unwrap());

    Option<String> none = Option.none();
    var resultNone = none.okOrElse(() -> "error");
    assertTrue(resultNone.isErr());
    assertEquals("error", resultNone.unwrapErr());
  }

  @Test
  void testIter() {
    Option<String> some = Option.some("test");
    var iterable = some.iter();
    var iterator = iterable.iterator();
    assertTrue(iterator.hasNext());
    assertEquals("test", iterator.next());
    assertFalse(iterator.hasNext());

    Option<String> none = Option.none();
    var iterableNone = none.iter();
    var iteratorNone = iterableNone.iterator();
    assertFalse(iteratorNone.hasNext());
  }

  @Test
  void testIsSomeAnd() {
    Option<Integer> some = Option.some(5);
    assertTrue(some.isSomeAnd(n -> n > 3));
    assertFalse(some.isSomeAnd(n -> n > 10));

    Option<Integer> none = Option.none();
    assertFalse(none.isSomeAnd(n -> n > 3));
  }

  @Test
  void testIsNoneOr() {
    Option<Integer> some = Option.some(5);
    assertFalse(some.isNoneOr(n -> n > 3));
    assertTrue(some.isNoneOr(n -> n > 10));

    Option<Integer> none = Option.none();
    assertTrue(none.isNoneOr(n -> n > 3));
  }
}
