package io.github.leawind.inventory.just;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import org.jspecify.annotations.NonNull;

/**
 * @see <a href="https://doc.rust-lang.org/core/result/enum.Result.html">`Result` on
 *     doc.rust-lang.org</a>
 */
public sealed interface Result<T, E> permits Result.Ok, Result.Err {

  @SuppressWarnings("unchecked")
  static <T, E> Ok<T, E> ok(T value) {
    if (value == null) {
      return (Ok<T, E>) Ok.EMPTY;
    }
    return new Ok<>(value);
  }

  @SuppressWarnings("unchecked")
  static <T, E> Err<T, E> err(E error) {
    if (error == null) {
      return (Err<T, E>) Err.EMPTY;
    }
    return new Err<>(error);
  }

  Optional<T> toOptional();

  // ///////////////////////////////////////////////////////////////////////
  // Querying the contained values
  // ///////////////////////////////////////////////////////////////////////

  boolean isOk();

  boolean isOkAnd(Predicate<T> fn);

  boolean isErr();

  boolean isErrAnd(Predicate<E> fn);

  // //////////////////////////////////////////////////////////////////////
  // Adapter for each variant
  // //////////////////////////////////////////////////////////////////////

  Option<T> ok();

  Option<E> err();

  // ///////////////////////////////////////////////////////////////////////
  // Transforming contained values
  // ///////////////////////////////////////////////////////////////////////

  <U> Result<U, E> map(Function<T, U> fn);

  <U> U mapOr(U defaultValue, Function<T, U> fn);

  <U> U mapOrElse(Function<E, U> defaultGetter, Function<T, U> fn);

  <F> Result<T, F> mapErr(Function<E, F> op);

  Result<T, E> inspect(Consumer<T> fn);

  Result<T, E> inspectErr(Consumer<E> fn);

  // //////////////////////////////////////////////////////////////////////
  // Iterator constructors
  // //////////////////////////////////////////////////////////////////////

  Iterable<T> iter();

  // //////////////////////////////////////////////////////////////////////
  // Extract a value
  // //////////////////////////////////////////////////////////////////////

  T expect(String msg);

  T unwrap();

  E expectErr(String msg);

  E unwrapErr();

  // /////////////////////////////////////////////////////////////////////
  // Boolean operations on the values, eager and lazy
  // //////////////////////////////////////////////////////////////////////

  <U> Result<U, E> and(Result<U, E> res);

  <U> Result<U, E> andThen(Function<T, Result<U, E>> op);

  <F> Result<T, F> or(Result<T, F> res);

  <U> Result<T, U> orElse(Function<E, Result<T, U>> op);

  T unwrapOr(T defaultValue);

  T unwrapOrElse(Function<E, T> op);

  // /////////////////////////////////////////////////////////////////////
  // Other
  // //////////////////////////////////////////////////////////////////////

  default Result<T, E> copied() {
    return this;
  }

  record Ok<T, E>(T value) implements Result<T, E> {

    private static final Ok<?, ?> EMPTY = new Ok<>(null);

    @SuppressWarnings("unchecked")
    public <U> Ok<T, U> cast() {
      return (Ok<T, U>) this;
    }

    @Override
    public Optional<T> toOptional() {
      return Optional.ofNullable(value);
    }

    @Override
    public boolean isOk() {
      return true;
    }

    @Override
    public boolean isOkAnd(Predicate<T> fn) {
      return fn.test(value);
    }

    @Override
    public boolean isErr() {
      return false;
    }

    @Override
    public boolean isErrAnd(Predicate<E> fn) {
      return false;
    }

    @Override
    public Option<T> ok() {
      return Option.some(value);
    }

    @Override
    public Option<E> err() {
      return Option.none();
    }

    @Override
    public <U> Result<U, E> map(Function<T, U> fn) {
      return Result.ok(fn.apply(value));
    }

    @Override
    public <U> U mapOr(U defaultValue, Function<T, U> fn) {
      return fn.apply(value);
    }

    @Override
    public <U> U mapOrElse(Function<E, U> defaultGetter, Function<T, U> fn) {
      return fn.apply(value);
    }

    @Override
    public <F> Result<T, F> mapErr(Function<E, F> op) {
      return cast();
    }

