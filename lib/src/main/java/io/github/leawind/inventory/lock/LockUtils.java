package io.github.leawind.inventory.lock;

import io.github.leawind.inventory.misc.UncheckedCloseable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

public class LockUtils {

  public static UncheckedCloseable lock(Lock lock) {
    lock.lock();
    return lock::unlock;
  }

  public static UncheckedCloseable readLock(ReadWriteLock lock) {
    return lock(lock.readLock());
  }

  public static UncheckedCloseable writeLock(ReadWriteLock lock) {
    return lock(lock.writeLock());
  }
}
