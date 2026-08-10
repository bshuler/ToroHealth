package net.torocraft.torohealth.config.loader;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.Watchable;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Exercises {@link FileWatcher#pollEvents(WatchService)} against hand-rolled
 * {@link WatchService}/{@link WatchKey}/{@link WatchEvent} fakes rather than a
 * real OS filesystem watch: real filesystem-event delivery is subject to
 * host-dependent polling latency (notably slow/coalesced on macOS), which
 * would make tests either slow or flaky. The real end-to-end {@code watch()}/
 * {@code run()}/{@code stop()}/{@code isRunning()} lifecycle is tested
 * separately below without waiting on any actual filesystem event.
 */
class FileWatcherTest {

  @TempDir
  File tempDir;

  private static final class FakeWatchEvent implements WatchEvent<Path> {
    private final Path context;

    FakeWatchEvent(Path context) {
      this.context = context;
    }

    @Override
    public Kind<Path> kind() {
      return StandardWatchEventKinds.ENTRY_MODIFY;
    }

    @Override
    public int count() {
      return 1;
    }

    @Override
    public Path context() {
      return context;
    }
  }

  private static final class FakeWatchKey implements WatchKey {
    private final List<WatchEvent<?>> events;
    private final boolean resetResult;

    FakeWatchKey(List<WatchEvent<?>> events, boolean resetResult) {
      this.events = events;
      this.resetResult = resetResult;
    }

    @Override
    public boolean isValid() {
      return true;
    }

    @Override
    public List<WatchEvent<?>> pollEvents() {
      return events;
    }

    @Override
    public boolean reset() {
      return resetResult;
    }

    @Override
    public void cancel() {
      // no-op fake
    }

    @Override
    public Watchable watchable() {
      return null;
    }
  }

  private static final class FakeWatchService implements WatchService {
    private final WatchKey keyToReturn;
    private final InterruptedException exceptionToThrow;

    FakeWatchService(WatchKey keyToReturn) {
      this.keyToReturn = keyToReturn;
      this.exceptionToThrow = null;
    }

    FakeWatchService(InterruptedException exceptionToThrow) {
      this.keyToReturn = null;
      this.exceptionToThrow = exceptionToThrow;
    }

    @Override
    public void close() {
      // no-op fake
    }

    @Override
    public WatchKey poll() {
      return keyToReturn;
    }

    @Override
    public WatchKey poll(long timeout, TimeUnit unit) {
      return keyToReturn;
    }

    @Override
    public WatchKey take() throws InterruptedException {
      if (exceptionToThrow != null) {
        throw exceptionToThrow;
      }
      return keyToReturn;
    }
  }

  @Test
  void pollEvents_invokesListenerWhenChangedFileMatchesWatchedFilename() throws Exception {
    File watched = new File(tempDir, "config.json");
    AtomicInteger callCount = new AtomicInteger();
    FileWatcher watcher = new FileWatcher(watched, callCount::incrementAndGet);

    FakeWatchEvent event = new FakeWatchEvent(Paths.get("config.json"));
    FakeWatchKey key = new FakeWatchKey(Collections.singletonList(event), true);
    FakeWatchService service = new FakeWatchService(key);

    boolean result = watcher.pollEvents(service);

    assertTrue(result);
    assertEquals(1, callCount.get());
  }

  @Test
  void pollEvents_ignoresEventsForOtherFiles() throws Exception {
    File watched = new File(tempDir, "config.json");
    AtomicInteger callCount = new AtomicInteger();
    FileWatcher watcher = new FileWatcher(watched, callCount::incrementAndGet);

    FakeWatchEvent event = new FakeWatchEvent(Paths.get("unrelated.json"));
    FakeWatchKey key = new FakeWatchKey(Collections.singletonList(event), true);
    FakeWatchService service = new FakeWatchService(key);

    watcher.pollEvents(service);

    assertEquals(0, callCount.get());
  }

  @Test
  void pollEvents_swallowsExceptionThrownByListenerAndStillResetsKey() throws Exception {
    File watched = new File(tempDir, "config.json");
    FileWatcher watcher = new FileWatcher(watched, () -> {
      throw new RuntimeException("listener boom");
    });

    FakeWatchEvent event = new FakeWatchEvent(Paths.get("config.json"));
    FakeWatchKey key = new FakeWatchKey(Collections.singletonList(event), true);
    FakeWatchService service = new FakeWatchService(key);

    boolean result = assertDoesNotThrow(() -> watcher.pollEvents(service));

    assertTrue(result);
  }

  @Test
  void pollEvents_returnsKeyResetValue() throws Exception {
    File watched = new File(tempDir, "config.json");
    FileWatcher watcher = new FileWatcher(watched, () -> { });

    FakeWatchKey key = new FakeWatchKey(Collections.emptyList(), false);
    FakeWatchService service = new FakeWatchService(key);

    assertFalse(watcher.pollEvents(service));
  }

  @Test
  void pollEvents_propagatesInterruptedExceptionFromTake() {
    File watched = new File(tempDir, "config.json");
    FileWatcher watcher = new FileWatcher(watched, () -> { });
    FakeWatchService service = new FakeWatchService(new InterruptedException("interrupted"));

    assertThrows(InterruptedException.class, () -> watcher.pollEvents(service));
  }

  @Test
  void isRunning_isFalseBeforeStart() {
    File watched = new File(tempDir, "config.json");
    FileWatcher watcher = new FileWatcher(watched, () -> { });

    assertFalse(watcher.isRunning());
  }

  @Test
  void stop_isANoOpWhenNeverStarted() {
    File watched = new File(tempDir, "config.json");
    FileWatcher watcher = new FileWatcher(watched, () -> { });

    assertDoesNotThrow(watcher::stop);
  }

  @Test
  void watchLifecycle_runsAfterStartAndStopsAfterStop() throws IOException, InterruptedException {
    File watched = new File(tempDir, "config.json");
    assertTrue(watched.createNewFile());

    FileWatcher watcher = FileWatcher.watch(watched, () -> { });
    try {
      assertTrue(waitUntil(watcher::isRunning, 2000), "watcher should be running shortly after start()");
      // isRunning() (Thread.isAlive()) goes true the instant start() is
      // invoked -- well before run() actually reaches its blocking
      // WatchService.take() call. Give the background thread a moment to
      // get there before interrupting it, so stop() exercises run()'s real
      // try-body-then-catch path (pollEvents() -> InterruptedException ->
      // catch) instead of racing an as-yet-unscheduled thread.
      Thread.sleep(500);
    } finally {
      watcher.stop();
    }

    assertTrue(waitUntil(() -> !watcher.isRunning(), 5000), "watcher should stop shortly after stop()");
  }

  @Test
  void watchLifecycle_deliversRealFilesystemEventThenStopsCleanly() throws Exception {
    // Unlike the fake-based pollEvents() tests above, this drives the real
    // run()/watch() loop end-to-end against a real OS filesystem-change
    // event, to cover run()'s while-loop actually iterating (pollEvents()
    // returning normally, not just via an interrupt) before it is stopped.
    File watched = new File(tempDir, "real-event.json");
    assertTrue(watched.createNewFile());

    AtomicInteger updateCount = new AtomicInteger();
    FileWatcher watcher = FileWatcher.watch(watched, updateCount::incrementAndGet);
    try {
      assertTrue(waitUntil(watcher::isRunning, 2000), "watcher should be running shortly after start()");
      // Give the registration a moment to settle before writing, so the
      // change is reliably observed rather than raced against setup.
      Thread.sleep(300);
      try (FileWriter writer = new FileWriter(watched)) {
        writer.write("changed");
      }

      assertTrue(waitUntil(() -> updateCount.get() > 0, 10000),
          "expected a real filesystem change to reach the listener");
    } finally {
      watcher.stop();
    }

    assertTrue(waitUntil(() -> !watcher.isRunning(), 5000), "watcher should stop shortly after stop()");
  }

  private static boolean waitUntil(BooleanSupplier condition, long timeoutMillis) {
    long deadline = System.currentTimeMillis() + timeoutMillis;
    while (System.currentTimeMillis() < deadline) {
      if (condition.getAsBoolean()) {
        return true;
      }
      try {
        Thread.sleep(10);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return false;
      }
    }
    return condition.getAsBoolean();
  }
}
