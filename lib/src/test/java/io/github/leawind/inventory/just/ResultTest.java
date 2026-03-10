package io.github.leawind.inventory.just;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

public class ResultTest {
  @Test
  void testOk() {
    Result<String, Integer> okWithValue = Result.ok("test");
    assertTrue(okWithValue.isOk());
    assertFalse(okWithValue.isErr());
    assertEquals("test", okWithValue.unwrap());

    Result<String, Integer> okEmpty = Result.ok(null);
    assertTrue(okEmpty.isOk());
    assertNull(okEmpty.unwrap());
  }

  @Test
  void testErr() {
    Result<String, Integer> errWithValue = Result.err(404);
    assertFalse(errWithValue.isOk());
    assertTrue(errWithValue.isErr());
    assertEquals(404, errWithValue.unwrapErr());

    Result<String, Integer> errEmpty = Result.err(null);
    assertTrue(errEmpty.isErr());
    assertNull(errEmpty.unwrapErr());
  }

  @Test
  void testToOptional() {
    Result<String, Integer> okWithValue = Result.ok("test");
    assertTrue(okWithValue.toOptional().isPresent());
    assertEquals("test", okWithValue.toOptional().get());

    Result<String, Integer> okEmpty = Result.ok(null);
    assertFalse(okEmpty.toOptional().isPresent());

    Result<String, Integer> errWithValue = Result.err(404);
    assertFalse(errWithValue.toOptional().isPresent());
  }

  @Test
  void testIsOk() {
    assertTrue(Result.ok("value").isOk());
    assertFalse(Result.err("error").isOk());
  }

  @Test
  void testIsOkAnd() {
    assertTrue(Result.ok(5).isOkAnd(v -> v > 0));
    assertFalse(Result.ok(5).isOkAnd(v -> v < 0));
    assertFalse(Result.err("error").isOkAnd(v -> true));
  }

  @Test
  void testIsErr() {
    assertFalse(Result.ok("value").isErr());
    assertTrue(Result.err("error").isErr());
  }

  @Test
  void testIsErrAnd() {
    assertTrue(Result.err(5).isErrAnd(e -> e > 0));
    assertFalse(Result.err(5).isErrAnd(e -> e < 0));
    assertFalse(Result.ok("value").isErrAnd(e -> true));
  }

  @Test
  void testMap() {
    Result<Integer, String> ok = Result.ok(5);
    Result<String, String> mapped = ok.map(v -> "Value: " + v);
    assertTrue(mapped.isOk());
    assertEquals("Value: 5", mapped.unwrap());

    Result<Integer, String> err = Result.err("error");
    Result<String, String> mappedErr = err.map(v -> "Value: " + v);
    assertTrue(mappedErr.isErr());
    assertEquals("error", mappedErr.unwrapErr());
  }

  @Test
  void testMapOr() {
    Result<Integer, String> ok = Result.ok(5);
    String result = ok.mapOr("default", v -> "Value: " + v);
    assertEquals("Value: 5", result);

    Result<Integer, String> err = Result.err("error");
    String errResult = err.mapOr("default", v -> "Value: " + v);
    assertEquals("default", errResult);
  }

  @Test
  void testMapOrElse() {
    Result<Integer, String> ok = Result.ok(5);
    String result = ok.mapOrElse(e -> "Error: " + e, v -> "Value: " + v);
    assertEquals("Value: 5", result);

    Result<Integer, String> err = Result.err("not found");
    String errResult = err.mapOrElse(e -> "Error: " + e, v -> "Value: " + v);
    assertEquals("Error: not found", errResult);
  }

  @Test
  void testMapErr() {
    Result<String, Integer> ok = Result.ok("value");
    Result<String, String> mapped = ok.mapErr(e -> "Error: " + e);
    assertTrue(mapped.isOk());
    assertEquals("value", mapped.unwrap());

    Result<String, Integer> err = Result.err(404);
    Result<String, String> mappedErr = err.mapErr(e -> "Error: " + e);
    assertTrue(mappedErr.isErr());
    assertEquals("Error: 404", mappedErr.unwrapErr());
  }

