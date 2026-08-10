package net.torocraft.torohealth.bars;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
//? if >=26.1 {
/*import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
*///?} else {
import net.minecraft.client.renderer.MultiBufferSource;
//?}
import net.minecraft.world.phys.Vec3;
import net.torocraft.torohealth.ToroHealth;

public class ParticleRenderer {

  //? if >=26.1 {
  /*public static void renderParticles(PoseStack poseStack, Camera camera,
      SubmitNodeCollector submitNodeCollector) {
    if (BarStates.PARTICLES.isEmpty()) {
      return;
    }

    for (BarParticle particle : BarStates.PARTICLES) {
      renderParticle(poseStack, submitNodeCollector, particle, camera);
    }
  }
  *///?} else {
  public static void renderParticles(PoseStack poseStack, Camera camera) {
    if (BarStates.PARTICLES.isEmpty()) {
      return;
    }

    Minecraft client = Minecraft.getInstance();
    MultiBufferSource.BufferSource bufferSource = client.renderBuffers().bufferSource();

    for (BarParticle particle : BarStates.PARTICLES) {
      renderParticle(poseStack, bufferSource, particle, camera);
    }

    try {
      bufferSource.endBatch();
    } catch (Exception e) {
      System.err.println("ToroHealth: particle buffer flush failed: " + e.getMessage());
    }
  }
  //?}

  //? if >=26.1 {
  /*private static void renderParticle(PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
      BarParticle particle, Camera camera) {
    Vec3 cameraPos = camera.position();
    double distanceSquared = cameraPos.distanceToSqr(particle.x, particle.y, particle.z);
  *///?} else {
  private static void renderParticle(PoseStack poseStack, MultiBufferSource bufferSource,
      BarParticle particle, Camera camera) {
    Vec3 cameraPos = camera.getPosition();
    double distanceSquared = cameraPos.distanceToSqr(particle.x, particle.y, particle.z);
  //?}

    if (distanceSquared > ToroHealth.CONFIG.particle.distanceSquared) {
      return;
    }

    Vec3 particlePos = new Vec3(particle.x, particle.y, particle.z);
    Vec3 offset = particlePos.subtract(cameraPos);

    poseStack.pushPose();
    poseStack.translate(offset.x, offset.y, offset.z);

    float scale = 0.035f;
    poseStack.scale(-scale, -scale, scale);

    String damageText = String.valueOf(Math.abs(particle.damage));
    Minecraft client = Minecraft.getInstance();
    Font font = client.font;

    int textWidth = font.width(damageText);
    float textX = -textWidth / 2.0f;
    float textY = 0.0f;

    int color = particle.damage < 0 ? 0xFF00FF00 : 0xFFFF0000;

    //? if <1.19 {
    /*// 1.18.2's Font.drawInBatch takes a plain boolean ("seeThrough") in
    // place of the >=1.19 Font.DisplayMode enum used below - there is no
    // NORMAL/SEE_THROUGH distinction to fall back between, so this is a
    // single call rather than the try/fallback pair.
    try {
      font.drawInBatch(damageText, textX, textY, color, false, poseStack.last().pose(),
          bufferSource, true, 0, 15728880);
    } catch (Exception e) {
      System.err.println("ToroHealth: particle text rendering failed: " + e.getMessage());
    }
    *///?} elif >=26.1 {
    /*// Font.drawInBatch is gone entirely in 26.2 (confirmed via javap - no
    // survivor on Font at all); the replacement is
    // SubmitNodeCollector.submitText(PoseStack, float x, float y,
    // FormattedCharSequence, boolean dropShadow, Font.DisplayMode,
    // int packedLight, int color, int, int) - argument order and the two
    // trailing ints confirmed by disassembling a real vanilla caller
    // (AbstractSignRenderer.submitSignText, javap -c) since Font/
    // SubmitNodeCollector's own javap output only lists parameter types, not
    // names. The two trailing ints are an outline/background pair that
    // vanilla only uses for sign text; this mod's floating damage numbers
    // don't need an outline, so both are 0. Unlike the old immediate-mode
    // drawInBatch, submitText only records the draw for later and is not
    // expected to throw, but the try/catch is kept for parity with every
    // other era's defensive handling here.
    try {
      submitNodeCollector.submitText(poseStack, textX, textY,
          FormattedCharSequence.forward(damageText, Style.EMPTY), false,
          Font.DisplayMode.SEE_THROUGH, 15728880, color, 0, 0);
    } catch (Exception e) {
      System.err.println("ToroHealth: particle text rendering failed: " + e.getMessage());
    }
    *///?} else {
    try {
      font.drawInBatch(damageText, textX, textY, color, false, poseStack.last().pose(),
          bufferSource, Font.DisplayMode.SEE_THROUGH, 0, 15728880);
    } catch (Exception e) {
      try {
        font.drawInBatch(damageText, textX, textY, color, false, poseStack.last().pose(),
            bufferSource, Font.DisplayMode.NORMAL, 0, 15728880);
      } catch (Exception e2) {
        System.err.println("ToroHealth: particle text rendering failed: " + e2.getMessage());
      }
    }
    //?}

    poseStack.popPose();
  }
}
