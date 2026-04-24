package io.github.leawind.inventory.misc;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class TempDirectory implements AutoCloseable {

  private final Path tempDir;

  public TempDirectory() throws IOException {
    this.tempDir = Files.createTempDirectory(null);
  }

  public TempDirectory(@Nullable String prefix) throws IOException {
    this.tempDir = Files.createTempDirectory(prefix);
  }

  public TempDirectory(String prefix, FileAttribute<?>... attrs) throws IOException {
    this.tempDir = Files.createTempDirectory(prefix, attrs);
  }

  public Path getPath() {
    return tempDir;
  }

  @Override
  public void close() throws IOException {
    deleteDirectoryRecursively(tempDir);
  }

  private static void deleteDirectoryRecursively(Path dir) throws IOException {
    if (!Files.exists(dir)) {
      return;
    }

    Files.walkFileTree(
        dir,
        new SimpleFileVisitor<>() {
          @Override
          public @NonNull FileVisitResult visitFile(
              @NonNull Path file, @NonNull BasicFileAttributes attrs) throws IOException {
            Files.delete(file);
            return FileVisitResult.CONTINUE;
          }

          @Override
          public @NonNull FileVisitResult postVisitDirectory(@NonNull Path dir, IOException exc)
              throws IOException {
            if (exc != null) {
              throw exc;
            }
            Files.delete(dir);
            return FileVisitResult.CONTINUE;
          }
        });
  }
}
