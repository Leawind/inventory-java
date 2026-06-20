package io.github.leawind.inventory.dep;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;

class DependencyRegistryTest {

  @Test
  void shouldRegisterAndStreamElementsWithoutDependencies() {
    DependencyRegistry.Owned<String, String> registry = DependencyRegistry.create();
    registry.register("A", "ValueA");
    registry.register("B", "ValueB");
    registry.register("C", "ValueC");

    List<String> result = registry.stream().toList();
    assertEquals(List.of("ValueA", "ValueB", "ValueC"), result);
  }

  @Test
  void shouldOrderElementsBasedOnAfterDependency() {
    DependencyRegistry.Owned<String, String> registry = DependencyRegistry.create();
    registry.register("B", "ValueB", Dependencies.<String>create().after("A"));
    registry.register("A", "ValueA");

    List<String> result = registry.stream().toList();
    assertEquals(List.of("ValueA", "ValueB"), result);
  }

  @Test
  void shouldOrderElementsBasedOnBeforeDependency() {
    DependencyRegistry.Owned<String, String> registry = DependencyRegistry.create();
    registry.register("B", "ValueB");
    registry.register("A", "ValueA", Dependencies.<String>create().before("B"));

    List<String> result = registry.stream().toList();
    assertEquals(List.of("ValueA", "ValueB"), result);
  }

  @Test
  void shouldResolveComplexDependencies() {
    int elementCount = 50;
    int maxLayer = 8;
    int maxDepsPerElement = 4;
    Random rng = new Random(42);

    String[] keys = new String[elementCount];
    for (int i = 0; i < elementCount; i++) {
      keys[i] = "K" + i;
    }

    int[] layer = new int[elementCount];
    for (int i = 0; i < elementCount; i++) {
      layer[i] = rng.nextInt(maxLayer);
    }

    DependencyRegistry.Owned<String, String> registry = DependencyRegistry.create();
    Map<String, Dependencies<String>> depsMap = new LinkedHashMap<>();

    for (int i = 0; i < elementCount; i++) {
      List<String> candidatesAfter = new ArrayList<>();
      List<String> candidatesBefore = new ArrayList<>();
      for (int j = 0; j < elementCount; j++) {
        if (i == j) continue;
        if (layer[j] < layer[i]) candidatesAfter.add(keys[j]);
        else if (layer[j] > layer[i]) candidatesBefore.add(keys[j]);
      }
      Collections.shuffle(candidatesAfter, rng);
      Collections.shuffle(candidatesBefore, rng);

      Dependencies.Mutable<String> d = Dependencies.create();
      int afterCount = Math.min(rng.nextInt(maxDepsPerElement + 1), candidatesAfter.size());
      for (int n = 0; n < afterCount; n++) d.after(candidatesAfter.get(n));
      int beforeCount = Math.min(rng.nextInt(maxDepsPerElement + 1), candidatesBefore.size());
      for (int n = 0; n < beforeCount; n++) d.before(candidatesBefore.get(n));

      depsMap.put(keys[i], d);
    }

    List<String> shuffled = Arrays.asList(keys);
    Collections.shuffle(shuffled, rng);
    for (String key : shuffled) {
      registry.register(key, "V_" + key, depsMap.get(key));
    }

    List<String> result = registry.stream().toList();
    assertEquals(elementCount, result.size());

    Map<String, Integer> index = new HashMap<>();
    for (int i = 0; i < result.size(); i++) {
      index.put(result.get(i), i);
    }

    for (Map.Entry<String, Dependencies<String>> entry : depsMap.entrySet()) {
      String key = entry.getKey();
      Dependencies<String> d = entry.getValue();
      int idx = index.get("V_" + key);

      for (String afterKey : d.getAfter()) {
        assertTrue(
            index.get("V_" + afterKey) < idx, "'" + key + "' should be after '" + afterKey + "'");
      }
      for (String beforeKey : d.getBefore()) {
        assertTrue(
            index.get("V_" + beforeKey) > idx,
            "'" + key + "' should be before '" + beforeKey + "'");
      }
    }
  }

  @Test
  void shouldOverwriteValueOnDuplicateKey() {
    DependencyRegistry.Owned<String, String> registry = DependencyRegistry.create();
    registry.register("A", "ValueA");
    registry.register("A", "ValueA2");
    assertEquals(List.of("ValueA2"), registry.stream().toList());
  }

