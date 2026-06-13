package io.github.leawind.inventory.event.impl;

import io.github.leawind.inventory.event.SimpleEventEmitter;
import java.util.ArrayList;
import java.util.Collection;
import org.jspecify.annotations.Nullable;

public class SimpleEventEmitterImpl<E> implements SimpleEventEmitter.Owner<E> {

  private final Collection<Listener<E>> listeners;

  public SimpleEventEmitterImpl() {
    this.listeners = new ArrayList<>();
  }

  public SimpleEventEmitterImpl(Collection<Listener<E>> listeners) {
    this.listeners = listeners;
  }

  @Override
  public SimpleEventEmitter<E> clear() {
    listeners.clear();
    return this;
  }

  @Override
  public SimpleEventEmitter<E> on(Listener<E> listener) {
    listeners.add(listener);
    return this;
  }

  @Override
  public SimpleEventEmitter<E> on(Listener.NoArg<E> listener) {
    listeners.add(listener);
    return this;
  }

  @Override
  public void emit() {
    emit(null);
  }

  @Override
  public void emit(@Nullable E event) {
    listeners.forEach(listener -> listener.on(event));
  }
}
