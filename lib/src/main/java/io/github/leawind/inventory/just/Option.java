package io.github.leawind.inventory.just;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * @see <a href="https://doc.rust-lang.org/core/option/enum.Option.html">`Option` on
 *     doc.rust-lang.org</a>
 */
public sealed interface Option<T> {

  @SuppressWarnings("unchecked")
  static <T> Option<T> some(T value) {
    if (value == null) {
      return (Option<T>) Some.EMPTY;
    }
    return new Some<>(value);
  }

  @SuppressWarnings("unchecked")
  static <T> Option<T> none() {
    return (Option<T>) None.INSTANCE;
  }

  Optional<T> toOptional();

  // //////////////////////////////////////////////////////////////////////
  // Querying the contained values
  // //////////////////////////////////////////////////////////////////////

  boolean isSome();

  boolean isSomeAnd(Predicate<T> fn);

  boolean isNone();

  boolean isNoneOr(Predicate<T> fn);

  // //////////////////////////////////////////////////////////////////////
  // Getting to contained values
  // //////////////////////////////////////////////////////////////////////

  T expect(String msg);

  T unwrap();

  T unwrapOr(T defaultValue);

  T unwrapOrElse(Supplier<T> fn);

  // //////////////////////////////////////////////////////////////////////
  // Transforming contained values
  // //////////////////////////////////////////////////////////////////////
  <U> Option<U> map(Function<T, U> fn);

  Option<T> inspect(Consumer<T> fn);

  <U> U mapOr(U defaultValue, Function<T, U> fn);

  <U> U mapOrElse(Supplier<U> defaultValue, Function<T, U> fn);

  <E> Result<T, E> okOr(E error);

  <E> Result<T, E> okOrElse(Supplier<E> fn);

  // //////////////////////////////////////////////////////////////////////
  // Iterator constructors
  // //////////////////////////////////////////////////////////////////////

  Iterable<T> iter();

  // //////////////////////////////////////////////////////////////////////
  // Boolean operations on the values, eager and lazy
  // //////////////////////////////////////////////////////////////////////

  /** Returns `optb` if the option is `Some`, otherwise returns `self`. */
  <U> Option<U> and(Option<U> optb);

  <U> Option<U> andThen(Function<T, Option<U>> fn);

  Option<T> filter(Predicate<T> fn);

  Option<T> or(Option<T> optb);

  Option<T> orElse(Supplier<Option<T>> fn);

  Option<T> xor(Option<T> optb);

  final class Some<T> implements Option<T> {
    private final T value;

    private Some(T value) {
      this.value = value;
    }

    private static final Some<?> EMPTY = new Some<>(null);

    @Override
    public Optional<T> toOptional() {
      return Optional.ofNullable(value);
    }

    @Override
    public boolean isSome() {
      return true;
    }

    @Override
    public boolean isSomeAnd(Predicate<T> fn) {
      return fn.test(value);
    }

    @Override
    public boolean isNone() {
      return false;
    }

    @Override
    public boolean isNoneOr(Predicate<T> fn) {
      return !fn.test(value);
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
    public T unwrapOr(T defaultValue) {
      return value;
    }

    @Override
    public T unwrapOrElse(Supplier<T> fn) {
      return value;
    }

    @Override
    public <U> Option<U> map(Function<T, U> fn) {
      return some(fn.apply(value));
    }

    @Override
    public Option<T> inspect(Consumer<T> fn) {
      fn.accept(value);
      return this;
    }

    @Override
    public <U> U mapOr(U defaultValue, Function<T, U> fn) {
      return fn.apply(value);
    }

    @Override
    public <U> U mapOrElse(Supplier<U> defaultValue, Function<T, U> fn) {
      return fn.apply(value);
    }

    @Override
    public <E> Result<T, E> okOr(E error) {
      return Result.ok(value);
    }

    @Override
    public <E> Result<T, E> okOrElse(Supplier<E> fn) {
      return Result.ok(value);
    }

    @Override
    public Iterable<T> iter() {
      return List.of(value);
    }

    @Override
    public <U> Option<U> and(Option<U> optb) {
      return optb;
    }

    @Override
    public <U> Option<U> andThen(Function<T, Option<U>> fn) {
      return fn.apply(value);
    }

    @Override
    public Option<T> filter(Predicate<T> fn) {
      return fn.test(value) ? this : none();
    }

    @Override
    public Option<T> or(Option<T> optb) {
      return this;
    }

    @Override
    public Option<T> orElse(Supplier<Option<T>> fn) {
      return this;
    }

    @Override
    public Option<T> xor(Option<T> optb) {
      return optb.isSome() ? none() : this;
    }
  }

  final class None<T> implements Option<T> {
    private static final None<?> INSTANCE = new None<>();

    @SuppressWarnings("unchecked")
    public <U> Option<U> cast() {
      return (Option<U>) this;
    }

    @Override
    public Optional<T> toOptional() {
      return Optional.empty();
    }

    @Override
    public boolean isSome() {
      return false;
    }

    @Override
    public boolean isSomeAnd(Predicate<T> fn) {
      return false;
    }

    @Override
    public boolean isNone() {
      return true;
    }

    @Override
    public boolean isNoneOr(Predicate<T> fn) {
      return true;
    }

    @Override
    public T expect(String msg) {
      throw expectFailed(msg);
    }

    @Override
    public T unwrap() {
      throw unwrapFailed();
    }

    @Override
    public T unwrapOr(T defaultValue) {
      return defaultValue;
    }

    @Override
    public T unwrapOrElse(Supplier<T> fn) {
      return fn.get();
    }

    @Override
    public <U> Option<U> map(Function<T, U> fn) {
      return cast();
    }

    @Override
    public Option<T> inspect(Consumer<T> fn) {
      return this;
    }

    @Override
    public <U> U mapOr(U defaultValue, Function<T, U> fn) {
      return defaultValue;
    }

    @Override
    public <U> U mapOrElse(Supplier<U> defaultValue, Function<T, U> fn) {
      return defaultValue.get();
    }

    @Override
    public <E> Result<T, E> okOr(E error) {
      return Result.err(error);
    }

    @Override
    public <E> Result<T, E> okOrElse(Supplier<E> fn) {
      return Result.err(fn.get());
    }

    @Override
    public Iterable<T> iter() {
      return List.of();
    }

    @Override
    public <U> Option<U> and(Option<U> optb) {
      return cast();
    }

    @Override
    public <U> Option<U> andThen(Function<T, Option<U>> fn) {
      return cast();
    }

    @Override
    public Option<T> filter(Predicate<T> fn) {
      return this;
    }

    @Override
    public Option<T> or(Option<T> optb) {
      return optb;
    }

    @Override
    public Option<T> orElse(Supplier<Option<T>> fn) {
      return fn.get();
    }

    @Override
    public Option<T> xor(Option<T> optb) {
      return optb.isSome() ? optb : this;
    }
  }

  private static JustError unwrapFailed() {
    return JustError.panic("called `Option#unwrap()` on a `None` value");
  }

  private static JustError expectFailed(String msg) {
    return JustError.panic(msg);
  }
}
