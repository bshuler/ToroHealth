package net.torocraft.torohealth.render;

import net.minecraft.client.gui.Font;
//? if >=26.1 {
import net.minecraft.resources.Identifier;
//?} else {
/*import net.minecraft.resources.ResourceLocation;
*///?}
import net.minecraft.world.entity.LivingEntity;

/**
 * Loader/version-agnostic drawing surface for ToroHealth's HUD-rendering
 * pipeline ({@code Hud}, {@code BarDisplay}, {@code EntityDisplay}).
 *
 * <p>Modeled on the sibling mod FlightHud's {@code FlightCanvas}/
 * {@code PlatformCanvas} split: every rendering-logic file below depends
 * ONLY on this interface, never on {@code GuiGraphics}/{@code PoseStack}
 * directly, so none of them carry any Stonecutter version conditionals.
 * {@link PlatformHudCanvas} is the ONE Stonecutter-branched file
 * implementing this interface — see its class doc for the per-era shapes.
 */
public interface HudCanvas {

  void fill(int x1, int y1, int x2, int y2, int color);

  void drawString(Font font, String text, int x, int y, int color, boolean dropShadow);

  void pushPose();

  void popPose();

  void translate(float x, float y, float z);

  void scale(float x, float y, float z);

  /**
   * Draws a texture over {@code [x, y, x+width, y+height)}, sampling from
   * {@code [u, v, u+width, v+height)} of a {@code texWidth x texHeight}
   * texture. Mirrors the {@code blit(ResourceLocation, x, y, u, v, width,
   * height, texWidth, texHeight)} call shape shared across every era except
   * that pre-1.20 needs the texture bound separately first — see
   * {@link PlatformHudCanvas} for how each era resolves that.
   */
  //? if >=26.1 {
  void blitBackground(Identifier texture, int x, int y, float u, float v, int width,
      int height, int texWidth, int texHeight);
  //?} else {
  /*void blitBackground(ResourceLocation texture, int x, int y, float u, float v, int width,
      int height, int texWidth, int texHeight);
  *///?}

  /**
   * Renders {@code entity} as an inventory-style icon centered at
   * {@code (x, y)}, facing away from the camera and rotated by
   * {@code yRotDegrees} (typically the entity's own {@code getYRot()}).
   * The exact underlying API ({@code InventoryScreen.renderEntityInInventory})
   * has three incompatible shapes across this project's target versions —
   * see {@link PlatformHudCanvas} for the javap-confirmed detail per era.
   */
  void renderEntity(float x, float y, float scale, float yRotDegrees, LivingEntity entity);
}
