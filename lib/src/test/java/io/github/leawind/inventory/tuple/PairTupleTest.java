package io.github.leawind.inventory.tuple;

import org.junit.jupiter.api.Test;

public class PairTupleTest {
  @Test
  public void test() {
    var t = PairTuple.of(123, "hello", false);

    assert (t.length == 3);

    assert (t.<Integer>get(0) == 123);
    assert (t.<String>get(1).equals("hello"));
    assert (t.<Boolean>get(2) == false);

    assert (t.get(0, Integer.class) == 123);
    assert (t.get(1, String.class).equals("hello"));
    assert (t.get(2, Boolean.class) == false);

    assert (t.<Integer>at(0) == 123);
    assert (t.<String>at(1).equals("hello"));
    assert (t.<Boolean>at(2) == false);

    assert (t.<Integer>at(-3) == 123);
    assert (t.<String>at(-2).equals("hello"));
    assert (t.<Boolean>at(-1) == false);

    var t4 = t.with(12138);
    assert (t4.length == 4);
    assert (t4.<Integer>get(0) == 123);
    assert (t4.<Integer>get(3).equals(12138));
  }
}
