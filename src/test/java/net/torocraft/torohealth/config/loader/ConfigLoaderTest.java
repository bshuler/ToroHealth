package net.torocraft.torohealth.config.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.torocraft.torohealth.config.Config;
import net.torocraft.torohealth.config.Config.AnchorPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigLoaderTest {

  @TempDir
  File tempDir;

  private Config newDefaultConfig(boolean watch) {
    Config config = new Config();
    config.watchForChanges = watch;
    return config;
  }

  @Test
  void load_writesDefaultConfigWhenFileIsMissingAndInvokesCallback() {
    File file = new File(tempDir, "torohealth.json");
    List<Config> loaded = new ArrayList<>();
    ConfigLoader<Config> loader =
        new ConfigLoader<>(newDefaultConfig(false), tempDir, "torohealth.json", loaded::add);

    assertFalse(file.exists());
    loader.load();

    assertTrue(file.exists());
    assertEquals(1, loaded.size());
    assertEquals(AnchorPoint.TOP_LEFT, loaded.get(0).hud.anchorPoint);
    assertFalse(loader.isWatching());
  }

  @Test
  void load_readsExistingFileInsteadOfOverwritingIt() throws IOException {
    File file = new File(tempDir, "torohealth.json");
    try (FileWriter writer = new FileWriter(file)) {
      writer.write("{\"watchForChanges\":false,"
          + "\"hud\":{\"scale\":9.5,\"anchorPoint\":\"BOTTOM_RIGHT\"}}");
    }

    List<Config> loaded = new ArrayList<>();
    ConfigLoader<Config> loader =
        new ConfigLoader<>(newDefaultConfig(true), tempDir, "torohealth.json", loaded::add);
    loader.load();

    assertEquals(1, loaded.size());
    Config config = loaded.get(0);
    // Values came from the pre-existing file, not the constructor's defaults.
    assertEquals(9.5f, config.hud.scale, 1e-6f);
    assertEquals(AnchorPoint.BOTTOM_RIGHT, config.hud.anchorPoint);
    assertEquals(20, config.hud.hideDelay, "a field absent from the file keeps Hud()'s own default");
  }

  @Test
  void load_startsWatchingWhenLoadedConfigRequestsIt() {
    ConfigLoader<Config> loader =
        new ConfigLoader<>(newDefaultConfig(true), tempDir, "torohealth.json", config -> { });

    loader.load();

    assertTrue(loader.isWatching());
  }

  @Test
  void watch_isIdempotentWhenCalledTwice() {
    File file = new File(tempDir, "torohealth.json");
    ConfigLoader<Config> loader =
        new ConfigLoader<>(newDefaultConfig(false), tempDir, "torohealth.json", config -> { });

    assertFalse(loader.isWatching());
    loader.watch(file);
    assertTrue(loader.isWatching());

    // Second call must hit the early-return guard, not create/replace a watcher.
    loader.watch(file);
    assertTrue(loader.isWatching());
  }

  @Test
  void reload_reReadsFileAndInvokesCallbackAgain() {
    // reload() is the exact callback watch() wires up to the FileWatcher's
    // background thread; invoking it directly verifies that callback path
    // without waiting on a real filesystem-change event to fire it.
    File file = new File(tempDir, "torohealth.json");
    List<Config> loaded = new ArrayList<>();
    ConfigLoader<Config> loader =
        new ConfigLoader<>(newDefaultConfig(false), tempDir, "torohealth.json", loaded::add);

    loader.load();
    assertEquals(1, loaded.size());

    loader.reload();
    assertEquals(2, loaded.size());
  }

  @Test
  void read_returnsDefaultConfigWhenFileDoesNotExist() {
    Config defaultConfig = newDefaultConfig(false);
    ConfigLoader<Config> loader =
        new ConfigLoader<>(defaultConfig, tempDir, "missing.json", config -> { });

    Config result = loader.read();

    assertEquals(defaultConfig, result);
  }

  @Test
  void read_returnsDefaultConfigWhenFileContentsAreInvalidJson() throws IOException {
    File file = new File(tempDir, "torohealth.json");
    try (FileWriter writer = new FileWriter(file)) {
      writer.write("not valid json {{{");
    }

    Config defaultConfig = newDefaultConfig(false);
    ConfigLoader<Config> loader =
        new ConfigLoader<>(defaultConfig, tempDir, "torohealth.json", config -> { });

    Config result = loader.read();

    assertEquals(defaultConfig, result);
  }

  @Test
  void read_parsesWellFormedJsonIntoTheConfigType() throws IOException {
    File file = new File(tempDir, "torohealth.json");
    try (FileWriter writer = new FileWriter(file)) {
      writer.write("{\"hud\":{\"scale\":3.5}}");
    }

    ConfigLoader<Config> loader =
        new ConfigLoader<>(newDefaultConfig(false), tempDir, "torohealth.json", config -> { });

    Config result = loader.read();

    assertEquals(3.5f, result.hud.scale, 1e-6f);
  }

  @Test
  void save_writesJsonRepresentationOfConfigToFile() {
    ConfigLoader<Config> loader =
        new ConfigLoader<>(newDefaultConfig(false), tempDir, "torohealth.json", config -> { });
    Config toSave = newDefaultConfig(false);
    toSave.hud.scale = 2.5f;

    loader.save(toSave);

    Config readBack = loader.read();
    assertEquals(2.5f, readBack.hud.scale, 1e-6f);
  }

  @Test
  void save_swallowsIoFailureWhenTargetCannotBeWritten() {
    // A directory can never be opened as a FileWriter target -- exercises
    // save()'s catch branch without needing filesystem-permission tricks.
    File asDirectory = new File(tempDir, "torohealth.json");
    assertTrue(asDirectory.mkdir());
    ConfigLoader<Config> loader =
        new ConfigLoader<>(newDefaultConfig(false), tempDir, "torohealth.json", config -> { });

    loader.save(newDefaultConfig(false));
    // No exception propagates; the failure is only logged internally.
  }
}
