package io.github.leawind.inventory.tuple;

import javax.annotation.Nullable;

public final class PairTuple {

  public static class Pair<A, B> {
    private final A a;
    private final B b;

    private final boolean aIsPair;
    private final boolean bIsPair;

    private final @Nullable Pair<?, ?> pairA;
    private final @Nullable Pair<?, ?> pairB;

    public final int length;

    private Pair(A a, B b) {
      this.a = a;
      this.b = b;

      aIsPair = a instanceof Pair;
      bIsPair = b instanceof Pair;

      pairA = aIsPair ? (Pair<?, ?>) a : null;
      pairB = bIsPair ? (Pair<?, ?>) b : null;

      int length = 0;
      length += a instanceof Pair ? ((Pair<?, ?>) a).length : 1;
      length += b instanceof Pair ? ((Pair<?, ?>) b).length : 1;
      this.length = length;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(int index) {
      validateIndex(index);

      if (!aIsPair && !bIsPair) {
        return (T) (index == 0 ? a : b);
      }

      if (aIsPair) {
        assert pairA != null;
        if (index < pairA.length) {
          return pairA.get(index);
        }
        if (bIsPair) {
          assert pairB != null;
          return pairB.get(index - pairA.length);
        }
        return (T) b;
      } else {
        if (index == 0) {
          return (T) a;
        }
        assert pairB != null;
        return pairB.get(index - 1);
      }
    }

    public <T> T get(int index, Class<T> clazz) {
      validateIndex(index);
      var value = get(index);
      if (!clazz.isAssignableFrom(value.getClass())) {
        throw new ClassCastException("Type mismatch at index " + index);
      }
      return clazz.cast(value);
    }

    public <T> T at(int index) {
      index = normalizeIndex(index);
      validateIndex(index);
      return get(index);
    }

    public <T> T at(int index, Class<T> clazz) {
      index = normalizeIndex(index);
      validateIndex(index);
      return get(index, clazz);
    }

    public <T> Pair<Pair<A, B>, T> with(T value) {
      return new Pair<>(this, value);
    }

    private boolean isIndexValid(int index) {
      return index >= 0 && index < length;
    }

    private void validateIndex(int index) {
      if (!isIndexValid(index)) {
        throw new IndexOutOfBoundsException("Index: " + index + ", Length: " + length);
      }
    }

    private int normalizeIndex(int index) {
      if (index < 0) {
        return length + index;
      }
      return index;
    }
  }

  public static <T1, T2> Pair<T1, T2> of(T1 v1, T2 v2) {
    return new Pair<>(v1, v2);
  }

  public static <T1, T2, T3> Pair<Pair<T1, T2>, T3> of(T1 v1, T2 v2, T3 v3) {
    return of(of(v1, v2), v3);
  }

  public static <T1, T2, T3, T4> Pair<Pair<Pair<T1, T2>, T3>, T4> of(T1 v1, T2 v2, T3 v3, T4 v4) {
    return of(of(of(v1, v2), v3), v4);
  }

  public static <T1, T2, T3, T4, T5> Pair<Pair<Pair<Pair<T1, T2>, T3>, T4>, T5> of(
      T1 v1, T2 v2, T3 v3, T4 v4, T5 v5) {
    return of(of(of(of(v1, v2), v3), v4), v5);
  }

  public static <T1, T2, T3, T4, T5, T6> Pair<Pair<Pair<Pair<Pair<T1, T2>, T3>, T4>, T5>, T6> of(
      T1 v1, T2 v2, T3 v3, T4 v4, T5 v5, T6 v6) {
    return of(of(of(of(of(v1, v2), v3), v4), v5), v6);
  }

  public static <T1, T2, T3, T4, T5, T6, T7>
      Pair<Pair<Pair<Pair<Pair<Pair<T1, T2>, T3>, T4>, T5>, T6>, T7> of(
          T1 v1, T2 v2, T3 v3, T4 v4, T5 v5, T6 v6, T7 v7) {
    return of(of(of(of(of(of(v1, v2), v3), v4), v5), v6), v7);
  }

  public static <T1, T2, T3, T4, T5, T6, T7, T8>
      Pair<Pair<Pair<Pair<Pair<Pair<Pair<T1, T2>, T3>, T4>, T5>, T6>, T7>, T8> of(
          T1 v1, T2 v2, T3 v3, T4 v4, T5 v5, T6 v6, T7 v7, T8 v8) {
    return of(of(of(of(of(of(of(v1, v2), v3), v4), v5), v6), v7), v8);
  }
}
