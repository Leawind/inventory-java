package io.github.leawind.inventory.lock;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.jspecify.annotations.NonNull;

/**
 * A read-write lock that works across processes via a shared lock file.
 *
 * <p><b>Behavior:</b>
 *
 * <ul>
 *   <li>Multiple readers (across processes) can hold the read lock concurrently.
 *   <li>The write lock is exclusive — blocks both readers and writers across all processes.
 *   <li>Reentrant: the same thread can acquire the same lock multiple times and must unlock equally
 *       many times.
 *   <li>A thread holding the read lock can acquire the read lock again but cannot upgrade to the
 *       write lock (throws {@link IllegalStateException}).
 *   <li>A thread holding the write lock can acquire both the read and write locks again (lock
 *       downgrade is supported).
 *   <li>{@code lock()} blocks until the lock is available.
 *   <li>{@code tryLock()} returns {@code false} if the lock cannot be acquired immediately.
 *   <li>{@code lockInterruptibly()} and {@code tryLock(long, TimeUnit)} throw {@link
 *       UnsupportedOperationException}.
 *   <li>{@code newCondition()} throws {@link UnsupportedOperationException}.
 * </ul>
 */
public class FileBasedReentrantReadWriteLock implements ReadWriteLock {

  private final ReentrantReadWriteLock localLock = new ReentrantReadWriteLock(true);

  private final FileChannel channel;

  /** Cross-process file lock */
  private volatile FileLock processLock;

  /** Number of threads holding the read lock within the current JVM */
  private final AtomicInteger processReadHolders = new AtomicInteger();

  /** Number of threads holding the write lock within the current JVM */
  private final AtomicInteger processWriteHolders = new AtomicInteger();

  /** Read lock reentry count for the current thread */
  private final ThreadLocal<Integer> readHoldCount = ThreadLocal.withInitial(() -> 0);

  /** Write lock reentry count for the current thread */
  private final ThreadLocal<Integer> writeHoldCount = ThreadLocal.withInitial(() -> 0);

  private final Lock readLock = new ReadLock();
  private final Lock writeLock = new WriteLock();

  public FileBasedReentrantReadWriteLock(Path lockFile) throws IOException {

    this.channel =
        FileChannel.open(
            lockFile, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE);
  }

  @Override
  public @NonNull Lock readLock() {
    return readLock;
  }

  @Override
  public @NonNull Lock writeLock() {
    return writeLock;
  }

  private class ReadLock implements Lock {

    @Override
    public void lock() {

      if (writeHoldCount.get() > 0) {
        localLock.readLock().lock();
        readHoldCount.set(readHoldCount.get() + 1);
        return;
      }

      localLock.readLock().lock();

      try {
        int hold = readHoldCount.get();

        if (hold > 0) {
          readHoldCount.set(hold + 1);
          return;
        }

        if (processReadHolders.incrementAndGet() == 1) {
          try {
            processLock = channel.lock(0L, Long.MAX_VALUE, true);
          } catch (IOException e) {
            processReadHolders.decrementAndGet();
            localLock.readLock().unlock();
            throw new RuntimeException(e);
          }
        }

        readHoldCount.set(1);

      } catch (RuntimeException e) {
        localLock.readLock().unlock();
        throw e;
      }
    }

    @Override
    public void unlock() {

      int hold = readHoldCount.get();

      if (hold <= 0) {
        throw new IllegalMonitorStateException();
      }

      if (hold > 1) {
        readHoldCount.set(hold - 1);
        localLock.readLock().unlock();
        return;
      }

      readHoldCount.remove();

      if (processReadHolders.decrementAndGet() == 0) {
        try {
          if (processLock != null) {
            processLock.release();
            processLock = null;
          }
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }

      localLock.readLock().unlock();
    }

    @Override
    public void lockInterruptibly() {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean tryLock() {

      if (writeHoldCount.get() > 0) {
        if (localLock.readLock().tryLock()) {
          readHoldCount.set(readHoldCount.get() + 1);
          return true;
        }
        return false;
      }

      if (!localLock.readLock().tryLock()) {
        return false;
      }

      try {
        int hold = readHoldCount.get();
        if (hold > 0) {
          readHoldCount.set(hold + 1);
          return true;
        }

        if (processReadHolders.incrementAndGet() == 1) {
          FileLock lock;
          try {
            lock = channel.tryLock(0L, Long.MAX_VALUE, true);
          } catch (IOException e) {
            processReadHolders.decrementAndGet();
            localLock.readLock().unlock();
            throw new RuntimeException(e);
          }

          if (lock == null) {
            processReadHolders.decrementAndGet();
            localLock.readLock().unlock();
            return false;
          }

          processLock = lock;
        }

        readHoldCount.set(1);

        return true;

      } catch (RuntimeException e) {
        localLock.readLock().unlock();
        throw e;
      }
    }

    @Override
    public boolean tryLock(long time, @NonNull TimeUnit unit) {
      throw new UnsupportedOperationException();
    }

    @Override
    public @NonNull Condition newCondition() {
      throw new UnsupportedOperationException();
    }
  }

  private class WriteLock implements Lock {

    @Override
    public void lock() {

      if (readHoldCount.get() > 0 && writeHoldCount.get() == 0) {
        throw new IllegalStateException("Read lock cannot be upgraded to write lock");
      }

      localLock.writeLock().lock();

      try {

        int hold = writeHoldCount.get();

        if (hold > 0) {
          writeHoldCount.set(hold + 1);
          return;
        }

        if (processWriteHolders.incrementAndGet() == 1) {
          try {
            processLock = channel.lock();
          } catch (IOException e) {
            processWriteHolders.decrementAndGet();
            localLock.writeLock().unlock();
            throw new RuntimeException(e);
          }
        }

        writeHoldCount.set(1);

      } catch (RuntimeException e) {
        localLock.writeLock().unlock();
        throw e;
      }
    }

    @Override
    public void unlock() {

      int hold = writeHoldCount.get();

      if (hold <= 0) {
        throw new IllegalMonitorStateException();
      }

      if (hold > 1) {
        writeHoldCount.set(hold - 1);
        localLock.writeLock().unlock();
        return;
      }

      writeHoldCount.remove();

      if (processWriteHolders.decrementAndGet() == 0) {

        try {
          if (processLock != null) {
            processLock.release();
            processLock = null;
          }
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }

      localLock.writeLock().unlock();
    }

    @Override
    public void lockInterruptibly() {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean tryLock() {

      if (readHoldCount.get() > 0 && writeHoldCount.get() == 0) {
        throw new IllegalStateException("Read lock cannot be upgraded to write lock");
      }

      if (!localLock.writeLock().tryLock()) {
        return false;
      }

      try {

        int hold = writeHoldCount.get();

        if (hold > 0) {
          writeHoldCount.set(hold + 1);
          return true;
        }

        if (processWriteHolders.incrementAndGet() == 1) {
          FileLock lock;

          try {
            lock = channel.tryLock();
          } catch (IOException e) {
            processWriteHolders.decrementAndGet();
            localLock.writeLock().unlock();
            throw new RuntimeException(e);
          }

          if (lock == null) {
            processWriteHolders.decrementAndGet();
            localLock.writeLock().unlock();
            return false;
          }

          processLock = lock;
        }

        writeHoldCount.set(1);

        return true;

      } catch (RuntimeException e) {
        localLock.writeLock().unlock();
        throw e;
      }
    }

    @Override
    public boolean tryLock(long time, @NonNull TimeUnit unit) {
      throw new UnsupportedOperationException();
    }

    @Override
    public @NonNull Condition newCondition() {
      throw new UnsupportedOperationException();
    }
  }
}