  @Test
  void testInspect() {
    StringBuilder sb = new StringBuilder();
    Result<String, Integer> ok = Result.ok("test");
    Result<String, Integer> inspected = ok.inspect(sb::append);
    assertEquals("test", sb.toString());
    assertSame(ok, inspected);

    sb.setLength(0);
    Result<String, Integer> err = Result.err(404);
    Result<String, Integer> inspectedErr = err.inspect(sb::append);
    assertEquals("", sb.toString());
    assertSame(err, inspectedErr);
  }

  @Test
  void testInspectErr() {
    StringBuilder sb = new StringBuilder();
    Result<String, Integer> ok = Result.ok("test");
    Result<String, Integer> inspected = ok.inspectErr(sb::append);
    assertEquals("", sb.toString());
    assertSame(ok, inspected);

    sb.setLength(0);
    Result<String, Integer> err = Result.err(404);
    Result<String, Integer> inspectedErr = err.inspectErr(sb::append);
    assertEquals("404", sb.toString());
    assertSame(err, inspectedErr);
  }

  @Test
  void testIter() {
    var ok = Result.ok("test");
    assertEquals(List.of("test"), ok.iter());

    var err = Result.err(404);
    assertEquals(List.of(), err.iter());
  }

  @Test
  void testExpect() {
    Result<String, Integer> ok = Result.ok("test");
    assertEquals("test", ok.expect("Should be ok"));

    Result<String, Integer> err = Result.err(404);
    assertThrows(JustError.class, () -> err.expect("Should be ok"));
  }

  @Test
  void testUnwrap() {
    Result<String, Integer> ok = Result.ok("test");
    assertEquals("test", ok.unwrap());

    Result<String, Integer> err = Result.err(404);
    assertThrows(JustError.class, err::unwrap);
  }

  @Test
  void testExpectErr() {
    Result<String, Integer> err = Result.err(404);
    assertEquals(404, err.expectErr("Should be err"));

    Result<String, Integer> ok = Result.ok("test");
    assertThrows(JustError.class, () -> ok.expectErr("Should be err"));
  }

  @Test
  void testUnwrapErr() {
    Result<String, Integer> err = Result.err(404);
    assertEquals(404, err.unwrapErr());

    Result<String, Integer> ok = Result.ok("test");
    assertThrows(JustError.class, ok::unwrapErr);
  }

  @Test
  void testAnd() {
    Result<String, Integer> ok1 = Result.ok("test");
    Result<Integer, Integer> ok2 = Result.ok(42);
    Result<Integer, Integer> result = ok1.and(ok2);
    assertTrue(result.isOk());
    assertEquals(42, result.unwrap());

    Result<String, Integer> ok3 = Result.ok("test");
    Result<Integer, Integer> err = Result.err(404);
    Result<Integer, Integer> errResult = ok3.and(err);
    assertTrue(errResult.isErr());
    assertEquals(404, errResult.unwrapErr());

    Result<String, Integer> err2 = Result.err(500);
    Result<Integer, Integer> ok4 = Result.ok(42);
    Result<Integer, Integer> errResult2 = err2.and(ok4);
    assertTrue(errResult2.isErr());
    assertEquals(500, errResult2.unwrapErr());
  }

  @Test
  void testAndThen() {
    Result<Integer, String> ok = Result.ok(5);
    Result<String, String> result = ok.andThen(v -> Result.ok("Value: " + v));
    assertTrue(result.isOk());
    assertEquals("Value: 5", result.unwrap());

    Result<Integer, String> ok2 = Result.ok(5);
    Result<String, String> errResult = ok2.andThen(v -> Result.err("Error"));
    assertTrue(errResult.isErr());
    assertEquals("Error", errResult.unwrapErr());

    Result<Integer, String> err = Result.err("Original error");
    Result<String, String> errResult2 = err.andThen(v -> Result.ok("Value: " + v));
    assertTrue(errResult2.isErr());
    assertEquals("Original error", errResult2.unwrapErr());
  }

