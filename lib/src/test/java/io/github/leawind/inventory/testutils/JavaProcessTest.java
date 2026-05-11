package io.github.leawind.inventory.testutils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class JavaProcessTest {
  public static class TestMain {
    public static void main(String[] args) {
      for (String arg : args) {
        System.out.println(arg);
      }
    }
  }

  @Test
  void test() throws Exception {
    JavaProcess process = new JavaProcess(TestMain.class, "Alice", "Bob", "Charlie");
    process.start();

    int exitCode = process.waitForExit();
    assertEquals(0, exitCode);

    String output = process.getOutput();
    String error = process.getError();

    assertEquals("Alice\nBob\nCharlie\n", output);
    assertEquals("", error);
  }
}
