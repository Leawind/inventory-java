package io.github.leawind.inventory.type;

import java.util.List;

public class UnsafeTypeUtils {
  @SuppressWarnings("unchecked")
  public static <R, A> R forceCast(A value) {
    return (R) value;
  }

  @SuppressWarnings("unchecked")
  public static <R extends A, A> R cast(A obj) {
    return (R) obj;
  }

  @SuppressWarnings("unchecked")
  public static <R, A> List<R> cast(List<A> obj) {
    return (List<R>) obj;
  }
}
