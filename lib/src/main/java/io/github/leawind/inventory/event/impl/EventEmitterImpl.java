package io.github.leawind.inventory.event.impl;

import io.github.leawind.inventory.dep.DependencyRegistryImpl;
import io.github.leawind.inventory.event.BiListener;
import io.github.leawind.inventory.event.EventEmitter;

public class EventEmitterImpl<E, K>
    extends DependencyRegistryImpl<K, BiListener<E, EventEmitter.Control>>
    implements EventEmitter.Owned<E, K> {

  public EventEmitterImpl() {
    super();
  }

  public void emit(E event) {
    ControlImpl control = new ControlImpl();

    var it = sortedList().iterator();
    while (it.hasNext()) {
      control.reset();
      BiListener<E, Control> listener = it.next();
      listener.on(event, control);
      if (control.shouldUnregister) {
        it.remove();
      }
      if (control.shouldStop) {
        break;
      }
    }
  }

  private static class ControlImpl implements Control {
    boolean shouldStop;
    boolean shouldUnregister;

    void reset() {
      shouldStop = false;
      shouldUnregister = false;
    }

    @Override
    public void stop() {
      shouldStop = true;
    }

    @Override
    public void unregister() {
      shouldUnregister = true;
    }
  }
}
