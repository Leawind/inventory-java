package io.github.leawind.inventory.dep;

import java.util.stream.Stream;

/**
 * An ordered registry based on dependency relationships.
 *
 * <p>This interface defines the core contract for registering elements and their dependencies.
 * Elements can be registered at any time; topological sorting is computed lazily on the first call
 * to {@link Owned#stream()} and re-computed on subsequent calls if the registry has been modified
 * since the last sort.
 *
 * <p><strong>Not thread-safe</strong>: This interface and its default implementation are not
 * synchronized; external locking is required in multithreaded environments.
 *
 * @param <K> the unique key type
 * @param <T> the registered element type
 */
public interface DependencyRegistry<K, T> {

  /**
   * Registers an element with the given dependencies. If an element with the same key already
   * exists, it will be replaced.
   *
   * @param key the unique key of the element
   * @param value the element to register
   * @param dependencies the dependencies of the element
   */
  void register(K key, T value, Dependencies<K> dependencies);

  /**
   * Registers an element with no specific order dependencies. If an element with the same key
   * already exists, it will be replaced.
   *
   * @param key the unique key of the element
   * @param value the element to register
   */
  void register(K key, T value);

  /**
   * Represents ownership of the registry, providing access to resolved elements.
   *
   * @param <K> the unique key type
   * @param <T> the registered element type
   */
  interface Owned<K, T> extends DependencyRegistry<K, T> {

    /**
     * Computes or re-computes the topological sort eagerly.
     *
     * <p>If the registry has not been modified since the last sort, this method is a no-op. This is
     * useful for eager validation of dependencies without consuming the stream.
     *
     * @throws CyclicDependencyException if a cyclic dependency is detected
     */
    void sort();

    /**
     * Returns a stream of registered elements in resolved topological order.
     *
     * <p>The topological sort is computed lazily on the first call and re-computed on subsequent
     * calls if the registry has been modified since the last sort.
     *
     * @return a stream of elements
     * @throws CyclicDependencyException if a cyclic dependency is detected
     */
    Stream<T> stream();

    /**
     * Returns an iterable of registered elements in resolved topological order.
     *
     * <p>The topological sort is computed lazily on the first call and re-computed on subsequent
     * calls if the registry has been modified since the last sort.
     *
     * @return an iterable of elements
     * @throws CyclicDependencyException if a cyclic dependency is detected
     */
    Iterable<T> iterable();
  }

  static <K, T> Owned<K, T> create() {
    return new DependencyRegistryImpl<>();
  }
}
