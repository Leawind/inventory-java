package io.github.leawind.inventory.just;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.leawind.inventory.just.enums.Result;
import org.junit.jupiter.api.Test;

public class ResultTest {

  @Test
  public void testCreation() {
    // Test ok()
    Result<String, Integer> okResult = Result.ok("success");
    assertTrue(okResult.isOk());
    assertEquals("success", okResult.unwrap());

    // Test err()
    Result<String, Integer> errResult = Result.err(404);
    assertTrue(errResult.isErr());
    assertEquals(404, (int) errResult.unwrapErr());
  }

  @Test
  public void testQueryMethods() {
    Result<String, Integer> ok = Result.ok("valid");
    Result<String, Integer> err = Result.err(500);

    // Test isOk()
    assertTrue(ok.isOk());
    assertFalse(err.isOk());

    // Test isErr()
    assertFalse(ok.isErr());
    assertTrue(err.isErr());

    // Test isOkAnd()
    assertTrue(ok.isOkAnd(s -> s.length() == 5));
    assertFalse(ok.isOkAnd(s -> s.length() > 10));

    // Test isErrAnd()
    assertTrue(err.isErrAnd(e -> e == 500));
    assertFalse(err.isErrAnd(e -> e < 400));
  }

  @Test
  public void testAdapterMethods() {
    Result<String, Integer> ok = Result.ok("data");
    Result<String, Integer> err = Result.err(404);

    // Test ok()
    assertTrue(ok.ok().isSome());
    assertEquals("data", ok.ok().unwrap());
    assertTrue(err.ok().isNone());

    // Test err()
    assertTrue(err.err().isSome());
    assertEquals(404, (int) err.err().unwrap());
    assertTrue(ok.err().isNone());
  }

  @Test
  public void testTransformationMethods() {
    Result<Integer, String> ok = Result.ok(42);
    Result<Integer, String> err = Result.err("failure");

    // Test map()
    Result<String, String> mappedOk = ok.map(i -> "Value: " + i);
    assertTrue(mappedOk.isOk());
    assertEquals("Value: 42", mappedOk.unwrap());

    Result<String, String> mappedErr = err.map(i -> "Value: " + i);
    assertTrue(mappedErr.isErr());
    assertEquals("failure", mappedErr.unwrapErr());

    // Test mapOr()
    assertEquals("Value: 42", ok.mapOr("Default", i -> "Value: " + i));
    assertEquals("Default", err.mapOr("Default", i -> "Value: " + i));

    // Test mapOrElse()
    assertEquals("Value: 42", ok.mapOrElse(e -> "Error: " + e, i -> "Value: " + i));
    assertEquals("Error: failure", err.mapOrElse(e -> "Error: " + e, i -> "Value: " + i));

    // Test mapErr()
    Result<Integer, Integer> mappedErrCode = err.mapErr(String::length);
    assertTrue(mappedErrCode.isErr());
    assertEquals(7, (int) mappedErrCode.unwrapErr());

    // Test inspect()
    StringBuilder sb = new StringBuilder();
    ok.inspect(i -> sb.append("Inspected: ").append(i));
    assertEquals("Inspected: 42", sb.toString());

    // Test inspectErr()
    StringBuilder sbErr = new StringBuilder();
    err.inspectErr(e -> sbErr.append("Error: ").append(e));
    assertEquals("Error: failure", sbErr.toString());
  }

  @Test
  public void testExpect() {
    assertThrows(
        RuntimeException.class,
        () -> {
          Result<String, Integer> err = Result.err(500);
          err.expect("This should throw");
        });
  }

  @Test
  public void testUnwrapOnErr() {
    assertThrows(
        RuntimeException.class,
        () -> {
          Result<String, Integer> err = Result.err(400);
          err.unwrap();
        });
  }

  @Test
  public void testExpectErr() {
    assertThrows(
        RuntimeException.class,
        () -> {
          Result<String, Integer> ok = Result.ok("valid");
          ok.expectErr("This should throw");
        });
  }

  @Test
  public void testUnwrapErrOnOk() {
    assertThrows(
        RuntimeException.class,
        () -> {
          Result<String, Integer> ok = Result.ok("valid");
          ok.unwrapErr();
        });
  }

