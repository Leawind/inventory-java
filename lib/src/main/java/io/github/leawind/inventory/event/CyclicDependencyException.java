package io.github.leawind.inventory.event;

import java.util.Collection;

/** 检测到循环依赖。 */
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
