package net.torocraft.torohealth.bars;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import net.torocraft.torohealth.ToroHealth;

public class ParticleRenderer {

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

  private static void renderParticle(PoseStack poseStack, MultiBufferSource bufferSource,
      BarParticle particle, Camera camera) {
    Vec3 cameraPos = camera.getPosition();
    double distanceSquared = cameraPos.distanceToSqr(particle.x, particle.y, particle.z);

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

    poseStack.popPose();
  }
}