  @Test
  public void testBooleanOperations() {
    Result<Integer, String> ok1 = Result.ok(10);
    Result<Integer, String> err = Result.err("error");

    Result<String, String> ok2 = Result.ok("other");
    Result<Integer, Integer> ok3 = Result.ok(15);
    Result<Integer, String> err2 = Result.err("other error");

    // Test and()
    assertTrue(ok1.and(ok2).isOk());
    assertEquals("other", ok1.and(ok2).unwrap());
    assertTrue(ok1.and(err2).isErr());
    assertTrue(err.and(ok2).isErr());

    // Test andThen()
    Result<String, String> andThenOk = ok1.andThen(i -> Result.ok("Number: " + i));
    assertTrue(andThenOk.isOk());
    assertEquals("Number: 10", andThenOk.unwrap());
    assertTrue(err.andThen(i -> Result.ok("Number: " + i)).isErr());

    // Test or()
    assertTrue(ok1.or(err2).isOk());
    assertEquals(10, (int) ok1.or(err2).unwrap());
    assertTrue(err.or(ok3).isOk());
    assertEquals(15, err.or(ok3).unwrap());

    // Test orElse()
    Result<Integer, String> orElseResult = err.orElse(e -> Result.ok(e.length()));
    assertTrue(orElseResult.isOk());
    assertEquals(5, (int) orElseResult.unwrap());
  }

  @Test
  public void testUnwrapVariants() {
    Result<String, Integer> ok = Result.ok("success");
    Result<String, Integer> err = Result.err(404);

    // Test unwrapOr()
    assertEquals("success", ok.unwrapOr("default"));
    assertEquals("default", err.unwrapOr("default"));

    // Test unwrapOrElse()
    assertEquals("success", ok.unwrapOrElse(e -> "Error: " + e));
    assertEquals("Error: 404", err.unwrapOrElse(e -> "Error: " + e));
  }

  @Test
  public void testCopied() {
    Result<String, Integer> original = Result.ok("original");
    Result<String, Integer> copied = original.copied();

    assertEquals(original, copied);
    assertNotSame(original, copied);
  }

  @Test
  public void testFlatten() {
    Result<Result<String, Integer>, Integer> nestedOk = Result.ok(Result.ok("nested"));
    Result<Result<String, Integer>, Integer> nestedErr = Result.ok(Result.err(500));
    Result<Result<String, Integer>, Integer> outerErr = Result.err(404);

    // Test flatten()
    Result<String, ?> flattenedOk = nestedOk.flatten();
    assertTrue(flattenedOk.isOk());
    assertEquals("nested", flattenedOk.unwrap());

    Result<String, ?> flattenedErr = nestedErr.flatten();
    assertTrue(flattenedErr.isErr());
    assertEquals(500, flattenedErr.unwrapErr());

    Result<String, ?> flattenedOuterErr = outerErr.flatten();
    assertTrue(flattenedOuterErr.isErr());
    assertEquals(404, flattenedOuterErr.unwrapErr());

    // Test flatten(Class)
    Result<String, ?> typedFlatten = nestedOk.flatten(String.class);
    assertTrue(typedFlatten.isOk());
    assertEquals("nested", typedFlatten.unwrap());
  }

  @Test
  public void testFlattenWithWrongType() {
    assertThrows(
        ClassCastException.class,
        () -> {
          Result<Result<Integer, String>, String> nested = Result.ok(Result.ok(42));
          nested.flatten(String.class); // Should throw ClassCastException
        });
  }

  @Test
  public void testEquals() {
    Result<String, Integer> ok1 = Result.ok("test");
    Result<String, Integer> ok2 = Result.ok("test");
    Result<String, Integer> okDiff = Result.ok("different");
    Result<String, Integer> err1 = Result.err(404);
    Result<String, Integer> err2 = Result.err(404);
    Result<String, Integer> errDiff = Result.err(500);

    assertEquals(ok1, ok2);
    assertNotEquals(ok1, okDiff);
    assertNotEquals(ok1, err1);
    assertEquals(err1, err2);
    assertNotEquals(err1, errDiff);
    assertNotEquals("not a result", ok1.unwrap());
  }
}