  @Test
  void shouldOverwriteValueOnDuplicateKeyWithDependencies() {
    DependencyRegistry.Owned<String, String> registry = DependencyRegistry.create();
    registry.register("A", "ValueA");
    registry.register("A", "ValueA2", Dependencies.<String>create().after("B"));
    registry.register("B", "ValueB");
    assertEquals(List.of("ValueB", "ValueA2"), registry.stream().toList());
  }

  @Test
  void shouldThrowExceptionOnCyclicDependency() {
    DependencyRegistry.Owned<String, String> registry = DependencyRegistry.create();
    registry.register("A", "ValueA", Dependencies.<String>create().after("B"));
    registry.register("B", "ValueB", Dependencies.<String>create().after("A"));

    assertThrows(CyclicDependencyException.class, () -> registry.stream().toList());
  }

  @Test
  void shouldHandleSelfDependencyAsCyclic() {
    DependencyRegistry.Owned<String, String> registry = DependencyRegistry.create();
    registry.register("A", "ValueA", Dependencies.<String>create().after("A"));

    assertThrows(CyclicDependencyException.class, () -> registry.stream().toList());
  }

  @Test
  void shouldIgnoreMissingDependency() {
    DependencyRegistry.Owned<String, String> registry = DependencyRegistry.create();
    registry.register("A", "ValueA", Dependencies.<String>create().after("Missing"));

    List<String> result = registry.stream().toList();
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
  void shouldSupportIterable() {
    DependencyRegistry.Owned<String, String> registry = DependencyRegistry.create();
    registry.register("B", "ValueB", Dependencies.<String>create().after("A"));
    registry.register("A", "ValueA");

    List<String> result = new java.util.ArrayList<>();
    for (String s : registry.iterable()) {
      result.add(s);
    }
    assertEquals(List.of("ValueA", "ValueB"), result);
  }

  @Test
  void shouldSupportSortEagerly() {
    DependencyRegistry.Owned<String, String> registry = DependencyRegistry.create();
    registry.register("A", "ValueA");
    registry.register("B", "ValueB", Dependencies.<String>create().after("A"));

    registry.sort();
    assertEquals(List.of("ValueA", "ValueB"), registry.stream().toList());
  }

  @Test
  void shouldThrowOnSortWithCyclicDependency() {
    DependencyRegistry.Owned<String, String> registry = DependencyRegistry.create();
    registry.register("A", "ValueA", Dependencies.<String>create().after("B"));
    registry.register("B", "ValueB", Dependencies.<String>create().after("A"));

    assertThrows(CyclicDependencyException.class, registry::sort);
  }

  @Test
  void shouldReturnEmptyStreamForEmptyRegistry() {
    DependencyRegistry.Owned<String, String> registry = DependencyRegistry.create();
    assertTrue(registry.stream().toList().isEmpty());
  }

  @Test
  void shouldReturnEmptyIterableForEmptyRegistry() {
    DependencyRegistry.Owned<String, String> registry = DependencyRegistry.create();
    assertFalse(registry.iterable().iterator().hasNext());
  }

  @Test
  void shouldHandleCombinedBeforeAndAfter() {
    DependencyRegistry.Owned<String, String> registry = DependencyRegistry.create();
    registry.register("A", "ValueA");
    registry.register("B", "ValueB");
    registry.register("C", "ValueC", Dependencies.<String>create().after("A").before("B"));

    List<String> result = registry.stream().toList();
    int indexA = result.indexOf("ValueA");
    int indexC = result.indexOf("ValueC");
    int indexB = result.indexOf("ValueB");
    assertTrue(indexA < indexC);
    assertTrue(indexC < indexB);
  }

  @Test
  void shouldThrowNpeOnNullKey() {
    DependencyRegistry.Owned<String, String> registry = DependencyRegistry.create();
    assertThrows(NullPointerException.class, () -> registry.register(null, "ValueA"));
  }

  @Test
  void shouldThrowNpeOnNullValue() {
    DependencyRegistry.Owned<String, String> registry = DependencyRegistry.create();
    assertThrows(NullPointerException.class, () -> registry.register("A", null));
  }
}
