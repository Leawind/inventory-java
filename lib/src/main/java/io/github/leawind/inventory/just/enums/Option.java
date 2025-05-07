package io.github.leawind.inventory.just.enums;

import io.github.leawind.inventory.tuple.Tuple;
import io.github.leawind.inventory.tuple.Tuple.Tuple2;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.annotation.Nullable;

/**
 * A type that represents either success (Ok) or failure (Err).
 *
 * @param <T> The type of the value if `Ok`
 */
public final class Option<T> {
  /** Some value of type `T`. */
  public static <T> Option<T> some(T value) {
    return new Option<>(value);
  }

  /** No value. */
  public static <T> Option<T> none() {
    return new Option<>(null);
  }

  public static <T> Option<T> from(T value) {
    return some(value);
  }

  private @Nullable T value;

  private Option(@Nullable T value) {
    this.value = value;
  }

  /////////////////////////////////////////////////////////////////////////
  // Querying the contained values
  /////////////////////////////////////////////////////////////////////////

  /** Returns true if the option is a `Some` value. */
  public boolean isSome() {
    return value != null;
  }

  /** Returns true if the option is a `Some` value and matches the given predicate. */
  public boolean isSomeAnd(Predicate<T> fn) {
    return isSome() && fn.test(value);
  }

  /** Returns true if the option is a `None` value. */
  public boolean isNone() {
    return value == null;
  }

  /**
   * Returns true if the option is a `None` value or if the option is a `Some` value and matches the
   * given predicate.
   */
  public boolean isNoneOr(Predicate<T> fn) {
    return isNone() || fn.test(value);
  }

  /////////////////////////////////////////////////////////////////////////
  // Getting to contained values
  /////////////////////////////////////////////////////////////////////////

  /**
   * Returns the contained `Some` value, consuming the `Option`.
   *
   * @param msg The message to print if the option is `None`.
   */
  public T expect(String msg) {
    if (isNone()) {
      throw new RuntimeException(msg);
    }
    return value;
  }

  /**
   * Returns the value of this Option if it is Some, otherwise throws a RuntimeException with a
   * default message.
   */
  public T unwrap() {
    if (isNone()) {
      throw new RuntimeException("called `Option::unwrap()` on a `None` value");
    }
    return value;
  }

  /** Returns the value of this Option if it is Some, otherwise returns the default value. */
  public T unwrapOr(T defaultValue) {
    return isSome() ? value : defaultValue;
  }

  /**
   * Returns the value of this Option if it is Some, otherwise returns the result of the supplier.
   *
   * @param fn A function to compute a default value if this is None
   * @return The value of the result from the supplier
   */
  public T unwrapOrElse(Supplier<T> fn) {
    return isSome() ? value : fn.get();
  }

  /////////////////////////////////////////////////////////////////////////
  // Transforming contained values
  /////////////////////////////////////////////////////////////////////////

  /** Applies a function to the value of this Option if it is Some, otherwise returns None. */
  public <U> Option<U> map(Function<T, U> fn) {
    return isSome() ? Option.some(fn.apply(value)) : Option.none();
  }

  /** Applies a consumer to the value of this Option if it is Some. */
  public Option<T> inspect(Consumer<T> fn) {
    if (isSome()) {
      fn.accept(value);
    }
    return this;
  }

  /**
   * Returns the result of applying a function to the value of this Option if it is Some, otherwise
   * returns the default value.
   */
  public <U> U mapOr(U defaultValue, Function<T, U> fn) {
    return isSome() ? fn.apply(value) : defaultValue;
  }

  /**
   * Returns the result of applying a function to the value of this Option if it is Some, otherwise
   * returns the default value.
   *
   * @param fn The function to apply to the value
   */
  public <D> D mapOrElse(D defaultValue, Function<T, D> fn) {
    return isSome() ? fn.apply(value) : defaultValue;
  }

  /**
   * Converts this Option into a Result with Ok if it's Some, otherwise Err with the given error.
   */
  public <E> Result<T, E> okOr(E error) {
    return isSome() ? Result.ok(value) : Result.err(error);
  }

  /**
   * Converts this Option into a Result with Ok if it's Some, otherwise Err with the result of the
   * supplier.
   */
  public <E> Result<T, E> okOrElse(Supplier<E> fn) {
    return isSome() ? Result.ok(value) : Result.err(fn.get());
  }

  /////////////////////////////////////////////////////////////////////////
  // Boolean operations on the values, eager and lazy
  /////////////////////////////////////////////////////////////////////////

  /** Returns `optb` if the option is `Some`, otherwise returns `self`. */
  public <U> Option<U> and(Option<U> optb) {
    return isSome() ? optb : Option.none();
  }

  public <U> Option<U> andThen(Function<T, Option<U>> fn) {
    return isSome() ? fn.apply(value) : Option.none();
  }

  public Option<T> filter(Predicate<T> fn) {
    return isSome() && fn.test(value) ? this : Option.none();
  }

  public Option<T> or(Option<T> optb) {
    return isSome() ? this : optb;
  }

  public Option<T> orElse(Supplier<Option<T>> fn) {
    return isSome() ? this : fn.get();
  }

  public Option<T> xor(Option<T> optb) {
    if (isSome() ^ optb.isSome()) {
      return isSome() ? this : optb;
    }
    return none();
  }

  /////////////////////////////////////////////////////////////////////////
  // Entry-like operations to insert v1 value and return v1 reference
  /////////////////////////////////////////////////////////////////////////

  public T insert(T value) {
    this.value = value;
    return value;
  }

  public T getOrInsert(T value) {
    return getOrInsertWith(() -> value);
  }

  public T getOrInsertWith(Supplier<T> fn) {
    return isNone() ? value = fn.get() : value;
  }

  /////////////////////////////////////////////////////////////////////////
  // Misc
  /////////////////////////////////////////////////////////////////////////

  /** Takes the value out of the option, leaving `none` in its place. */
  public Option<T> take() {
    var old = copied();
    value = null;
    return old;
  }

  public Option<T> takeIf(Predicate<T> fn) {
    if (mapOr(false, fn::test)) {
      return take();
    } else {
      return none();
    }
  }

  public Option<T> replace(T value) {
    var old = copied();
    this.value = value;
    return old;
  }

  public <U> Option<Tuple2<T, U>> zip(Option<U> other) {
    return isSome() && other.isSome() ? some(Tuple.of(value, other.value)) : none();
  }

  public <U, R> Option<R> zipWith(Option<U> other, BiFunction<T, U, R> fn) {
    return isSome() && other.isSome() ? some(fn.apply(value, other.value)) : none();
  }

  @SuppressWarnings("unchecked")
  public <U> Option<U> flatten() {
    return isSome() ? value instanceof Option<?> inner ? inner.flatten() : some((U) value) : none();
  }

  public <U> Option<U> flatten(Class<U> clazz) {
    var opt = flatten();
    if (opt.isNone()) {
      return none();
    }
    assert opt.value != null;
    if (!clazz.isAssignableFrom(opt.value.getClass())) {
      throw new ClassCastException("Type mismatch");
    }
    return some(clazz.cast(opt.value));
  }

  public Option<T> copied() {
    return new Option<>(value);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Option<?> other) {
      if (isSome() != other.isSome()) {
        return false;
      }

      if (isSome()) {
        return value.equals(other.value);
      }

      return true;
    }
    return false;
  }
}
