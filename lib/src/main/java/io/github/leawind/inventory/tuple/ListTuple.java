package io.github.leawind.inventory.tuple;

import java.util.ArrayList;
import java.util.List;

public class ListTuple {
  private final List<Object> values = new ArrayList<>();
  private final List<Class<?>> types = new ArrayList<>();

  public static ListTuple of(Object... values) {
    ListTuple tuple = new ListTuple();
    for (Object value : values) {
      tuple.values.add(value);
      tuple.types.add(value.getClass());
    }
    return tuple;
  }

  public int length() {
    return values.size();
  }

  @SuppressWarnings("unchecked")
  public <T> T get(int index) {
    validateIndex(index);
    return (T) values.get(index);
  }

  public <T> T get(int index, Class<T> type) {
    validateIndex(index);

    Class<?> actualType = types.get(index);

    if (actualType != Void.class && !type.isAssignableFrom(actualType)) {
      throw new ClassCastException("Type mismatch at index " + index);
    }
    return type.cast(values.get(index));
  }

  private boolean isIndexValid(int index) {
    return index >= 0 && index < values.size();
  }

  private void validateIndex(int index) {
    if (!isIndexValid(index)) {
      throw new IndexOutOfBoundsException("Index: " + index + ", Length: " + values.size());
    }
  }
}
