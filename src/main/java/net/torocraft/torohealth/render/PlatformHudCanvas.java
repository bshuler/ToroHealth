package net.torocraft.torohealth.render;

import net.minecraft.client.gui.Font;
//? if >=26.1 {
/*import net.minecraft.resources.Identifier;
*///?} else {
import net.minecraft.resources.ResourceLocation;
//?}
import net.minecraft.world.entity.LivingEntity;
import net.torocraft.torohealth.util.Colors;

//? if <1.19 {
/*import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
*///?} elif <1.20 {
/*import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.joml.Quaternionf;
*///?} elif >=26.1 {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderPipelines;
*///?} elif >=1.21 {
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderType;
import org.joml.Quaternionf;
import org.joml.Vector3f;
//?} else {
/*import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.joml.Quaternionf;
*///?}

/**
 * The ONE Stonecutter-branched file behind {@link HudCanvas}, modeled
 * directly on the sibling mod FlightHud's {@code PlatformCanvas}. Every
 * other rendering-logic file ({@code Hud}, {@code BarDisplay},
 * {@code EntityDisplay}) is written once against {@link HudCanvas} and
 * never branches on a Stonecutter condition at all.
 *
 * <p>Two real shapes for the wrapped context (all confirmed via {@code
 * javap} against the real per-version mapped/loader jars):
 * <ul>
 *   <li>{@code <1.20} (1.18.2, 1.19.4): {@code GuiGraphics} does not exist
 *       yet. Drawing goes through a raw {@code PoseStack} plus
 *       {@code GuiComponent}'s static helpers instead — identical shape
 *       between 1.18.2 and 1.19.4 for {@code fill}/{@code drawString}/
 *       {@code blit}.</li>
 *   <li>else (1.20.1, 1.21.4): wraps {@code GuiGraphics} directly.</li>
 * </ul>
 *
 * <p>{@link #renderEntity} and the import block additionally need a flat
 * {@code <1.19} / {@code <1.20} / {@code >=1.21} / else split (four real
 * shapes, all confirmed via javap) rather than the two above:
 * <ul>
 *   <li>1.18.2: {@code InventoryScreen.renderEntityInInventory(int x, int y,
 *       int scale, float mouseX, float mouseY, LivingEntity)} — no context
 *       object at all, predates both {@code GuiGraphics} and JOML in
 *       Mojang's own math types (confirmed: {@code org.joml} is not even on
 *       1.18.2's classpath, so importing {@code Quaternionf} unconditionally
 *       would itself fail to compile for this cell).</li>
 *   <li>1.19.4: {@code (PoseStack, int x, int y, int scale, Quaternionf,
 *       Quaternionf, LivingEntity)}.</li>
 *   <li>1.20.1 (else below): identical to 1.19.4's shape but with
 *       {@code GuiGraphics} in place of {@code PoseStack}.</li>
 *   <li>&gt;=1.21: {@code (GuiGraphics, float x, float y, float scale,
 *       Vector3f translation, Quaternionf, Quaternionf, LivingEntity)}.</li>
 * </ul>
 *
 * <p>Every conditional in this file is a flat if/elif/else chain — never one
 * Stonecutter conditional nested inside another. Nesting a conditional
 * inside a branch that may itself be commented out corrupted the outer
 * comment block the one time it was tried in this project (see
 * {@code ToroHealth.java}'s {@code id()} method history / PLAN.md); flat
 * chains sidestep that risk entirely.
 */
public class PlatformHudCanvas implements HudCanvas {

  //? if <1.20 {
  /*private final PoseStack pose;

  public PlatformHudCanvas(PoseStack pose) {
    this.pose = pose;
  }
  *///?} elif >=26.1 {
  /*private final GuiGraphicsExtractor context;

  public PlatformHudCanvas(GuiGraphicsExtractor context) {
    this.context = context;
  }
  *///?} else {
  private final GuiGraphics context;

  public PlatformHudCanvas(GuiGraphics context) {
    this.context = context;
  }
  //?}

  @Override
  public void fill(int x1, int y1, int x2, int y2, int color) {
    //? if <1.20 {
    /*GuiComponent.fill(pose, x1, y1, x2, y2, color);
    *///?} else {
    context.fill(x1, y1, x2, y2, color);
    //?}
  }

