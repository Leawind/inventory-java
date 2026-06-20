package io.github.leawind.inventory.dep;

import java.util.*;

public interface Dependencies<K> {

  static <K> Mutable<K> create() {
    return new DependenciesImpl<>();
  }

  static <K> Dependencies<K> of(Collection<K> before, Collection<K> after) {
    return new DependenciesImpl<>(Set.copyOf(before), Set.copyOf(after));
  }

  Collection<K> getBefore();

  Collection<K> getAfter();

  interface Mutable<K> extends Dependencies<K> {
    Mutable<K> before(K id);

    Mutable<K> after(K id);
  }
}
