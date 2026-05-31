package io.github.leawind.inventory.just;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class JustTest {

  Result<Byte, String> add(byte a, byte b) {
    int s = (int) a + (int) b;
    if (s < Byte.MIN_VALUE || s > Byte.MAX_VALUE) {
      return Result.err("overflow");
    }
    return Result.ok((byte) s);
  }

  void toPanic() {
    throw JustError.panic("Panic!!");
  }

  @Test
  void examples() {
    var a = (byte) 127;
    var b = (byte) 1;
    var c = add(a, b);
    assertEquals(Result.err("overflow"), c);

    assertThrows(JustError.class, this::toPanic);
  }
}