  /**
   * {@inheritDoc}
   *
   * <p>Every branch normalises the colour through
   * {@link Colors#opaqueIfNoAlpha(int)} first. On the {@code >=26.1} branch
   * that is load-bearing rather than defensive: 26.x deleted {@code Font}'s
   * own alpha fixup, so any caller passing a bare {@code 0xRRGGBB} draws
   * nothing at all there while drawing correctly everywhere else. Doing it
   * for all three branches keeps them identical in behaviour and means a
   * future caller cannot reintroduce the bug by passing a config colour
   * straight through. See {@link Colors} for the {@code javap} evidence.
   */
  @Override
  public void drawString(Font font, String text, int x, int y, int color, boolean dropShadow) {
    int argb = Colors.opaqueIfNoAlpha(color);
    //? if <1.20 {
    /*if (dropShadow) {
      GuiComponent.drawString(pose, font, text, x, y, argb);
    } else {
      font.draw(pose, text, x, y, argb);
    }
    *///?} elif >=26.1 {
    /*context.text(font, text, x, y, argb, dropShadow);
    *///?} else {
    context.drawString(font, text, x, y, argb, dropShadow);
    //?}
  }

  @Override
  public void pushPose() {
    //? if <1.20 {
    /*pose.pushPose();
    *///?} elif >=26.1 {
    /*// Matrix3x2fStack (26.2's replacement for PoseStack in the GUI pipeline)
    // uses pushMatrix()/popMatrix() naming instead of pushPose()/popPose(),
    // confirmed via javap and mirrored from the sibling mod FlightHud's
    // PlatformCanvas, which already proves this exact shape against a real
    // 26.1+ build.
    context.pose().pushMatrix();
    *///?} else {
    context.pose().pushPose();
    //?}
  }

  @Override
  public void popPose() {
    //? if <1.20 {
    /*pose.popPose();
    *///?} elif >=26.1 {
    /*context.pose().popMatrix();
    *///?} else {
    context.pose().popPose();
    //?}
  }

  @Override
  public void translate(float x, float y, float z) {
    //? if <1.20 {
    /*pose.translate(x, y, z);
    *///?} elif >=26.1 {
    /*// Matrix3x2fStack is a 2D affine matrix - translate(x, y) has no z
    // parameter. Every HudCanvas call site in this project passes z=0.0f
    // for GUI drawing, so dropping it here is safe.
    context.pose().translate(x, y);
    *///?} else {
    context.pose().translate(x, y, z);
    //?}
  }

  @Override
  public void scale(float x, float y, float z) {
    //? if <1.20 {
    /*pose.scale(x, y, z);
    *///?} elif >=26.1 {
    /*context.pose().scale(x, y);
    *///?} else {
    context.pose().scale(x, y, z);
    //?}
  }

  //? if <1.20 {
  /*@Override
  public void blitBackground(ResourceLocation texture, int x, int y, float u, float v, int width,
      int height, int texWidth, int texHeight) {
    // Pre-1.20's GuiComponent.blit has no ResourceLocation-taking overload;
    // the texture must be bound separately first (identical shape between
    // 1.18.2 and 1.19.4, confirmed via javap).
    RenderSystem.setShaderTexture(0, texture);
    GuiComponent.blit(pose, x, y, u, v, width, height, texWidth, texHeight);
  }
  *///?} elif >=26.1 {
  /*@Override
  public void blitBackground(Identifier texture, int x, int y, float u, float v, int width,
      int height, int texWidth, int texHeight) {
    // RenderType::guiTextured (the old blit's pipeline-supplier argument)
    // was removed; RenderPipelines.GUI_TEXTURED (confirmed via javap) is
    // the direct replacement RenderPipeline constant.
    context.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height, texWidth,
        texHeight);
  }
  *///?} elif >=1.21 {
  @Override
  public void blitBackground(ResourceLocation texture, int x, int y, float u, float v, int width,
      int height, int texWidth, int texHeight) {
    context.blit(RenderType::guiTextured, texture, x, y, u, v, width, height, texWidth,
        texHeight);
  }
  //?} else {
  /*@Override
  public void blitBackground(ResourceLocation texture, int x, int y, float u, float v, int width,
      int height, int texWidth, int texHeight) {
    context.blit(texture, x, y, u, v, width, height, texWidth, texHeight);
  }
  *///?}

