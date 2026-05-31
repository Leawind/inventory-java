package io.github.leawind.inventory.lock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.leawind.inventory.misc.UncheckedCloseable;
import io.github.leawind.inventory.testutils.JavaProcess;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class FileBasedReentrantReadWriteLockTest {

  @TempDir File tempDir;

  private File lockFile;
  private FileBasedReentrantReadWriteLock rwLock;

  @BeforeEach
  void setUp() throws IOException {
    Path lockFilePath = tempDir.toPath().resolve("test.lock");
    lockFile = lockFilePath.toFile();
    rwLock = new FileBasedReentrantReadWriteLock(lockFilePath);
  }

  @Test
  void testReadLockReentrant() {
    rwLock.readLock().lock();
    rwLock.readLock().lock();
    rwLock.readLock().unlock();
    rwLock.readLock().unlock();
  }

  @Test
  void testWriteLockReentrant() {
    rwLock.writeLock().lock();
    rwLock.writeLock().lock();
    rwLock.writeLock().unlock();
    rwLock.writeLock().unlock();
  }

  @Test
  void testUnlockWithoutLockThrowsException() {
    assertThrows(IllegalMonitorStateException.class, () -> rwLock.readLock().unlock());
    assertThrows(IllegalMonitorStateException.class, () -> rwLock.writeLock().unlock());
  }

  @Test
  void testWriteLockDowngradeToReadLock() {
    {
      rwLock.writeLock().lock();
      rwLock.readLock().lock();
      rwLock.writeLock().unlock();
      rwLock.readLock().unlock();
    }

    {
      rwLock.writeLock().lock();
      rwLock.writeLock().lock();
      rwLock.readLock().lock();
      rwLock.writeLock().unlock();
      rwLock.writeLock().unlock();
      rwLock.readLock().unlock();
    }
  }

  @Test
  void testWriteLockDowngradeThenAnotherThreadCanRead() throws InterruptedException {
    rwLock.writeLock().lock();
    rwLock.readLock().lock();
    rwLock.writeLock().unlock();

    AtomicBoolean otherReadAcquired = new AtomicBoolean(false);
    CountDownLatch latch = new CountDownLatch(1);

    new Thread(
            () -> {
              rwLock.readLock().lock();
              otherReadAcquired.set(true);
              rwLock.readLock().unlock();
              latch.countDown();
            })
        .start();

    assertTrue(latch.await(2, TimeUnit.SECONDS));
    assertTrue(otherReadAcquired.get());

    rwLock.readLock().unlock();
  }

  @Test
  void testReadLockCannotUpgradeToWriteLock() {
    rwLock.readLock().lock();
    assertThrows(IllegalStateException.class, () -> rwLock.writeLock().lock());
    assertThrows(IllegalStateException.class, () -> rwLock.writeLock().tryLock());
    rwLock.readLock().unlock();
  }

  @Test
  void testMultipleThreadsCanHoldReadLockSimultaneously() throws InterruptedException {
    int threadCount = 5;
    CountDownLatch allAcquired = new CountDownLatch(threadCount);
    CountDownLatch canRelease = new CountDownLatch(1);
    AtomicInteger concurrentReaders = new AtomicInteger(0);
    AtomicInteger maxConcurrentReaders = new AtomicInteger(0);

    Thread[] threads = new Thread[threadCount];
    for (int i = 0; i < threadCount; i++) {
      threads[i] =
          new Thread(
              () -> {
                rwLock.readLock().lock();
                int current = concurrentReaders.incrementAndGet();
                maxConcurrentReaders.accumulateAndGet(current, Math::max);
                allAcquired.countDown();
                try {
                  canRelease.await();
                } catch (InterruptedException ignored) {
                } finally {
                  concurrentReaders.decrementAndGet();
                  rwLock.readLock().unlock();
                }
              });
      threads[i].start();
    }

    assertTrue(allAcquired.await(2, TimeUnit.SECONDS));
    assertEquals(threadCount, maxConcurrentReaders.get());

    canRelease.countDown();
    for (Thread t : threads) {
      t.join();
    }

    assertEquals(0, concurrentReaders.get());
  }

  @Test
  void testWriteLockIsExclusiveAcrossThreads() throws InterruptedException {
    int threadCount = 5;
    AtomicInteger concurrentWriters = new AtomicInteger(0);
    AtomicInteger maxConcurrentWriters = new AtomicInteger(0);
    CountDownLatch done = new CountDownLatch(threadCount);

    for (int i = 0; i < threadCount; i++) {
      new Thread(
              () -> {
                rwLock.writeLock().lock();
                int current = concurrentWriters.incrementAndGet();
                maxConcurrentWriters.accumulateAndGet(current, Math::max);
                try {
                  Thread.sleep(50);
                } catch (InterruptedException ignored) {
                } finally {
                  concurrentWriters.decrementAndGet();
                  rwLock.writeLock().unlock();
                  done.countDown();
                }
              })
          .start();
    }

    assertTrue(done.await(10, TimeUnit.SECONDS));
    assertEquals(
        1, maxConcurrentWriters.get(), "Only one thread should hold the write lock at a time");
  }

  @Test
  void testWriteLockBlocksReadLock() throws InterruptedException {
    rwLock.writeLock().lock();

    AtomicBoolean readAcquired = new AtomicBoolean(false);
    CountDownLatch tried = new CountDownLatch(1);

    new Thread(
            () -> {
              readAcquired.set(rwLock.readLock().tryLock());
              tried.countDown();
            })
        .start();

    tried.await();
    assertFalse(readAcquired.get(), "Read lock should not be acquired while write lock is held");

    rwLock.writeLock().unlock();
  }

  @Test
  void testReadLockBlocksWriteLock() throws InterruptedException {
    rwLock.readLock().lock();

    AtomicBoolean writeAcquired = new AtomicBoolean(false);
    CountDownLatch tried = new CountDownLatch(1);

    new Thread(
            () -> {
              writeAcquired.set(rwLock.writeLock().tryLock());
              tried.countDown();
            })
        .start();

    tried.await();
    assertFalse(writeAcquired.get(), "Write lock should not be acquired while read lock is held");

    rwLock.readLock().unlock();
  }

  @Test
  void testReadLockReleasedThenWriteLockAcquired() throws InterruptedException {
    rwLock.readLock().lock();

    AtomicBoolean writeAcquired = new AtomicBoolean(false);
    CountDownLatch done = new CountDownLatch(1);

    new Thread(
            () -> {
              rwLock.writeLock().lock();
              writeAcquired.set(true);
              rwLock.writeLock().unlock();
              done.countDown();
            })
        .start();

    Thread.sleep(50);
    assertFalse(writeAcquired.get());
    rwLock.readLock().unlock();

    assertTrue(done.await(5, TimeUnit.SECONDS));
    assertTrue(writeAcquired.get());
  }

  @Test
  void testWriteLockReleasedThenReadLockAcquired() throws InterruptedException {
    rwLock.writeLock().lock();

    AtomicBoolean readAcquired = new AtomicBoolean(false);
    CountDownLatch done = new CountDownLatch(1);

    Thread t =
        new Thread(
            () -> {
              rwLock.readLock().lock();
              readAcquired.set(true);
              rwLock.readLock().unlock();
              done.countDown();
            });
    t.start();

    Thread.sleep(50);
    assertFalse(readAcquired.get());
    rwLock.writeLock().unlock();

    assertTrue(done.await(5, TimeUnit.SECONDS));
    assertTrue(readAcquired.get());
  }

  @Test
  void testWriteDowngradeThenAnotherThreadWrites() throws InterruptedException {
    rwLock.writeLock().lock();
    rwLock.readLock().lock();
    rwLock.writeLock().unlock();

    AtomicBoolean writeAcquired = new AtomicBoolean(false);
    CountDownLatch tried = new CountDownLatch(1);
    new Thread(
            () -> {
              writeAcquired.set(rwLock.writeLock().tryLock());
              if (writeAcquired.get()) {
                rwLock.writeLock().unlock();
              }
              tried.countDown();
            })
        .start();

    tried.await();
    assertFalse(
        writeAcquired.get(), "Another thread should not get write lock while read lock is held");

    rwLock.readLock().unlock();
  }

  @Test
  void testMultipleReadersThenOneWriter() throws InterruptedException {
    int readerCount = 3;
    CountDownLatch allReading = new CountDownLatch(readerCount);
    CountDownLatch canStopReading = new CountDownLatch(1);
    AtomicInteger concurrentReaders = new AtomicInteger(0);
    AtomicInteger maxConcurrentReaders = new AtomicInteger(0);

    Thread[] readers = new Thread[readerCount];
    for (int i = 0; i < readerCount; i++) {
      readers[i] =
          new Thread(
              () -> {
                rwLock.readLock().lock();
                int current = concurrentReaders.incrementAndGet();
                maxConcurrentReaders.accumulateAndGet(current, Math::max);
                allReading.countDown();
                try {
                  canStopReading.await();
                } catch (InterruptedException ignored) {
                } finally {
                  concurrentReaders.decrementAndGet();
                  rwLock.readLock().unlock();
                }
              });
      readers[i].start();
    }

    assertTrue(allReading.await(5, TimeUnit.SECONDS));
    assertEquals(readerCount, maxConcurrentReaders.get());

    AtomicBoolean writeAcquired = new AtomicBoolean(false);
    CountDownLatch writeTried = new CountDownLatch(1);
    Thread writer =
        new Thread(
            () -> {
              writeAcquired.set(rwLock.writeLock().tryLock());
              if (writeAcquired.get()) {
                rwLock.writeLock().unlock();
              }
              writeTried.countDown();
            });
    writer.start();

    writeTried.await();
    assertFalse(writeAcquired.get());

    canStopReading.countDown();
    for (Thread r : readers) {
      r.join();
    }

    assertTrue(rwLock.writeLock().tryLock());
    rwLock.writeLock().unlock();
    writer.join();
  }

  // ===========================================================================
  // Multi-process tests
  // ===========================================================================
  public enum LockType {
    READ,
    WRITE
  }

  public enum Mode {
    LOCK,
    TRY_LOCK
  }

  /**
   * Subprocess main class for cross-process lock testing.
   *
   * <p>Output protocol:
   *
   * <ul>
   *   <li>ACQUIRED - lock was obtained
   *   <li>FAILED - tryLock returned false
   *   <li>RELEASED - lock was released after holding
   * </ul>
   */
  public static class LockProcessMain {

    public static void main(String[] args) {
      try {

        String lockFilePath = args[0];
        LockType lockType = LockType.valueOf(args[1]);
        Mode mode = Mode.valueOf(args[2]);

        long holdTimeMs = Long.parseLong(args[3]);
        long delayMs = Long.parseLong(args[4]);

        if (delayMs > 0) {
          Thread.sleep(delayMs);
        }

        Path path = Path.of(lockFilePath);
        FileBasedReentrantReadWriteLock lock = new FileBasedReentrantReadWriteLock(path);

        boolean acquired =
            switch (lockType) {
              case READ ->
                  switch (mode) {
                    case TRY_LOCK -> lock.readLock().tryLock();
                    case LOCK -> {
                      lock.readLock().lock();
                      yield true;
                    }
                  };
              case WRITE ->
                  switch (mode) {
                    case TRY_LOCK -> lock.writeLock().tryLock();
                    case LOCK -> {
                      lock.writeLock().lock();
                      yield true;
                    }
                  };
            };

        if (acquired) {
          System.out.println("ACQUIRED");
          System.out.flush();
          Thread.sleep(holdTimeMs);

          switch (lockType) {
            case READ -> lock.readLock().unlock();
            case WRITE -> lock.writeLock().unlock();
          }
          System.out.println("RELEASED");
        } else {
          System.out.println("FAILED");
        }
        System.out.flush();

      } catch (Throwable e) {
        System.err.println(e.getMessage());
        System.err.flush();
        System.exit(1);
      }
    }
  }

  private JavaProcess startLockProcess(LockType lockType, Mode mode, long holdTimeMs, long delayMs)
      throws IOException {
    JavaProcess proc =
        new JavaProcess(
            LockProcessMain.class,
            lockFile.getAbsolutePath(),
            lockType.name(),
            mode.name(),
            String.valueOf(holdTimeMs),
            String.valueOf(delayMs));
    proc.start();
    return proc;
  }

  @Test
  void testMultipleProcessesCanHoldReadLockSimultaneously() throws Exception {
    JavaProcess proc1 = startLockProcess(LockType.READ, Mode.LOCK, 1000, 0);
    JavaProcess proc2 = startLockProcess(LockType.READ, Mode.LOCK, 1000, 0);

    assertEquals(0, proc1.waitForExit());
    assertEquals(0, proc2.waitForExit());

    assertTrue(proc1.getOutput().contains("ACQUIRED"));
    assertTrue(proc2.getOutput().contains("ACQUIRED"));
  }

  @Test
  void testWriteLockIsExclusiveAcrossProcesses() throws Exception {
    JavaProcess procA = startLockProcess(LockType.WRITE, Mode.LOCK, 1000, 0);
    JavaProcess procB = startLockProcess(LockType.WRITE, Mode.TRY_LOCK, 500, 500);

    assertEquals(0, procA.waitForExit());
    assertEquals(0, procB.waitForExit());

    assertTrue(procA.getOutput().contains("ACQUIRED"));
    assertTrue(
        procB.getOutput().contains("FAILED"), "Second process should fail to acquire write lock");
  }

  @Test
  void testWriteLockBlocksReadLockAcrossProcesses() throws Exception {
    JavaProcess procA = startLockProcess(LockType.WRITE, Mode.LOCK, 1000, 0);
    JavaProcess procB = startLockProcess(LockType.READ, Mode.TRY_LOCK, 500, 500);

    assertEquals(0, procA.waitForExit());
    assertEquals(0, procB.waitForExit());

    assertTrue(procA.getOutput().contains("ACQUIRED"));
    assertTrue(
        procB.getOutput().contains("FAILED"),
        "Read lock should not be acquired while another process holds write lock");
  }

  @Test
  void testReadLockBlocksWriteLockAcrossProcesses() throws Exception {
    JavaProcess procA = startLockProcess(LockType.READ, Mode.LOCK, 1000, 0);
    JavaProcess procB = startLockProcess(LockType.WRITE, Mode.TRY_LOCK, 500, 500);

    assertEquals(0, procA.waitForExit());
    assertEquals(0, procB.waitForExit());

    assertTrue(procA.getOutput().contains("ACQUIRED"));
    assertTrue(
        procB.getOutput().contains("FAILED"),
        "Write lock should not be acquired while another process holds read lock");
  }

  @Test
  void testWriteLockReleasedThenOtherProcessCanAcquire() throws Exception {
    JavaProcess procA = startLockProcess(LockType.WRITE, Mode.LOCK, 0, 0);
    assertEquals(0, procA.waitForExit());
    assertTrue(procA.getOutput().contains("ACQUIRED"));
    assertTrue(procA.getOutput().contains("RELEASED"));

    JavaProcess procB = startLockProcess(LockType.WRITE, Mode.TRY_LOCK, 0, 0);
    assertEquals(0, procB.waitForExit());
    assertTrue(
        procB.getOutput().contains("ACQUIRED"),
        "Write lock should be acquired after previous holder releases it");
  }

  @Test
  void testReadLockReleasedThenOtherProcessCanWrite() throws Exception {
    JavaProcess procA = startLockProcess(LockType.READ, Mode.LOCK, 0, 0);
    assertEquals(0, procA.waitForExit());
    assertTrue(procA.getOutput().contains("ACQUIRED"));
    assertTrue(procA.getOutput().contains("RELEASED"));

    JavaProcess procB = startLockProcess(LockType.WRITE, Mode.TRY_LOCK, 0, 0);
    assertEquals(0, procB.waitForExit());
    assertTrue(
        procB.getOutput().contains("ACQUIRED"),
        "Write lock should be acquired after reader releases it");
  }

  @Test
  void testMultipleProcessesReadThenOneWriteFails() throws Exception {
    JavaProcess reader1 = startLockProcess(LockType.READ, Mode.LOCK, 1500, 0);
    JavaProcess reader2 = startLockProcess(LockType.READ, Mode.LOCK, 1500, 0);
    JavaProcess writer = startLockProcess(LockType.WRITE, Mode.TRY_LOCK, 500, 500);

    assertEquals(0, reader1.waitForExit());
    assertEquals(0, reader2.waitForExit());
    assertEquals(0, writer.waitForExit());

    assertTrue(reader1.getOutput().contains("ACQUIRED"));
    assertTrue(reader2.getOutput().contains("ACQUIRED"));
    assertTrue(
        writer.getOutput().contains("FAILED"),
        "Write lock should not be acquired while readers hold read locks");
  }

  @Nested
  class FileContentTest {
    String content;
    Path filePath;

    @BeforeEach
    void setup() {
      content = "hello world";
      filePath = lockFile.toPath();
    }

    @Test
    void canReadWhileReadLockHeld() throws IOException {
      Files.writeString(filePath, content);
      try (UncheckedCloseable ignored = LockUtils.readLock(rwLock)) {
        assertEquals(content, Files.readString(filePath));
      }
    }

    @Test
    void canReadWhileWriteLockHeld() throws IOException {
      Files.writeString(filePath, content);
      try (UncheckedCloseable ignored = LockUtils.writeLock(rwLock)) {
        assertEquals(content, Files.readString(filePath));
      }
    }

    @Test
    void canWriteWhileWriteLockHeld() throws IOException {
      try (UncheckedCloseable ignored = LockUtils.writeLock(rwLock)) {
        Files.writeString(filePath, content);
        assertEquals(content, Files.readString(filePath));
      }
    }

    @Test
    void canWriteWhileReadLockHeld() throws IOException {
      try (UncheckedCloseable ignored = LockUtils.readLock(rwLock)) {
        Files.writeString(filePath, content);
        assertEquals(content, Files.readString(filePath));
      }
    }

    @Test
    void readLockDoNotAffectContent() throws IOException {
      Files.writeString(filePath, content);
      try (UncheckedCloseable ignored = LockUtils.readLock(rwLock)) {
        assertEquals(content, Files.readString(filePath));
      }
    }

    @Test
    void writeLockDoNotAffectContent() throws IOException {
      Files.writeString(filePath, content);
      try (UncheckedCloseable ignored = LockUtils.writeLock(rwLock)) {
        assertEquals(content, Files.readString(filePath));
      }
    }
  }
}
