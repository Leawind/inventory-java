package io.github.leawind.inventory.event;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class DependencyRegistryTest {

  @Test
  void shouldRegisterAndStreamElementsWithoutDependencies() {
    DependencyRegistry.Owned<String, String> registry = DependencyRegistry.create();
    registry.register("A", "ValueA");
    registry.register("B", "ValueB");
    registry.register("C", "ValueC");

    List<String> result = registry.stream().collect(Collectors.toList());
    assertEquals(List.of("ValueA", "ValueB", "ValueC"), result);
  }

  @Test
  void shouldOrderElementsBasedOnAfterDependency() {
    DependencyRegistry.Owned<String, String> registry = DependencyRegistry.create();
    registry.register("A", "ValueA");
    registry.withDependencies().after("A").register("B", "ValueB");

    List<String> result = registry.stream().collect(Collectors.toList());
    assertEquals(List.of("ValueA", "ValueB"), result);
  }

  @Test
  void shouldOrderElementsBasedOnBeforeDependency() {
    DependencyRegistry.Owned<String, String> registry = DependencyRegistry.create();
    registry.withDependencies().before("B").register("A", "ValueA");
    registry.register("B", "ValueB");

    List<String> result = registry.stream().collect(Collectors.toList());
    assertEquals(List.of("ValueA", "ValueB"), result);
  }

  @Test
  void shouldResolveComplexDependencies() {
    DependencyRegistry.Owned<String, String> registry = DependencyRegistry.create();

    registry.withDependencies().after("B").register("C", "ValueC");
    registry.withDependencies().before("B").register("A", "ValueA");
    registry.register("B", "ValueB");
    registry.withDependencies().after("A").before("C").register("D", "ValueD");

    List<String> result = registry.stream().toList();

    int indexA = result.indexOf("ValueA");
    int indexB = result.indexOf("ValueB");
    int indexC = result.indexOf("ValueC");
    int indexD = result.indexOf("ValueD");

    assertTrue(indexA < indexB);
    assertTrue(indexB < indexC);
    assertTrue(indexA < indexD);
    assertTrue(indexD < indexC);
  }

  @Test
  void shouldThrowExceptionOnDuplicateKey() {
    DependencyRegistry.Owned<String, String> registry = DependencyRegistry.create();
    registry.register("A", "ValueA");

    assertThrows(IllegalStateException.class, () -> registry.register("A", "ValueA2"));
  }

  @Test
  void shouldThrowExceptionOnDuplicateKeyWithDependencies() {
    DependencyRegistry.Owned<String, String> registry = DependencyRegistry.create();
    registry.register("A", "ValueA");

    assertThrows(
        IllegalStateException.class,
        () -> registry.withDependencies().after("A").register("A", "ValueA2"));
  }

  @Test
  void shouldThrowExceptionOnCyclicDependency() {
    DependencyRegistry.Owned<String, String> registry = DependencyRegistry.create();
    registry.withDependencies().after("B").register("A", "ValueA");
    registry.withDependencies().after("A").register("B", "ValueB");

    assertThrows(CyclicDependencyException.class, () -> registry.stream().toList());
  }

  @Test
  void shouldHandleSelfDependencyAsCyclic() {
    DependencyRegistry.Owned<String, String> registry = DependencyRegistry.create();
    registry.withDependencies().after("A").register("A", "ValueA");

    assertThrows(CyclicDependencyException.class, () -> registry.stream().toList());
  }

  @Test
  void shouldIgnoreMissingDependency() {
    DependencyRegistry.Owned<String, String> registry = DependencyRegistry.create();
    registry.withDependencies().after("Missing").register("A", "ValueA");

    List<String> result = registry.stream().collect(Collectors.toList());
    assertEquals(List.of("ValueA"), result);
  }

  @Test
  void shouldAllowRegistrationAfterStream() {
    DependencyRegistry.Owned<String, String> registry = DependencyRegistry.create();
    registry.register("A", "ValueA");

    assertEquals(List.of("ValueA"), registry.stream().toList());

    registry.register("B", "ValueB");
    assertEquals(List.of("ValueA", "ValueB"), registry.stream().toList());
  }

  @Test
  void shouldThrowExceptionWhenBuilderRegistersDuplicateKey() {
    DependencyRegistry.Owned<String, String> registry = DependencyRegistry.create();
    registry.register("A", "ValueA");

    DependencyRegistry.Builder<String, String> builder = registry.withDependencies().before("A");
    assertThrows(IllegalStateException.class, () -> builder.register("A", "ValueA2"));
  }
}