  @Override
  public void renderEntity(float x, float y, float scale, float yRotDegrees, LivingEntity entity) {
    //? if <1.19 {
    /*// 1.18.2 predates the Quaternionf-based camera rewrite entirely and
    // takes no context object at all - rotation is driven by two "mouse
    // offset" floats instead of an explicit quaternion. 0,0 yields the same
    // fixed front-on icon view the mouse-driven variant produces when the
    // mouse hasn't moved off-center, which is what this mod's small
    // non-interactive HUD portrait needs.
    InventoryScreen.renderEntityInInventory((int) x, (int) y, (int) scale, 0.0F, 0.0F, entity);
    *///?} elif <1.20 {
    /*Quaternionf rotation = new Quaternionf()
        .rotationZ((float) Math.PI)
        .rotateY(yRotDegrees * ((float) Math.PI / 180.0f));
    InventoryScreen.renderEntityInInventory(pose, (int) x, (int) y, (int) scale, rotation,
        new Quaternionf(), entity);
    *///?} elif >=26.1 {
    /*// InventoryScreen.renderEntityInInventory is gone entirely in 26.2; the
    // only survivor is extractEntityInInventoryFollowsMouse(
    //   GuiGraphicsExtractor, int x1, int y1, int x2, int y2, int scale,
    //   float yOffset, float mouseX, float mouseY, LivingEntity).
    //
    // An earlier revision of this branch guessed at that signature from
    // javap's parameter *types* alone (which is all javap -p shows), read the
    // three trailing floats as (mouseX, mouseY, partialTick), and treated
    // (x, y) as the top-left corner of a scale-sized square. It compiled, it
    // shipped, and it rendered the portrait displaced and clipped - caught
    // only when the Tier 3 client gametest put a real 26.2 frame on screen.
    // Everything below is now read off the disassembled method body and
    // vanilla's own call site rather than inferred:
    //
    //   centre = ((x1+x2)/2, (y1+y2)/2)
    //   translation = (0, boundingBoxHeight/2 + yOffset, 0)
    //   the same x1,y1,x2,y2 are also the scissor rectangle
    //
    // so the box is not a hint, it positions AND clips. Checked against
    // vanilla's InventoryScreen: box (x+26, y+8)-(x+75, y+78), scale 30, a
    // 1.8-tall player renders 54px, centre y+43, feet land at y+70 just
    // inside the box bottom at y+78.
    //
    // Reproducing the other eras' contract - (x, y) is the feet anchor,
    // horizontally centred - therefore means placing the box so its centre
    // sits half the rendered height above y, and sizing it to the entity
    // rather than to `scale` so the scissor never cuts the model. The bounding
    // box is divided by the entity's own scale because the method does the
    // same to the render state before applying the scale argument.
    float entityScale = entity.getScale() == 0.0f ? 1.0f : entity.getScale();
    float renderedHeight = entity.getBbHeight() / entityScale * scale;
    float renderedWidth = entity.getBbWidth() / entityScale * scale;
    float halfWidth = Math.max(renderedWidth, renderedHeight) / 2.0f + 1.0f;
    int x1 = Math.round(x - halfWidth);
    int x2 = Math.round(x + halfWidth);
    int y1 = Math.round(y - renderedHeight - 1.0f);
    int y2 = Math.round(y + 1.0f);
    // mouseX/mouseY are fed to the method as `atan((centre - mouse) / 40) * 20`
    // degrees of yaw and pitch, so passing 0 - as the earlier revision did -
    // does not mean "no rotation", it means "look at the top-left corner of
    // the screen" and tilts the portrait by however far away that happens to
    // be. Passing the box centre makes both terms exactly zero, which is the
    // front-on icon view the other eras build explicitly with a quaternion.
    // yRotDegrees still cannot be honoured here: 26.2 exposes no fixed
    // rotation parameter, and the pre-26 branches only ever used it for this
    // same front-on portrait anyway.
    InventoryScreen.extractEntityInInventoryFollowsMouse(context, x1, y1, x2, y2,
        Math.round(scale), 0.0f, (x1 + x2) / 2.0f, (y1 + y2) / 2.0f, entity);
    *///?} elif >=1.21 {
    Quaternionf rotation = new Quaternionf()
        .rotationZ((float) Math.PI)
        .rotateY(yRotDegrees * ((float) Math.PI / 180.0f));
    InventoryScreen.renderEntityInInventory(context, x, y, scale,
        new Vector3f(0.0F, 0.0F, 0.0F), rotation, null, entity);
    //?} else {
    /*Quaternionf rotation = new Quaternionf()
        .rotationZ((float) Math.PI)
        .rotateY(yRotDegrees * ((float) Math.PI / 180.0f));
    InventoryScreen.renderEntityInInventory(context, (int) x, (int) y, (int) scale, rotation,
        new Quaternionf(), entity);
    *///?}
  }
}
