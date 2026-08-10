package net.torocraft.torohealth;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.torocraft.torohealth.bars.BarStates;
import net.torocraft.torohealth.bars.HealthBarRenderer;
import net.torocraft.torohealth.bars.ParticleRenderer;
import net.torocraft.torohealth.util.HoldingWeaponUpdater;

//? if fabric && >=26.1 {
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} elif fabric {
/*import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
*///?} elif neoforge && >=26.1 {
/*import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
*///?} elif neoforge {
/*import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
*///?} elif forge && <1.19 {
/*// 1.18.2: the GUI overlay event is still RenderGameOverlayEvent (renamed
// RenderGuiOverlayEvent starting forge 1.19) and there is no VanillaGuiOverlay
// registry yet - just a plain ElementType enum (confirmed via javap against
// the real forge-1.18.2-40.3.12 universal jar).
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
*///?} elif forge {
/*import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
*///?}

// Fabric's HudRenderCallback interface itself has a different method
// signature per era (PoseStack+float pre-1.20; GuiGraphics+float 1.20-1.21.1;
// GuiGraphics+DeltaTracker >=1.21.2 - see onHudRender's 3-way split below),
// so this import is independent of the loader-conditioned block above.
//? if fabric && <1.20 {
/*import com.mojang.blaze3d.vertex.PoseStack;
*///?}

/**
 * Per-loader registration of the HUD/world-render/tick hooks that drive this
 * mod. Fabric registers callbacks directly in the constructor call below;
 * NeoForge/Forge use static {@code @SubscribeEvent} methods on a class
 * registered against the mod event bus (see {@code ToroHealth}'s
 * per-loader constructor) — see CLAUDE.md's event-registration table.
 *
 * <p>Every conditional below is a flat if/elif/else chain - the forge arm is
 * split into {@code forge && <1.19} (1.18.2, {@code RenderGameOverlayEvent}),
 * {@code forge && <1.20} (1.19.4, {@code RenderGuiOverlayEvent} +
 * {@code getPoseStack()}) and a plain {@code forge} catch-all (1.20.1,
 * {@code RenderGuiOverlayEvent} + {@code getGuiGraphics()}) as three separate
 * top-level arms rather than nesting a second conditional inside forge's own
 * arm, matching the flat-chain style already proven throughout this file and
 * {@code PlatformHudCanvas}.
 */
public class ClientEventHandler {

  //? if fabric && >=26.1 {
  // 26.2's Fabric API replaced HudRenderCallback/WorldRenderEvents.
  // AFTER_TRANSLUCENT with HudElementRegistry (HudElement.extractRenderState
  // takes GuiGraphicsExtractor+DeltaTracker, matching Hud's own >=26.1
  // draw(...) overload) and LevelRenderEvents.AFTER_TRANSLUCENT_FEATURES
  // (LevelRenderContext exposes poseStack()+submitNodeCollector() but, like
  // every other 26.1+ world-render hook confirmed via javap this port, no
  // Camera accessor - fetched separately via
  // Minecraft.getInstance().gameRenderer.mainCamera()). ClientTickEvents is
  // unchanged. Neither HealthBarRenderer.renderInWorld's partialTick
  // parameter nor ParticleRenderer.renderParticles use partial-tick
  // interpolation in their >=26.1 bodies, so 0.0f is passed rather than
  // threading a DeltaTracker through just to satisfy the parameter.
  public static void register() {
    HudElementRegistry.addLast(ToroHealth.id("hud"), ClientEventHandler::onHudRender);
    LevelRenderEvents.AFTER_TRANSLUCENT_FEATURES.register(
        ClientEventHandler::onWorldRenderAfterTranslucentFeatures);
    ClientTickEvents.END_CLIENT_TICK.register(ClientEventHandler::onClientTick);
  }

  private static void onHudRender(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
    Minecraft client = Minecraft.getInstance();
    ToroHealth.HUD.draw(guiGraphics, deltaTracker.getGameTimeDeltaPartialTick(false),
        client.getWindow().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight());
  }

  private static void onWorldRenderAfterTranslucentFeatures(LevelRenderContext context) {
    Camera camera = Minecraft.getInstance().gameRenderer.mainCamera();
    HealthBarRenderer.renderInWorld(0.0f, context.poseStack(), camera,
        context.submitNodeCollector());
    ParticleRenderer.renderParticles(context.poseStack(), camera, context.submitNodeCollector());
  }

  private static void onClientTick(Minecraft client) {
    clientTick(client);
  }
  //?} elif fabric {
  /*public static void register() {
    HudRenderCallback.EVENT.register(ClientEventHandler::onHudRender);
    WorldRenderEvents.AFTER_TRANSLUCENT.register(ClientEventHandler::onWorldRenderAfterTranslucent);
    ClientTickEvents.END_CLIENT_TICK.register(ClientEventHandler::onClientTick);
  }

  //? if <1.20 {
  /^private static void onHudRender(PoseStack poseStack, float tickDelta) {
    Minecraft client = Minecraft.getInstance();
    ToroHealth.HUD.draw(poseStack, tickDelta,
        client.getWindow().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight());
  }
  ^///?} elif >=1.21.2 {
  private static void onHudRender(net.minecraft.client.gui.GuiGraphics guiGraphics,
      net.minecraft.client.DeltaTracker deltaTracker) {
    Minecraft client = Minecraft.getInstance();
    ToroHealth.HUD.draw(guiGraphics, deltaTracker.getGameTimeDeltaPartialTick(false),
        client.getWindow().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight());
  }
  //?} else {
  /^private static void onHudRender(net.minecraft.client.gui.GuiGraphics guiGraphics,
      float tickDelta) {
    Minecraft client = Minecraft.getInstance();
    ToroHealth.HUD.draw(guiGraphics, tickDelta,
        client.getWindow().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight());
  }
  ^///?}

  private static void onWorldRenderAfterTranslucent(WorldRenderContext context) {
    //? if >=1.21.2 {
    HealthBarRenderer.renderInWorld(context.tickCounter().getGameTimeDeltaPartialTick(false),
        context.matrixStack(), context.camera());
    //?} else {
    /^HealthBarRenderer.renderInWorld(context.tickDelta(), context.matrixStack(), context.camera());
    ^///?}
    ParticleRenderer.renderParticles(context.matrixStack(), context.camera());
  }

  private static void onClientTick(Minecraft client) {
    clientTick(client);
  }
  *///?} elif neoforge && >=26.1 {
  /*// RenderLevelStageEvent was restructured in 26.2 (confirmed via javap
  // against the real neoforge-26.2.0.58 jar): it dropped its Stage enum +
  // getStage() entirely in favor of one concrete subclass per stage, and -
  // critically - NONE of those subclasses expose a SubmitNodeCollector
  // anymore. SubmitCustomGeometryEvent is the only NeoForge 26.2 event that
  // still exposes one, so it replaces RenderLevelStageEvent.AFTER_PARTICLES
  // for this mod's world-render hook. RegisterGuiLayersEvent/GuiLayer are
  // structurally unchanged apart from GuiGraphics -> GuiGraphicsExtractor
  // (also confirmed via javap).
  @EventBusSubscriber(modid = ToroHealth.MODID, value = Dist.CLIENT)
  public static class Fml {

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
      event.registerAboveAll(ToroHealth.id("hud"), ClientEventHandler::renderGuiLayer);
    }

    @SubscribeEvent
    public static void onSubmitCustomGeometry(SubmitCustomGeometryEvent event) {
      Camera camera = Minecraft.getInstance().gameRenderer.mainCamera();
      HealthBarRenderer.renderInWorld(0.0f, event.getPoseStack(), camera,
          event.getSubmitNodeCollector());
      ParticleRenderer.renderParticles(event.getPoseStack(), camera, event.getSubmitNodeCollector());
    }

    @SubscribeEvent
    public static void onPlayerTickPre(PlayerTickEvent.Pre event) {
      Minecraft client = Minecraft.getInstance();
      if (client.player == event.getEntity()) {
        clientTick(client);
      }
    }
  }

  private static void renderGuiLayer(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
    Minecraft client = Minecraft.getInstance();
    ToroHealth.HUD.draw(guiGraphics, deltaTracker.getGameTimeDeltaPartialTick(false),
        client.getWindow().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight());
  }
  *///?} elif neoforge {
  /*@EventBusSubscriber(modid = ToroHealth.MODID, value = Dist.CLIENT)
  public static class Fml {

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
      event.registerAboveAll(ToroHealth.id("hud"), ClientEventHandler::renderGuiLayer);
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
      if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
        return;
      }
      HealthBarRenderer.renderInWorld(event.getPartialTick().getGameTimeDeltaPartialTick(false),
          event.getPoseStack(), event.getCamera());
      ParticleRenderer.renderParticles(event.getPoseStack(), event.getCamera());
    }

    @SubscribeEvent
    public static void onPlayerTickPre(PlayerTickEvent.Pre event) {
      Minecraft client = Minecraft.getInstance();
      if (client.player == event.getEntity()) {
        clientTick(client);
      }
    }
  }

  private static void renderGuiLayer(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
    Minecraft client = Minecraft.getInstance();
    ToroHealth.HUD.draw(guiGraphics, deltaTracker.getGameTimeDeltaPartialTick(false),
        client.getWindow().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight());
  }
  *///?} elif forge && <1.19 {
  /*// 1.18.2: RenderGameOverlayEvent has no VanillaGuiOverlay registry - ALL
  // fires once per frame after every individual overlay layer has rendered
  // (Post), giving the same "draw once, after vanilla HUD" semantics as the
  // >=1.19 POTION_ICONS-keyed Pre hook below (confirmed via javap against
  // the real forge-1.18.2-40.3.12 universal jar).
  @Mod.EventBusSubscriber(modid = ToroHealth.MODID, value = Dist.CLIENT)
  public static class Fml {

    @SubscribeEvent
    public static void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
      if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) {
        return;
      }
      Minecraft client = Minecraft.getInstance();
      ToroHealth.HUD.draw(event.getMatrixStack(), event.getPartialTicks(),
          client.getWindow().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight());
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
      if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
        return;
      }
      HealthBarRenderer.renderInWorld(event.getPartialTick(), event.getPoseStack(),
          event.getCamera());
      ParticleRenderer.renderParticles(event.getPoseStack(), event.getCamera());
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
      if (event.phase != TickEvent.Phase.START) {
        return;
      }
      Minecraft client = Minecraft.getInstance();
      if (client.player == event.player) {
        clientTick(client);
      }
    }
  }
  *///?} elif forge && <1.20 {
  /*// 1.19.4: RenderGuiOverlayEvent predates GuiGraphics - it only
  // exposes getPoseStack() (confirmed via javap against the real forge-1.19.4
  // universal jar; RenderGuiOverlayEvent/VanillaGuiOverlay/RenderLevelStageEvent
  // are otherwise unchanged from the >=1.20 shape below).
  @Mod.EventBusSubscriber(modid = ToroHealth.MODID, value = Dist.CLIENT)
  public static class Fml {

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Pre event) {
      if (event.getOverlay().id() != VanillaGuiOverlay.POTION_ICONS.id()) {
        return;
      }
      Minecraft client = Minecraft.getInstance();
      ToroHealth.HUD.draw(event.getPoseStack(), event.getPartialTick(),
          client.getWindow().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight());
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
      if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
        return;
      }
      HealthBarRenderer.renderInWorld(event.getPartialTick(), event.getPoseStack(),
          event.getCamera());
      ParticleRenderer.renderParticles(event.getPoseStack(), event.getCamera());
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
      if (event.phase != TickEvent.Phase.START) {
        return;
      }
      Minecraft client = Minecraft.getInstance();
      if (client.player == event.player) {
        clientTick(client);
      }
    }
  }
  *///?} elif forge {
  /*// 1.20.1 (forge's only >=1.20 target in this project's matrix):
  // RenderGuiOverlayEvent exposes getGuiGraphics() instead of getPoseStack().
  @Mod.EventBusSubscriber(modid = ToroHealth.MODID, value = Dist.CLIENT)
  public static class Fml {

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Pre event) {
      if (event.getOverlay().id() != VanillaGuiOverlay.POTION_ICONS.id()) {
        return;
      }
      Minecraft client = Minecraft.getInstance();
      ToroHealth.HUD.draw(event.getGuiGraphics(), event.getPartialTick(),
          client.getWindow().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight());
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
      if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
        return;
      }
      HealthBarRenderer.renderInWorld(event.getPartialTick(), event.getPoseStack(),
          event.getCamera());
      ParticleRenderer.renderParticles(event.getPoseStack(), event.getCamera());
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
      if (event.phase != TickEvent.Phase.START) {
        return;
      }
      Minecraft client = Minecraft.getInstance();
      if (client.player == event.player) {
        clientTick(client);
      }
    }
  }
  *///?}

  private static void clientTick(Minecraft client) {
    if (client.player == null || client.level == null) {
      return;
    }

    HoldingWeaponUpdater.update();
    BarStates.tick();

    LivingEntity target = ToroHealth.RAYTRACE.getEntityInCrosshair(1.0F,
        ToroHealth.CONFIG.hud.distance);
    ToroHealth.HUD.setEntity(target);
    ToroHealth.HUD.tick();

    for (var entity : client.level.entitiesForRendering()) {
      if (entity instanceof LivingEntity livingEntity) {
        HealthBarRenderer.prepareRenderInWorld(livingEntity);
      }
    }
  }
}
