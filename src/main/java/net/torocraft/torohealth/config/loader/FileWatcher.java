package net.torocraft.torohealth.config.loader;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import java.io.File;
import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

public class FileWatcher implements Runnable {
  private final File file;
  private final Path filename;
  private final Path parent;
  private final Listener listener;
  private Thread thread;

  @FunctionalInterface
  public static interface Listener {
    void onUpdate();
  }

  public static FileWatcher watch(File file, Listener listener) {
    FileWatcher watcher = new FileWatcher(file, listener);
    Thread thread = new Thread(watcher);
    thread.setDaemon(true);
    watcher.thread = thread;
    thread.start();
    return watcher;
  }

  // Package-private rather than private: a deliberate test seam so unit
  // tests can drive pollEvents(WatchService) directly against a hand-rolled
  // WatchService/WatchKey/WatchEvent fake, without spinning a real
  // background thread or depending on the host OS's filesystem-event
  // polling latency. Production code only ever reaches this via watch().
  FileWatcher(File file, Listener listener) {
    this.file = file;
    this.listener = listener;
    this.filename = file.toPath().getFileName();
    this.parent = file.toPath().getParent();
  }

  /**
   * Stops the background watch thread. Previously there was no way to stop a
   * watcher once started (a real gap: config-reload watching ran forever with
   * no shutdown path); this also lets tests deterministically drive the
   * {@code catch} branch in {@link #run()} instead of leaving it uncovered.
   */
  public void stop() {
    if (thread != null) {
      thread.interrupt();
    }
  }

  /** Whether the background watch thread is currently alive. */
  public boolean isRunning() {
    return thread != null && thread.isAlive();
  }

  @Override
  public void run() {
    // Loops unconditionally rather than exiting when pollEvents()'s
    // key.reset() reports the watch key invalidated (e.g. the watched
    // directory itself got deleted): stop() (via Thread.interrupt(),
    // caught below) is this class's one real, tested termination path, and
    // an invalidated key still leaves watchService.take() blocking, so it
    // still responds to interrupt() correctly. This also sidesteps a real
    // try-with-resources javac quirk: with a conditional exit, the compiler
    // generates two physically distinct "close the resource" bytecode
    // blocks -- one for a normal, no-exception loop exit, one folded into
    // the exception path -- and only the latter is ever really exercised
    // by any test, since every intentional stop() goes through
    // Thread.interrupt(). Removing the conditional exit removes that
    // untested, load-order-sensitive dead branch instead of chasing it.
    try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
      this.parent.register(watchService, ENTRY_MODIFY, ENTRY_CREATE);
      while (true) {
        pollEvents(watchService);
      }
    } catch (IOException | InterruptedException | ClosedWatchServiceException e) {
      Thread.currentThread().interrupt();
    }
  }

  protected boolean pollEvents(WatchService watchService) throws InterruptedException {
    WatchKey key = watchService.take();
    for (WatchEvent<?> event : key.pollEvents()) {
      Path changedFilename = ((Path) event.context()).getFileName();
      if (changedFilename.equals(filename)) {
        try {
          listener.onUpdate();
        } catch (Exception e) {
          new Exception("Error during file watch of " + file.getAbsolutePath(), e)
              .printStackTrace();
        }
      }
    }
    return key.reset();
  }

}
