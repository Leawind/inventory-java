package io.github.leawind.inventory.dep;

import io.github.leawind.inventory.event.DependencyException;
import java.util.Collection;

public final class CyclicDependencyException extends DependencyException {

  private final Collection<?> cycleNodes;

  public CyclicDependencyException(Collection<?> cycleNodes) {
    super("Cyclic dependency detected among: " + cycleNodes);
    this.cycleNodes = cycleNodes;
  }

  public Collection<?> getCycleNodes() {
    return cycleNodes;
  }
}
