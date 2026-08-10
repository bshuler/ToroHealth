package net.torocraft.torohealth;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.torocraft.torohealth.bars.BarStates;
import net.torocraft.torohealth.bars.HealthBarRenderer;
import net.torocraft.torohealth.bars.ParticleRenderer;
import net.torocraft.torohealth.util.HoldingWeaponUpdater;

//? if fabric {
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
//?} elif neoforge {
/*import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
*///?} elif forge {
/*import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
*///?}

/**
 * Per-loader registration of the HUD/world-render/tick hooks that drive this
 * mod. Fabric registers callbacks directly in the constructor call below;
 * NeoForge/Forge use static {@code @SubscribeEvent} methods on a class
 * registered against the mod event bus (see {@code ToroHealth}'s
 * per-loader constructor) — see CLAUDE.md's event-registration table.
 */
public class ClientEventHandler {

  //? if fabric {
  public static void register() {
    HudRenderCallback.EVENT.register(ClientEventHandler::onHudRender);
    WorldRenderEvents.AFTER_TRANSLUCENT.register(ClientEventHandler::onWorldRenderAfterTranslucent);
    ClientTickEvents.END_CLIENT_TICK.register(ClientEventHandler::onClientTick);
  }

  private static void onHudRender(net.minecraft.client.gui.GuiGraphics guiGraphics,
      net.minecraft.client.DeltaTracker deltaTracker) {
    Minecraft client = Minecraft.getInstance();
    ToroHealth.HUD.draw(guiGraphics, deltaTracker.getGameTimeDeltaPartialTick(false),
        client.getWindow().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight());
  }

  private static void onWorldRenderAfterTranslucent(WorldRenderEvents.AfterTranslucent context) {
    HealthBarRenderer.renderInWorld(context.tickCounter().getGameTimeDeltaPartialTick(false),
        context.matrixStack(), context.camera());
    ParticleRenderer.renderParticles(context.matrixStack(), context.camera());
  }

  private static void onClientTick(Minecraft client) {
    clientTick(client);
  }
  //?} elif neoforge {
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
      HealthBarRenderer.renderInWorld(event.getPartialTick(), event.getPoseStack(),
          event.getCamera());
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
  *///?} elif forge {
  /*@Mod.EventBusSubscriber(modid = ToroHealth.MODID, value = Dist.CLIENT)
  public static class Fml {

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Pre event) {
      if (event.getOverlay().id() != VanillaGuiOverlay.EFFECTS.id()) {
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
