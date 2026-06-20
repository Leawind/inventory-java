package io.github.leawind.inventory.dep;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public final class DependenciesImpl<K> implements Dependencies.Mutable<K> {

  @SuppressWarnings("rawtypes")
  public static final Dependencies EMPTY = new DependenciesImpl<>(Set.of(), Set.of());

  private final Set<K> beforeKeys;
  private final Set<K> afterKeys;

  public DependenciesImpl(Set<K> beforeKeys, Set<K> afterKeys) {
    this.beforeKeys = beforeKeys;
    this.afterKeys = afterKeys;
  }

  public DependenciesImpl() {
    this(new HashSet<>(), new HashSet<>());
  }

  @Override
  public Collection<K> getBefore() {
    return Collections.unmodifiableSet(beforeKeys);
  }

  @Override
  public Collection<K> getAfter() {
    return Collections.unmodifiableSet(afterKeys);
  }

  @Override
  public Mutable<K> before(K id) {
    beforeKeys.add(Objects.requireNonNull(id));
    return this;
  }

  @Override
  public Mutable<K> after(K id) {
    afterKeys.add(Objects.requireNonNull(id));
    return this;
  }
}
