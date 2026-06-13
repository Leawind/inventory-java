package io.github.leawind.inventory.event.impl;

import io.github.leawind.inventory.event.CyclicDependencyException;
import io.github.leawind.inventory.event.DependencyRegistry;
import java.util.*;
import java.util.stream.Stream;

public class DependencyRegistryImpl<K, T> implements DependencyRegistry.Owned<K, T> {

  private record Entry<K, T>(K key, T value, Set<K> beforeKeys, Set<K> afterKeys) {}

  private final Map<K, Entry<K, T>> entries = new LinkedHashMap<>();
  private List<T> sortedCache = new ArrayList<>();
  private boolean dirty = false;

  @Override
  public void register(K key, T value) {
    Objects.requireNonNull(key, "key");
    Objects.requireNonNull(value, "value");
    if (entries.containsKey(key)) {
      throw new IllegalStateException("Duplicate listener key: " + key);
    }
    entries.put(key, new Entry<>(key, value, Collections.emptySet(), Collections.emptySet()));
    dirty = true;
  }

  @Override
  public Builder<K, T> withDependencies() {
    return new BuilderImpl();
  }

  @Override
  public void sort() {
    sortedList();
  }

  @Override
  public Stream<T> stream() {
    return sortedList().stream();
  }

  protected List<T> sortedList() {
    if (dirty) {
      rebuild();
      dirty = false;
    }
    return sortedCache;
  }

  // -------------------- Builder Implementation --------------------

  private class BuilderImpl implements Builder<K, T> {
    private final Set<K> beforeKeys = new LinkedHashSet<>();
    private final Set<K> afterKeys = new LinkedHashSet<>();

    @Override
    public Builder<K, T> before(K key) {
      Objects.requireNonNull(key, "before key");
      beforeKeys.add(key);
      return this;
    }

    @Override
    public Builder<K, T> after(K key) {
      Objects.requireNonNull(key, "after key");
      afterKeys.add(key);
      return this;
    }

    @Override
    public void register(K key, T value) {
      Objects.requireNonNull(key, "key");
      Objects.requireNonNull(value, "value");
      if (entries.containsKey(key)) {
        throw new IllegalStateException("Duplicate listener key: " + key);
      }
      entries.put(
          key,
          new Entry<>(
              key,
              value,
              Collections.unmodifiableSet(new LinkedHashSet<>(beforeKeys)),
              Collections.unmodifiableSet(new LinkedHashSet<>(afterKeys))));
      dirty = true;
    }
  }

  // -------------------- Topological Sorting Implementation --------------------

  private void rebuild() {
    Map<K, Set<K>> adj = new HashMap<>(entries.size());
    Map<K, Integer> inDegree = new HashMap<>(entries.size());

    for (K key : entries.keySet()) {
      adj.put(key, new LinkedHashSet<>());
      inDegree.put(key, 0);
    }

    for (Entry<K, T> entry : entries.values()) {
      K u = entry.key;

      // u.before(v): u must come before v → edge u → v
      for (K v : entry.beforeKeys) {
        addEdge(u, v, adj, inDegree, entries.containsKey(v));
      }
      // u.after(v): v must come before u → edge v → u
      for (K v : entry.afterKeys) {
        addEdge(v, u, adj, inDegree, entries.containsKey(v));
      }
    }

    Deque<K> queue = new ArrayDeque<>();
    for (Map.Entry<K, Integer> e : inDegree.entrySet()) {
      if (e.getValue() == 0) {
        queue.add(e.getKey());
      }
    }

    List<T> result = new ArrayList<>(entries.size());
    Set<K> visited = new HashSet<>();
    while (!queue.isEmpty()) {
      K cur = queue.poll();
      visited.add(cur);
      result.add(entries.get(cur).value);
      for (K next : adj.get(cur)) {
        int d = inDegree.merge(next, -1, Integer::sum);
        if (d == 0) {
          queue.add(next);
        }
      }
    }

    if (visited.size() != entries.size()) {
      Set<K> cycleNodes = new LinkedHashSet<>(entries.keySet());
      cycleNodes.removeAll(visited);
      throw new CyclicDependencyException(cycleNodes);
    }

    sortedCache = new ArrayList<>(result);
  }

  private void addEdge(
      K from, K to, Map<K, Set<K>> adj, Map<K, Integer> inDegree, boolean toExists) {
    if (!toExists) {
      return;
    }
    if (adj.get(from).add(to)) {
      inDegree.merge(to, 1, Integer::sum);
    }
  }
}
