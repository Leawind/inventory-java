package io.github.leawind.inventory.type;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

@SuppressWarnings("unused")
public class UnsafeTypeUtilsTest {

  @Test
  void testReturnSelf() {
    class MyClass<T, Self extends MyClass<T, Self>> {
      public Self self() {
        // return this;
        //        ^^^^
        // Required type: Self
        // Provided:      MyClass <T, Self>

        return UnsafeTypeUtils.cast(this);
      }
    }
  }

  @Test
  void testUnnamed() {
    class Meta {
      List<?> getNames() {
        return List.of();
      }
    }

    // List<String> list = new Meta().getList();
    //                                ^^^^^^^
    // Required type: List<String>
    // Provided:      List<capture of ?>

    List<String> list = UnsafeTypeUtils.cast(new Meta().getNames());
  }

  @Test
  void test1() {
    Set<List<?>> set =
        Set.of(new ArrayList<>(List.of(1, 2, 3)), new ArrayList<>(List.of('a', 'b')));

    for (List<?> list : set) {
      // list.add(list.get(0));
      //         ^^^^^^^^^^^^^
      // Required type: capture of ?
      // Provided:      capture of ?

      list.add(UnsafeTypeUtils.cast(list.get(0)));
    }
  }

  @Test
  void genericList() {
    // List<Object> a = new ArrayList<Integer>();
    // Required type: List<Object>
    // Provided: ArrayList<Integer>
    List<Object> a = UnsafeTypeUtils.cast(new ArrayList<Integer>());
  }
}
