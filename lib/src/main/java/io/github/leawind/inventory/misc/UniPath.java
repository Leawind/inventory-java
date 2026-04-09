package io.github.leawind.inventory.misc;

import java.io.File;
import java.nio.file.Path;

public final class UniPath {
  private static final Path DEFAULT = Path.of("");

  private String windowsPath;
  private String unixPath;

  public UniPath() {
    this.windowsPath = null;
    this.unixPath = null;
  }

  public Path get() {
    String pathStr = isWindows() ? windowsPath : unixPath;
    if (pathStr == null) {
      return DEFAULT;
    }
    return Path.of(pathStr);
  }

  public UniPath set(Path path) {
    String pathStr = path.toString();

    if (isWindows()) {
      this.windowsPath = pathStr;
    } else {
      this.unixPath = pathStr;
    }
    return this;
  }

  public UniPath setWindows(String path) {
    this.windowsPath = path;
    return this;
  }

  public UniPath setUnix(String path) {
    this.unixPath = path;
    return this;
  }

  private static boolean isWindows() {
    return File.separatorChar == '\\';
  }
}
