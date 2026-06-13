package io.github.leawind.inventory.event;

public interface BiListener<E, C> {
  void on(E e, C c);

  interface NoArg<E, C> extends BiListener<E, C> {
    void on();

    default void on(E e, C c) {
      on();
    }
  }

  interface OneArg<E, C> extends BiListener<E, C> {

    void on(E e);

    default void on(E e, C c) {
      on(e);
    }
  }
}
