package io.github.leawind.inventory.misc;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class TempDirectoryTest {

  @Test
  void testSuccessfulCreation() throws IOException {
    try (TempDirectory tempDir = new TempDirectory()) {
      Path path = tempDir.getPath();
      assertNotNull(path);
      assertTrue(Files.exists(path));
      assertTrue(Files.isDirectory(path));
    }
  }

  @Test
  void testDirectoryAutomaticallyDeletedAfterTryWithResources() throws IOException {
    Path path;
    try (TempDirectory tempDir = new TempDirectory()) {
      path = tempDir.getPath();
      assertTrue(Files.exists(path));
      // Create a file inside to verify we can use the directory
      Path testFile = path.resolve("test.txt");
      Files.writeString(testFile, "test content");
      assertTrue(Files.exists(testFile));
    }
    // After try-with-resources, directory and its contents should be deleted
    assertFalse(Files.exists(path));
  }

  @Test
  void testDirectoryDeletedEvenIfExceptionOccurs() {
    Path path = null;
    try (TempDirectory tempDir = new TempDirectory()) {
      path = tempDir.getPath();
      assertTrue(Files.exists(path));
      // Throw an exception to test cleanup
      throw new IOException("Test exception");
    } catch (IOException e) {
      // Expected exception
      assertEquals("Test exception", e.getMessage());
    }
    // Directory should still be deleted despite the exception
    assertFalse(path != null && Files.exists(path));
  }

  @Test
  void testCanCreateAndUseFilesInside() throws IOException {
    try (TempDirectory tempDir = new TempDirectory()) {
      Path dirPath = tempDir.getPath();
      Path testFile = dirPath.resolve("test.txt");

      // Write to file
      String testContent = "Hello, World!";
      Files.writeString(testFile, testContent);

      // Read from file
      String readContent = Files.readString(testFile);
      assertEquals(testContent, readContent);

      // Verify file exists in directory
      assertTrue(Files.exists(testFile));
    }
  }

  @Test
  void testCustomPrefix() throws IOException {
    try (TempDirectory tempDir = new TempDirectory("myPrefix")) {
      Path path = tempDir.getPath();
      String fileName = path.getFileName().toString();
      assertTrue(fileName.startsWith("myPrefix"));
    }
  }
}
