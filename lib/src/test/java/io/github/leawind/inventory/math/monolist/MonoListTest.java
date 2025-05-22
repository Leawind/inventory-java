package io.github.leawind.inventory.math.monolist;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class MonoListTest {
  @Nested
  class SignTest {
    @Test
    void test() {
      assertThat(StaticMonoList.of(new double[] {5, 5, 4, 3, 2}).sign(), is(-1));
      assertThat(StaticMonoList.of(new double[] {3, 3, 4, 5, 6}).sign(), is(1));

      assertThat(DeferedMonoList.of(new double[] {5, 5, 4, 3, 2}).sign(), is(-1));
      assertThat(DeferedMonoList.of(new double[] {3, 3, 4, 5, 6}).sign(), is(1));
    }
  }

  @Nested
  class LinearMonoListImplTest {
    static Stream<MonoList> provideLinearMonoListImpl() {
      return Stream.of(StaticMonoList.linear(10), DeferedMonoList.linear(10));
    }

    @ParameterizedTest
    @MethodSource("provideLinearMonoListImpl")
    void testMonoListBasic(MonoList list) {
      assertThat(list.length(), is(10));
      assertThat(list.sign(), is(1));

      assertThat(list.get(0), is(0d));
      assertThat(list.get(1), is(1d));
    }

    @ParameterizedTest
    @MethodSource("provideLinearMonoListImpl")
    void nearestIndexTest(MonoList list) {
      assertThat(list.nearestIndex(0.9), is(1));
      assertThat(list.nearestIndex(5.5), is(6));
      assertThat(list.nearestIndex(7.4), is(7));
    }

    @ParameterizedTest
    @MethodSource("provideLinearMonoListImpl")
    void nearestValueTest(MonoList list) {
      assertThat(list.nearestValue(0.9), is(1d));
      assertThat(list.nearestValue(5.5), is(6d));
      assertThat(list.nearestValue(7.4), is(7d));
    }

    @ParameterizedTest
    @MethodSource("provideLinearMonoListImpl")
    void offsetValueTest(MonoList list) {
      assertThat(list.offsetValue(3.2, 2), is(5d));
      assertThat(list.offsetValue(3.2, 0), is(3d));
      assertThat(list.offsetValue(3.2, -2), is(1d));

      assertThat(list.offsetValue(5.5, 3), is(9d));
      assertThat(list.offsetValue(5.5, -3), is(3d));
    }

    @ParameterizedTest
    @MethodSource("provideLinearMonoListImpl")
    void nextValueTest(MonoList list) {
      assertThat(list.nextValue(3.2), is(4d));
      assertThat(list.nextValue(8.7), is(9d));
    }

    @ParameterizedTest
    @MethodSource("provideLinearMonoListImpl")
    void previousValueTest(MonoList list) {
      assertThat(list.previousValue(3.2), is(2d));
      assertThat(list.previousValue(8.7), is(8d));
    }
  }
}
