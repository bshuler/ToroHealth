package net.torocraft.torohealth.display;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.torocraft.torohealth.ToroHealth;
import net.torocraft.torohealth.config.Config;
import net.torocraft.torohealth.config.Config.AnchorPoint;

public class Hud extends Screen {
  private static final ResourceLocation BACKGROUND_TEXTURE =
      ResourceLocation.fromNamespaceAndPath(ToroHealth.MODID, "textures/gui/default_skin_basic.png");
  private final EntityDisplay entityDisplay = new EntityDisplay();
  private final BarDisplay barDisplay = new BarDisplay();
  private LivingEntity entity;
  private Config config = new Config();
  private int age;

  public Hud() {
    super(Component.literal("ToroHealth HUD"));
    this.minecraft = Minecraft.getInstance();
  }

  public void draw(GuiGraphics guiGraphics, float partialTick, int width, int height) {
    this.config = ToroHealth.CONFIG;
    if (this.config == null) {
      this.config = new Config();
    }
    float x = determineX();
    float y = determineY();
    draw(guiGraphics, x, y, config.hud.scale);
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

  private void draw(GuiGraphics guiGraphics, float x, float y, float scale) {
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

    var matrix = guiGraphics.pose();
    matrix.pushMatrix();

    matrix.translate(finalX, finalY);
    matrix.scale(scale, scale);

    matrix.translate(-10, -10);
    if (config.hud.showSkin) {
      this.drawSkin(guiGraphics);
    }
    matrix.translate(10, 10);

    if (config.hud.showEntity) {
      entityDisplay.draw(guiGraphics, scale, finalX, finalY);
    }

    matrix.translate(44, 0);
    if (config.hud.showBar) {
      barDisplay.draw(guiGraphics, entity);
    }

    matrix.popMatrix();
  }

  private void drawSkin(GuiGraphics guiGraphics) {
    int w = 160, h = 60;
    guiGraphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND_TEXTURE, 0, 0, 0.0f, 0.0f, w, h, w,
        h, w, h);
  }
}
