package io.github.leawind.inventory.just.enums;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.annotation.Nullable;

public final class Result<T, E> {

  private final boolean isOk;
  private final @Nullable Object value;

  private Result(boolean isOk, @Nullable Object value) {
    this.isOk = isOk;
    this.value = value;
  }

  @SuppressWarnings("unchecked")
  private <Y> Y value() {
    return (Y) value;
  }

  // ///////////////////////////////////////////////////////////////////////
  // Querying the contained values
  // ///////////////////////////////////////////////////////////////////////

  public boolean isOk() {
    return isOk;
  }

  public boolean isOkAnd(Predicate<T> fn) {
    return isOk && fn.test(value());
  }

  public boolean isErr() {
    return !isOk;
  }

  public boolean isErrAnd(Predicate<E> fn) {
    return !isOk && fn.test(value());
  }

  // //////////////////////////////////////////////////////////////////////
  // Adapter for each variant
  // //////////////////////////////////////////////////////////////////////

  public Option<T> ok() {
    return isOk ? Option.some(value()) : Option.none();
  }

  public Option<E> err() {
    return !isOk ? Option.some(value()) : Option.none();
  }

  // ///////////////////////////////////////////////////////////////////////
  // Transforming contained values
  // ///////////////////////////////////////////////////////////////////////

  public <U> Result<U, E> map(Function<T, U> fn) {
    return isOk ? Result.Ok(fn.apply(value())) : Result.Err(value());
  }

  public <U> U mapOr(U defaultValue, Function<T, U> fn) {
    return isOk ? fn.apply(value()) : defaultValue;
  }

  public <U> U mapOrElse(Function<E, U> defaultGetter, Function<T, U> fn) {
    return isOk ? fn.apply(value()) : defaultGetter.apply(value());
  }

  public <F> Result<T, F> mapErr(Function<E, F> op) {
    return isOk ? Result.Ok(value()) : Result.Err(op.apply(value()));
  }

  public Result<T, E> inspect(Consumer<T> fn) {
    if (isOk) {
      fn.accept(value());
    }
    return this;
  }

  public Result<T, E> inspectErr(Consumer<E> fn) {
    if (!isOk) {
      fn.accept(value());
    }
    return this;
  }

  // //////////////////////////////////////////////////////////////////////
  // Extract a value
  // //////////////////////////////////////////////////////////////////////
  @SuppressWarnings("unchecked")
  public @Nullable T expect(String msg) {
    if (!isOk) {
      throw new RuntimeException(msg);
    }
    return (T) value;
  }

  @SuppressWarnings("unchecked")
  public @Nullable T unwrap() {
    if (!isOk) {
      throw new RuntimeException("called `Result::unwrap()` on an `Err` value" + value);
    }
    return (T) value;
  }

  @SuppressWarnings("unchecked")
  public @Nullable E expectErr(String msg) {
    if (isOk) {
      throw new RuntimeException(msg);
    }
    return (E) value;
  }

  @SuppressWarnings("unchecked")
  public @Nullable E unwrapErr() {
    if (isOk) {
      throw new RuntimeException("called `Result::unwrapErr()` on an `Ok` value" + value);
    }
    return (E) value;
  }

  // /////////////////////////////////////////////////////////////////////
  // Boolean operations on the values, eager and lazy
  // //////////////////////////////////////////////////////////////////////

  public <U> Result<U, E> and(Result<U, E> res) {
    return isOk ? res : Err(value());
  }

  public <U> Result<U, E> andThen(Function<T, Result<U, E>> op) {
    return isOk ? op.apply(value()) : Err(value());
  }

  public <F> Result<T, F> or(Result<T, F> res) {
    return isOk ? Ok(value()) : res;
  }

  public <U> Result<T, U> orElse(Function<E, Result<T, U>> op) {
    return isOk ? Ok(value()) : op.apply(value());
  }

  public T unwrapOr(T defaultValue) {
    return isOk ? value() : defaultValue;
  }

  public T unwrapOrElse(Function<E, T> op) {
    return isOk ? value() : op.apply(value());
  }

  // /////////////////////////////////////////////////////////////////////
  // Other
  // //////////////////////////////////////////////////////////////////////

  public Result<T, E> copied() {
    return isOk ? Result.Ok(value()) : Result.Err(value());
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Result<?, ?> other) {
      return isOk == other.isOk && Objects.equals(value, other.value);
    }
    return false;
  }

  public <U> Result<U, ?> flatten() {
    return isOk
        ? value instanceof Result<?, ?> inner ? inner.flatten() : Ok(value())
        : Err(value());
  }

  public <U> Result<U, ?> flatten(Class<U> clazz) {
    var res = flatten();

    if (res.isErr()) {
      return Err(res.value);
    }

    assert res.value != null;
    if (!clazz.isAssignableFrom(res.value.getClass())) {
      throw new ClassCastException("Type mismatch");
    }
    return Ok(clazz.cast(res.value));
  }

  public static <T, E> Result<T, E> Ok(@Nullable T result) {
    return new Result<>(true, result);
  }

  public static <T, E> Result<T, E> Err(@Nullable E error) {
    return new Result<>(false, error);
  }

  private static <E> Result<Void, E> Ok() {
    return new Result<>(true, null);
  }

  private static <E> Result<Void, E> Err() {
    return new Result<>(false, null);
  }

  public static Result<Void, Throwable> from(Runnable runnable) {
    try {
      runnable.run();
      return Ok();
    } catch (Throwable e) {
      return Err(e);
    }
  }

  public static <T> Result<T, Throwable> from(Supplier<T> supplier) {
    try {
      return Ok(supplier.get());
    } catch (Throwable e) {
      return Err(e);
    }
  }
}
