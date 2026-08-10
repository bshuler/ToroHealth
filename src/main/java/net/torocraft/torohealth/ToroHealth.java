package net.torocraft.torohealth;

import com.mojang.logging.LogUtils;
import java.io.File;
import java.util.Random;
import net.minecraft.resources.ResourceLocation;
import net.torocraft.torohealth.config.Config;
import net.torocraft.torohealth.config.loader.ConfigLoader;
import net.torocraft.torohealth.display.Hud;
import net.torocraft.torohealth.util.RayTrace;
import org.slf4j.Logger;

//? if fabric {
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
//?} elif neoforge {
/*import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
*///?} elif forge {
/*import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
*///?}

/**
 * Shared statics plus the per-loader entry point.
 *
 * <p>Config-directory resolution is loader-specific (Fabric's
 * {@code FabricLoader.getConfigDir()} vs. NeoForge/Forge's
 * {@code FMLPaths.CONFIGDIR}), so it lives here rather than in
 * {@code ConfigLoader} itself — see CLAUDE.md, "src/main vs src/client".
 *
 * <p>{@code @Mod} must annotate the class itself (not a constructor), so
 * NeoForge/Forge need their own class declaration line - the whole class
 * body is therefore Stonecutter-conditioned per loader rather than just the
 * constructor, mirroring the house template (critical-orientation's
 * OrientationClient.java).
 */
//? if fabric {
public class ToroHealth implements ClientModInitializer {

  public static final String MODID = "torohealth";
  public static final Logger LOGGER = LogUtils.getLogger();
  public static final Random RAND = new Random();

  public static final RayTrace RAYTRACE = new RayTrace();
  public static final Hud HUD = new Hud();

  public static volatile Config CONFIG = new Config();
  public static volatile boolean IS_HOLDING_WEAPON = false;

  private static ConfigLoader<Config> configLoader;

  public static ResourceLocation id(String path) {
    return ResourceLocation.fromNamespaceAndPath(MODID, path);
  }

  public static void saveConfig() {
    if (configLoader != null) {
      configLoader.save(CONFIG);
    }
  }

  private static void loadConfig(File configDir) {
    configLoader = new ConfigLoader<>(new Config(), configDir, MODID + ".json",
        config -> CONFIG = config);
    configLoader.load();
  }

  @Override
  public void onInitializeClient() {
    loadConfig(FabricLoader.getInstance().getConfigDir().toFile());
    ClientEventHandler.register();
  }
}
//?} elif neoforge {
/*@Mod(ToroHealth.MODID)
public class ToroHealth {

  public static final String MODID = "torohealth";
  public static final Logger LOGGER = LogUtils.getLogger();
  public static final Random RAND = new Random();

  public static final RayTrace RAYTRACE = new RayTrace();
  public static final Hud HUD = new Hud();

  public static volatile Config CONFIG = new Config();
  public static volatile boolean IS_HOLDING_WEAPON = false;

  private static ConfigLoader<Config> configLoader;

  public static ResourceLocation id(String path) {
    return ResourceLocation.fromNamespaceAndPath(MODID, path);
  }

  public static void saveConfig() {
    if (configLoader != null) {
      configLoader.save(CONFIG);
    }
  }

  private static void loadConfig(File configDir) {
    configLoader = new ConfigLoader<>(new Config(), configDir, MODID + ".json",
        config -> CONFIG = config);
    configLoader.load();
  }

  public ToroHealth(IEventBus modEventBus) {
    loadConfig(FMLPaths.CONFIGDIR.get().toFile());
  }
}
*///?} elif forge {
/*@Mod(ToroHealth.MODID)
public class ToroHealth {

  public static final String MODID = "torohealth";
  public static final Logger LOGGER = LogUtils.getLogger();
  public static final Random RAND = new Random();

  public static final RayTrace RAYTRACE = new RayTrace();
  public static final Hud HUD = new Hud();

  public static volatile Config CONFIG = new Config();
  public static volatile boolean IS_HOLDING_WEAPON = false;

  private static ConfigLoader<Config> configLoader;

  public static ResourceLocation id(String path) {
    return ResourceLocation.fromNamespaceAndPath(MODID, path);
  }

  public static void saveConfig() {
    if (configLoader != null) {
      configLoader.save(CONFIG);
    }
  }

  private static void loadConfig(File configDir) {
    configLoader = new ConfigLoader<>(new Config(), configDir, MODID + ".json",
        config -> CONFIG = config);
    configLoader.load();
  }

  public ToroHealth() {
    loadConfig(FMLPaths.CONFIGDIR.get().toFile());
  }
}
*///?}
