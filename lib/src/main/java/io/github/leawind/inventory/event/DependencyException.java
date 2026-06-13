package io.github.leawind.inventory.event;

/** Base class for dependency sorting related exceptions. */
public class DependencyException extends RuntimeException {

  public DependencyException(String message) {
    super(message);
  }
}
