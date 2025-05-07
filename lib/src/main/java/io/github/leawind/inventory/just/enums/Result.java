package io.github.leawind.inventory.just.enums;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class Result<T, E> {

  public static <T, E> Result<T, E> ok(T result) {
    return new Result<>(result);
  }

  public static <T, E> Result<T, E> err(E error) {
    return new Result<>(error, true);
  }

  private final @Nullable T result;
  private final @Nullable E error;

  private Result(@Nonnull T result) {
    this.result = result;
    this.error = null;
  }

  private Result(@Nonnull E result, boolean isErr) {
    this.error = result;
    this.result = null;
  }

  /////////////////////////////////////////////////////////////////////////
  // Querying the contained values
  /////////////////////////////////////////////////////////////////////////

  public boolean isOk() {
    return result != null;
  }

  public boolean isOkAnd(Predicate<T> fn) {
    return isOk() && fn.test(result);
  }

  public boolean isErr() {
    return error != null;
  }

  public boolean isErrAnd(Predicate<E> fn) {
    return isErr() && fn.test(error);
  }

  /////////////////////////////////////////////////////////////////////////
  // Adapter for each variant
  /////////////////////////////////////////////////////////////////////////

  public Option<T> ok() {
    return isOk() ? Option.some(result) : Option.none();
  }

  public Option<E> err() {
    return isErr() ? Option.some(error) : Option.none();
  }

  /////////////////////////////////////////////////////////////////////////
  // Transforming contained values
  /////////////////////////////////////////////////////////////////////////

  public <U> Result<U, E> map(Function<T, U> fn) {
    return isOk() ? Result.ok(fn.apply(result)) : Result.err((error));
  }

  public <U> U mapOr(U defaultValue, Function<T, U> fn) {
    return isOk() ? fn.apply(result) : defaultValue;
  }

  public <U> U mapOrElse(Function<E, U> defaultGetter, Function<T, U> fn) {
    return isOk() ? fn.apply(result) : defaultGetter.apply(error);
  }

  public <F> Result<T, F> mapErr(Function<E, F> op) {
    return isOk() ? Result.ok(result) : Result.err(op.apply(error));
  }

  public Result<T, E> inspect(Consumer<T> fn) {
    if (isOk()) {
      fn.accept(result);
    }
    return this;
  }

  public Result<T, E> inspectErr(Consumer<E> fn) {
    if (isErr()) {
      fn.accept(error);
    }
    return this;
  }

  /////////////////////////////////////////////////////////////////////////
  // Extract a value
  /////////////////////////////////////////////////////////////////////////
  public T expect(String msg) {
    if (isErr()) {
      throw new RuntimeException(msg);
    }
    return result;
  }

  public T unwrap() {
    if (isErr()) {
      throw new RuntimeException("called `Result::unwrap()` on an `Err` value" + error);
    }
    return result;
  }

  public E expectErr(String msg) {
    if (isOk()) {
      throw new RuntimeException(msg);
    }
    return error;
  }

  public E unwrapErr() {
    if (isOk()) {
      throw new RuntimeException("called `Result::unwrapErr()` on an `Ok` value" + result);
    }
    return error;
  }

  ////////////////////////////////////////////////////////////////////////
  // Boolean operations on the values, eager and lazy
  /////////////////////////////////////////////////////////////////////////

  public <U> Result<U, E> and(Result<U, E> res) {
    return isOk() ? res : err(error);
  }

  public <U> Result<U, E> andThen(Function<T, Result<U, E>> op) {
    return isOk() ? op.apply(result) : err(error);
  }

  public <F> Result<T, F> or(Result<T, F> res) {
    return isOk() ? ok(result) : res;
  }

  public <U> Result<T, U> orElse(Function<E, Result<T, U>> op) {
    return isOk() ? ok(result) : op.apply(error);
  }

  public T unwrapOr(T defaultValue) {
    return isOk() ? result : defaultValue;
  }

  public T unwrapOrElse(Function<E, T> op) {
    return isOk() ? result : op.apply(error);
  }

  ////////////////////////////////////////////////////////////////////////
  // Other
  /////////////////////////////////////////////////////////////////////////

  public Result<T, E> copied() {
    return isOk() ? Result.ok(result) : Result.err(error);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Result<?, ?> other) {
      if (isOk() != other.isOk()) {
        return false;
      }

      if (isOk()) {
        return result.equals(other.result);
      }

      return Objects.equals(error, other.error);
    }
    return false;
  }

  @SuppressWarnings("unchecked")
  public <U> Result<U, ?> flatten() {
    return isOk()
        ? result instanceof Result<?, ?> inner ? inner.flatten() : ok((U) result)
        : err(error);
  }

  public <U> Result<U, ?> flatten(Class<U> clazz) {
    var res = flatten();

    if (res.isErr()) {
      return err(res.error);
    }

    assert res.result != null;
    if (!clazz.isAssignableFrom(res.result.getClass())) {
      throw new ClassCastException("Type mismatch");
    }
    return ok(clazz.cast(res.result));
  }
}
