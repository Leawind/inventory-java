package io.github.leawind.inventory.testutils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class JavaProcess {
  private final ProcessBuilder builder;
  private Process process = null;
  private String output = null;
  private String error = null;

  public JavaProcess(Class<?> mainClass, String... args) {
    this.builder = getBuilder(mainClass, args);
  }

  public void start() throws IOException {
    process = builder.start();
  }

  public int waitForExit() throws InterruptedException {
    if (process == null) {
      throw new IllegalStateException("process not started");
    }

    int exitCode = process.waitFor();
    output = readStream(process.getInputStream());
    error = readStream(process.getErrorStream());
    return exitCode;
  }

  public String getOutput() {
    if (output == null) {
      throw new IllegalStateException("process not started or not waited for exit");
    }
    return output;
  }

  public String getError() {
    if (error == null) {
      throw new IllegalStateException("process not started or not waited for exit");
    }
    return error;
  }

  private static String readStream(InputStream is) {
    return new BufferedReader(new InputStreamReader(is))
        .lines()
        .collect(
            Collectors.collectingAndThen(
                Collectors.toList(), list -> list.isEmpty() ? "" : String.join("\n", list) + "\n"));
  }

  public static ProcessBuilder getBuilder(Class<?> mainClass, String... args) {
    String javaHome = System.getProperty("java.home");
    String classpath = System.getProperty("java.class.path");

    List<String> command = new ArrayList<>();
    String javaExec =
        javaHome + File.separator + "bin" + File.separator + (isWindows() ? "java.exe" : "java");
    command.add(javaExec);
    command.add("-cp");
    command.add(classpath);
    command.add(mainClass.getName());
    command.addAll(Arrays.asList(args));

    return new ProcessBuilder(command);
  }

  private static boolean isWindows() {
    return System.getProperty("os.name").toLowerCase().contains("win");
  }
}