    @Override
    public Result<T, E> inspect(Consumer<T> fn) {
      fn.accept(value);
      return this;
    }

    @Override
    public Result<T, E> inspectErr(Consumer<E> fn) {
      return this;
    }

    @Override
    public Iterable<T> iter() {
      return List.of(value);
    }

    @Override
    public T expect(String msg) {
      return value;
    }

    @Override
    public T unwrap() {
      return value;
    }

    @Override
    public E expectErr(String msg) {
      throw unwrapFailed(msg, value);
    }

    @Override
    public E unwrapErr() {
      throw unwrapFailed("called `Result#unwrapErr()` on an `Ok` value", value);
    }

    @Override
    public <U> Result<U, E> and(Result<U, E> res) {
      return res;
    }

    @Override
    public <U> Result<U, E> andThen(Function<T, Result<U, E>> op) {
      return op.apply(value);
    }

    @Override
    public <F> Result<T, F> or(Result<T, F> res) {
      return cast();
    }

    @Override
    public <U> Result<T, U> orElse(Function<E, Result<T, U>> op) {
      return cast();
    }

    @Override
    public T unwrapOr(T defaultValue) {
      return value;
    }

    @Override
    public T unwrapOrElse(Function<E, T> op) {
      return value;
    }

    @NonNull
    @Override
    public String toString() {
      return "Ok(" + value + ")";
    }
  }

  record Err<T, E>(E error) implements Result<T, E> {
    private static final Err<?, ?> EMPTY = new Err<>(null);

    @SuppressWarnings("unchecked")
    public <U> Err<U, E> cast() {
      return (Err<U, E>) this;
    }

    @Override
    public Optional<T> toOptional() {
      return Optional.empty();
    }

    @Override
    public boolean isOk() {
      return false;
    }

    @Override
    public boolean isOkAnd(Predicate<T> fn) {
      return false;
    }

    @Override
    public boolean isErr() {
      return true;
    }

    @Override
    public boolean isErrAnd(Predicate<E> fn) {
      return fn.test(error);
    }

    @Override
    public Option<T> ok() {
      return Option.none();
    }

    @Override
    public Option<E> err() {
      return Option.some(error);
    }

    @Override
    public <U> Result<U, E> map(Function<T, U> fn) {
      return cast();
    }

    @Override
    public <U> U mapOr(U defaultValue, Function<T, U> fn) {
      return defaultValue;
    }

    @Override
    public <U> U mapOrElse(Function<E, U> defaultGetter, Function<T, U> fn) {
      return defaultGetter.apply(error);
    }

    @Override
    public <F> Result<T, F> mapErr(Function<E, F> op) {
      return Result.err(op.apply(error));
    }

    @Override
    public Result<T, E> inspect(Consumer<T> fn) {
      return this;
    }

    @Override
    public Result<T, E> inspectErr(Consumer<E> fn) {
      fn.accept(error);
      return this;
    }

    @Override
    public Iterable<T> iter() {
      return List.of();
    }

    @Override
    public T expect(String msg) {
      throw unwrapFailed(msg, error);
    }

    @Override
    public T unwrap() {
      throw unwrapFailed("called `Result#unwrap()` on an `Err` value", error);
    }

    @Override
    public E expectErr(String msg) {
      return error;
    }

    @Override
    public E unwrapErr() {
      return error;
    }

    @Override
    public <U> Result<U, E> and(Result<U, E> res) {
      return cast();
    }

    @Override
    public <U> Result<U, E> andThen(Function<T, Result<U, E>> op) {
      return cast();
    }

    @Override
    public <F> Result<T, F> or(Result<T, F> res) {
      return res;
    }

    @Override
    public <U> Result<T, U> orElse(Function<E, Result<T, U>> op) {
      return op.apply(error);
    }

    @Override
    public T unwrapOr(T defaultValue) {
      return defaultValue;
    }

    @Override
    public T unwrapOrElse(Function<E, T> op) {
      return op.apply(error);
    }

    @NonNull
    @Override
    public String toString() {
      return "Err(" + error + ")";
    }
  }

  private static JustError unwrapFailed(String message, Object error) {
    return JustError.panic("%s: %s", message, error);
  }
}
