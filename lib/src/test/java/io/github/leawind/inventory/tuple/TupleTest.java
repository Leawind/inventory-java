package io.github.leawind.inventory.tuple;

import org.junit.jupiter.api.Test;

public class TupleTest {
  @Test
  public void testHardTuple() {
    var t = Tuple.of(234, false, "Hello");
    assert (t.v1() == 234);
    assert (t.v2() == false);
  }
}
