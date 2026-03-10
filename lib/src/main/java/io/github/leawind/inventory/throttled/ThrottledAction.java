package io.github.leawind.inventory.throttled;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class ThrottledAction<T> {
  private final @NonNull Supplier<T> actionFn;
  private final long interval;

  private long lastExecuteTimeMillis;
  private @Nullable ScheduledFuture<?> scheduledFuture;
  private @Nullable CompletableFuture<T> currentFuture;
  private final @NonNull ScheduledExecutorService scheduler;

  public ThrottledAction(@NonNull Supplier<T> actionFn, long interval) {
    this(actionFn, interval, Executors.newSingleThreadScheduledExecutor());
  }

  /**
   * Create a new ThrottledAction
   *
   * @param actionFn The action to execute
   * @param interval Mimimum time between executions
   * @param scheduler The scheduler to use for executing the action
   */
  public ThrottledAction(
      @NonNull Supplier<T> actionFn, long interval, @NonNull ScheduledExecutorService scheduler) {
    this.actionFn = actionFn;
    this.interval = interval;
    this.scheduler = scheduler;
    this.lastExecuteTimeMillis = 0;
  }

  /// Execute the action immediately, without waiting for the interval to pass.
  public synchronized T executeImmediately() {
    lastExecuteTimeMillis = System.currentTimeMillis();
    return actionFn.get();
  }

  /// Check if the action is scheduled.
  public boolean isScheduled() {
    return scheduledFuture != null;
  }

  /// Schedule the action to execute after the interval has passed
  public synchronized @NonNull CompletableFuture<T> urge() {
    long now = System.currentTimeMillis();
    long elapsed = now - lastExecuteTimeMillis;

    if (isScheduled()) {
      // The action is already scheduled, return the existing future
      return Objects.requireNonNull(currentFuture);
    }

    if (elapsed >= interval) {
      // Already passed the interval, execute immediately
      return CompletableFuture.completedFuture(executeImmediately());
    } else {
      // Not yet passed the interval, schedule v1 new one
      long delay = interval - elapsed;

      currentFuture = new CompletableFuture<>();
      scheduledFuture =
          scheduler.schedule(
              () -> {
                synchronized (this) {
                  currentFuture.complete(executeImmediately());

                  currentFuture = null;
                  scheduledFuture = null;
                }
              },
              delay,
              TimeUnit.MILLISECONDS);

      return Objects.requireNonNull(currentFuture);
    }
  }

  public synchronized long sinceLastExecute() {
    return sinceLastExecute(System.currentTimeMillis());
  }

  public synchronized long sinceLastExecute(long now) {
    return now - lastExecuteTimeMillis;
  }

  public void shutdown() {
    scheduler.shutdown();
  }
}
