package net.torocraft.torohealth.display;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
//? if <1.19 {
/*import net.minecraft.network.chat.TextComponent;
*///?}
//? if >=26.1 {
import net.minecraft.resources.Identifier;
//?} else {
/*import net.minecraft.resources.ResourceLocation;
*///?}
import net.minecraft.world.entity.LivingEntity;
import net.torocraft.torohealth.ToroHealth;
import net.torocraft.torohealth.config.Config;
import net.torocraft.torohealth.config.Config.AnchorPoint;
import net.torocraft.torohealth.render.HudCanvas;
import net.torocraft.torohealth.render.PlatformHudCanvas;

//? if <1.20 {
/*import com.mojang.blaze3d.vertex.PoseStack;
*///?} elif >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else {
/*import net.minecraft.client.gui.GuiGraphics;
*///?}

/**
 * The public entry point ({@link #draw(HudCanvas, float, int, int)}'s two
 * per-era overloads below) is the only place this class still branches on a
 * Stonecutter condition - it exists purely to construct the right
 * {@link PlatformHudCanvas} for whatever context the caller was handed
 * ({@code PoseStack} pre-1.20, {@code GuiGraphics} otherwise; see
 * {@code ClientEventHandler}). Every method below that is private takes
 * {@link HudCanvas} and carries zero version conditionals of its own,
 * mirroring the sibling mod FlightHud's canvas-abstraction pattern.
 */
public class Hud extends Screen {
  //? if >=26.1 {
  private static final Identifier BACKGROUND_TEXTURE =
      Identifier.fromNamespaceAndPath(ToroHealth.MODID, "textures/gui/default_skin_basic.png");
  //?} elif >=1.21 {
  /*private static final ResourceLocation BACKGROUND_TEXTURE =
      ResourceLocation.fromNamespaceAndPath(ToroHealth.MODID, "textures/gui/default_skin_basic.png");
  *///?} else {
  /*private static final ResourceLocation BACKGROUND_TEXTURE =
      new ResourceLocation(ToroHealth.MODID, "textures/gui/default_skin_basic.png");
  *///?}
  private final EntityDisplay entityDisplay = new EntityDisplay();
  private final BarDisplay barDisplay = new BarDisplay();
  private LivingEntity entity;
  private Config config = new Config();
  private int age;

  public Hud() {
    //? if <1.19 {
    /*super(new TextComponent("ToroHealth HUD"));
    this.minecraft = Minecraft.getInstance();
    *///?} elif >=26.1 {
    // Screen.minecraft is `final` in 26.2 (confirmed via javap); the new
    // 3-arg Screen(Minecraft, Font, Component) constructor lets a subclass
    // supply it at construction time instead of assigning the field
    // afterward, which no longer compiles against a final field.
    super(Minecraft.getInstance(), Minecraft.getInstance().font, Component.literal("ToroHealth HUD"));
    //?} else {
    /*super(Component.literal("ToroHealth HUD"));
    this.minecraft = Minecraft.getInstance();
    *///?}
  }

  //? if <1.20 {
  /*public void draw(PoseStack poseStack, float partialTick, int width, int height) {
    draw(new PlatformHudCanvas(poseStack), partialTick, width, height);
  }
  *///?} elif >=26.1 {
  public void draw(GuiGraphicsExtractor guiGraphics, float partialTick, int width, int height) {
    draw(new PlatformHudCanvas(guiGraphics), partialTick, width, height);
  }
  //?} else {
  /*public void draw(GuiGraphics guiGraphics, float partialTick, int width, int height) {
    draw(new PlatformHudCanvas(guiGraphics), partialTick, width, height);
  }
  *///?}

  private void draw(HudCanvas canvas, float partialTick, int width, int height) {
    this.config = ToroHealth.CONFIG;
    if (this.config == null) {
      this.config = new Config();
    }
    float x = determineX();
    float y = determineY();
    draw(canvas, x, y, config.hud.scale);
  }

  private float determineX() {
    float x = config.hud.x;
    AnchorPoint anchor = config.hud.anchorPoint;
    Minecraft mc = Minecraft.getInstance();
    float wScreen = mc.getWindow().getGuiScaledWidth();

    switch (anchor) {
      case BOTTOM_CENTER:
      case TOP_CENTER:
        return (wScreen / 2) + x;
      case BOTTOM_RIGHT:
      case TOP_RIGHT:
        return wScreen - x;
      default:
        return x;
    }
  }

  private float determineY() {
    float y = config.hud.y;
    AnchorPoint anchor = config.hud.anchorPoint;
    Minecraft mc = Minecraft.getInstance();
    float hScreen = mc.getWindow().getGuiScaledHeight();

    switch (anchor) {
      case BOTTOM_CENTER:
      case BOTTOM_LEFT:
      case BOTTOM_RIGHT:
        return hScreen - y;
      default:
        return y;
    }
  }

  public void tick() {
    age++;
  }

  public void setEntity(LivingEntity entity) {
    if (entity != null) {
      age = 0;
    }

    if (entity == null && age > config.hud.hideDelay) {
      setEntityWork(null);
    }

    if (entity != null && entity != this.entity) {
      setEntityWork(entity);
    }
  }

  private void setEntityWork(LivingEntity entity) {
    this.entity = entity;
    entityDisplay.setEntity(entity);
  }

  public LivingEntity getEntity() {
    return entity;
  }

  private void draw(HudCanvas canvas, float x, float y, float scale) {
    if (entity == null) {
      return;
    }

    if (config.hud.onlyWhenHurt && entity.getHealth() >= entity.getMaxHealth()) {
      return;
    }

    AnchorPoint anchor = config.hud.anchorPoint;
    float hudWidth = 160;
    float hudHeight = 60;

    float finalX = x;
    float finalY = y;

    if (anchor == AnchorPoint.TOP_RIGHT || anchor == AnchorPoint.BOTTOM_RIGHT) {
      finalX = x - hudWidth;
    } else if (anchor == AnchorPoint.TOP_CENTER || anchor == AnchorPoint.BOTTOM_CENTER) {
      finalX = x - (hudWidth / 2);
    }

    if (anchor == AnchorPoint.BOTTOM_LEFT || anchor == AnchorPoint.BOTTOM_CENTER
        || anchor == AnchorPoint.BOTTOM_RIGHT) {
      finalY = y - hudHeight;
    }

    canvas.pushPose();

    canvas.translate(finalX, finalY, 0.0f);
    canvas.scale(scale, scale, 1.0f);

    canvas.translate(-10.0f, -10.0f, 0.0f);
    if (config.hud.showSkin) {
      this.drawSkin(canvas);
    }
    canvas.translate(10.0f, 10.0f, 0.0f);

    if (config.hud.showEntity) {
      entityDisplay.draw(canvas, scale, finalX, finalY);
    }

    canvas.translate(44.0f, 0.0f, 0.0f);
    if (config.hud.showBar) {
      barDisplay.draw(canvas, entity);
    }

    canvas.popPose();
  }

  private void drawSkin(HudCanvas canvas) {
    int w = 160, h = 60;
    canvas.blitBackground(BACKGROUND_TEXTURE, 0, 0, 0.0f, 0.0f, w, h, w, h);
  }
}