  @Test
  void testOr() {
    Result<String, Integer> ok1 = Result.ok("test");
    Result<String, Integer> ok2 = Result.ok("other");
    Result<String, Integer> result = ok1.or(ok2);
    assertTrue(result.isOk());
    assertEquals("test", result.unwrap());

    Result<String, Integer> err = Result.err(404);
    Result<String, Integer> ok3 = Result.ok("fallback");
    Result<String, Integer> errResult = err.or(ok3);
    assertTrue(errResult.isOk());
    assertEquals("fallback", errResult.unwrap());

    Result<String, Integer> err1 = Result.err(404);
    Result<String, Integer> err2 = Result.err(500);
    Result<String, Integer> errResult2 = err1.or(err2);
    assertTrue(errResult2.isErr());
    assertEquals(500, errResult2.unwrapErr());
  }

  @Test
  void testOrElse() {
    Result<String, Integer> ok = Result.ok("test");
    Result<String, Integer> result = ok.orElse(e -> Result.ok("fallback"));
    assertTrue(result.isOk());
    assertEquals("test", result.unwrap());

    Result<String, Integer> err = Result.err(404);
    Result<String, Integer> errResult = err.orElse(e -> Result.ok("fallback"));
    assertTrue(errResult.isOk());
    assertEquals("fallback", errResult.unwrap());

    Result<String, Integer> err2 = Result.err(404);
    Result<String, Integer> errResult2 = err2.orElse(e -> Result.err(500));
    assertTrue(errResult2.isErr());
    assertEquals(500, errResult2.unwrapErr());
  }

  @Test
  void testUnwrapOr() {
    Result<String, Integer> ok = Result.ok("test");
    assertEquals("test", ok.unwrapOr("fallback"));

    Result<String, Integer> err = Result.err(404);
    assertEquals("fallback", err.unwrapOr("fallback"));
  }

  @Test
  void testUnwrapOrElse() {
    Result<String, Integer> ok = Result.ok("test");
    assertEquals("test", ok.unwrapOrElse(e -> "Error: " + e));

    Result<String, Integer> err = Result.err(404);
    assertEquals("Error: 404", err.unwrapOrElse(e -> "Error: " + e));
  }

  @Test
  void testCopied() {
    Result<String, Integer> ok = Result.ok("test");
    Result<String, Integer> copied = ok.copied();
    assertSame(ok, copied);

    Result<String, Integer> err = Result.err(404);
    Result<String, Integer> copiedErr = err.copied();
    assertSame(err, copiedErr);
  }

  @Test
  void testOkEquals() {
    Result<String, Integer> ok1 = Result.ok("test");
    Result<String, Integer> ok2 = Result.ok("test");
    Result<String, Integer> ok3 = Result.ok("other");
    Result<String, Integer> err = Result.err(404);

    assertEquals(ok1, ok2); // Value equality
    assertNotEquals(ok1, ok3);
    assertNotEquals(ok1, err);
  }

  @Test
  void testErrEquals() {
    Result<String, Integer> err1 = Result.err(404);
    Result<String, Integer> err2 = Result.err(404);
    Result<String, Integer> err3 = Result.err(500);
    Result<String, Integer> ok = Result.ok("test");

    assertEquals(err1, err2); // Value equality
    assertNotEquals(err1, err3);
    assertNotEquals(err1, ok);
  }

  @Test
  void testToString() {
    Result<String, Integer> ok = Result.ok("test");
    assertEquals("Ok(test)", ok.toString());

    Result<String, Integer> err = Result.err(404);
    assertEquals("Err(404)", err.toString());

    Result<String, Integer> okEmpty = Result.ok(null);
    assertEquals("Ok(null)", okEmpty.toString());

    Result<String, Integer> errEmpty = Result.err(null);
    assertEquals("Err(null)", errEmpty.toString());
  }
}
