package io.github.leawind.inventory.event;

import io.github.leawind.inventory.event.impl.DependencyRegistryImpl;
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
 * synchronized; external locking is required in multi-threaded environments.
 *
 * @param <K> the unique key type
 * @param <T> the registered element type
 */
public interface DependencyRegistry<K, T> {

  /**
   * Registers an element with no specific order dependencies.
   *
   * @param key the unique key of the element
   * @param value the element to register
   * @throws IllegalStateException if the key already exists
   */
  void register(K key, T value);

  /**
   * Starts a fluent API chain to register an element with dependencies.
   *
   * @return a dependency builder
   */
  Builder<K, T> withDependencies();

  /**
   * A builder for registering an element with dependency constraints.
   *
   * @param <K> the unique key type
   * @param <T> the registered element type
   */
  interface Builder<K, T> {

    /**
     * Specifies that the element being registered must appear before the given key.
     *
     * @param key the target element's key
     * @return the current builder for chaining
     */
    Builder<K, T> before(K key);

    /**
     * Specifies that the element being registered must appear after the given key.
     *
     * @param key the target element's key
     * @return the current builder for chaining
     */
    Builder<K, T> after(K key);

    /**
     * Registers the element with the previously specified dependencies.
     *
     * @param key the unique key of the element
     * @param value the element to register
     * @throws IllegalStateException if the key already exists
     */
    void register(K key, T value);
  }

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
  }

  static <K, T> Owned<K, T> create() {
    return new DependencyRegistryImpl<>();
  }
}
