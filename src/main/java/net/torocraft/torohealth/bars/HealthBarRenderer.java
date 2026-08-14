package net.torocraft.torohealth.bars;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
//? if >=26.1 {
/*import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
*///?} else {
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
//?}
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.torocraft.torohealth.ToroHealth;
import net.torocraft.torohealth.config.Config.Bar;
import net.torocraft.torohealth.config.Config.InWorld;
import net.torocraft.torohealth.config.Config.Mode;
import net.torocraft.torohealth.util.Colors;
import net.torocraft.torohealth.util.EntityUtil;
import net.torocraft.torohealth.util.EntityUtil.Relation;
//? if <1.19 {
/*import com.mojang.math.Matrix4f;
*///?} else {
import org.joml.Matrix4f;
//?}

public class HealthBarRenderer {

  private static InWorld getConfig() {
    return ToroHealth.CONFIG.inWorld;
  }

  private static final List<LivingEntity> renderedEntities = new ArrayList<>();

  public static void prepareRenderInWorld(LivingEntity entity) {
    Minecraft client = Minecraft.getInstance();

    if (!EntityUtil.showHealthBar(entity, client)) {
      return;
    }

    Entity cameraEntity = client.getCameraEntity();
    if (cameraEntity == null) {
      return;
    }
    if (entity.distanceTo(cameraEntity) > ToroHealth.CONFIG.inWorld.distance) {
      return;
    }

    BarStates.getState(entity);

    if (Mode.WHEN_HOLDING_WEAPON.equals(getConfig().mode) && !ToroHealth.IS_HOLDING_WEAPON) {
      return;
    }

    if (Mode.NONE.equals(getConfig().mode)) {
      return;
    }

    if (ToroHealth.CONFIG.inWorld.onlyWhenLookingAt && ToroHealth.HUD.getEntity() != entity) {
      return;
    }

    if (ToroHealth.CONFIG.inWorld.onlyWhenHurt && entity.getHealth() >= entity.getMaxHealth()) {
      return;
    }

    renderedEntities.add(entity);
  }

  //? if >=26.1 {
  /*public static void renderInWorld(float partialTick, PoseStack poseStack, Camera camera,
      SubmitNodeCollector submitNodeCollector) {
    if (renderedEntities.isEmpty()) {
      return;
    }

    Vec3 cameraPos = camera.position();

    for (LivingEntity entity : renderedEntities) {
      if (entity == null || !entity.isAlive()) {
        continue;
      }

      Vec3 entityPos = entity.position().add(0, entity.getBbHeight() + 0.5, 0);
      Vec3 offset = entityPos.subtract(cameraPos);

      poseStack.pushPose();
      poseStack.translate(offset.x, offset.y, offset.z);
      poseStack.mulPose(camera.rotation());

      // 26.2 replaced immediate MultiBufferSource-based rendering with a
      // two-phase "record a geometry callback now, draw it later" submit-node
      // model (confirmed via javap: SubmitNodeCollector has no
      // getBuffer()/endBatch() survivor at all). submitCustomGeometry snapshots
      // the current PoseStack.Pose and invokes the supplied
      // CustomGeometryRenderer with (PoseStack.Pose, VertexConsumer) once it
      // actually draws, so the pushPose/translate/mulPose above still
      // positions the geometry exactly like the pre-26.1 branch below.
      submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.lines(),
          (pose, vertexConsumer) -> renderHealthBar(pose.pose(), vertexConsumer, entity));

      poseStack.popPose();
    }

    renderedEntities.clear();
  }
  *///?} else {
  public static void renderInWorld(float partialTick, PoseStack poseStack, Camera camera) {
    if (renderedEntities.isEmpty()) {
      return;
    }

    Minecraft client = Minecraft.getInstance();
    MultiBufferSource.BufferSource bufferSource = client.renderBuffers().bufferSource();
    VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.lines());

    Vec3 cameraPos = camera.getPosition();

    for (LivingEntity entity : renderedEntities) {
      if (entity == null || !entity.isAlive()) {
        continue;
      }

      Vec3 entityPos = entity.position().add(0, entity.getBbHeight() + 0.5, 0);
      Vec3 offset = entityPos.subtract(cameraPos);

      poseStack.pushPose();
      poseStack.translate(offset.x, offset.y, offset.z);
      poseStack.mulPose(camera.rotation());

      renderHealthBar(poseStack.last().pose(), vertexConsumer, entity);

      poseStack.popPose();
    }

    bufferSource.endBatch(RenderType.lines());
    renderedEntities.clear();
  }
  //?}

  /**
   * Draws the three stacked layers upstream drew: a dark background, the
   * "ghost" bar trailing at the entity's previous displayed health in the
   * secondary colour, and the current health in the primary colour. Which
   * pair of colours is used depends on {@link EntityUtil#determineRelation},
   * exactly as upstream's {@code HealthBarRenderer#render} did.
   *
   * <p>This restores four config options - {@code bar.friendColor},
   * {@code bar.friendColorSecondary}, {@code bar.foeColor} and
   * {@code bar.foeColorSecondary} - that this fork's port had left wired to
   * nothing at all: the ported renderer computed a fixed red-to-green health
   * ramp and never read them, so the config file, the config screen and the
   * lang files all offered four colour settings that could not change a
   * single pixel. See PLAN.md "Restored: friend/foe bar colours".
   *
   * <p>The colours come out of config as {@code 0xRRGGBB} (ColorJsonAdapter
   * masks the alpha byte off on read so that read(write(x)) round-trips), so
   * they are unpacked through {@link Colors}, whose {@code alpha} applies the
   * no-alpha-means-opaque fixup first. Unpacking the alpha naively here would
   * make every in-world bar fully transparent on every version, which is the
   * same class of bug as the 26.x text one - just self-inflicted.
   */
  private static void renderHealthBar(Matrix4f matrix, VertexConsumer vertexConsumer,
      LivingEntity entity) {
    float health = entity.getHealth();
    float maxHealth = entity.getMaxHealth();
    float healthPercent = Mth.clamp(health / maxHealth, 0.0f, 1.0f);
    float previousPercent =
        Mth.clamp(BarStates.getState(entity).math.previousHealthDisplay / maxHealth, 0.0f, 1.0f);

    boolean friend = EntityUtil.determineRelation(entity) == Relation.FRIEND;
    Bar barConfig = ToroHealth.CONFIG.bar;
    int primary = friend ? barConfig.friendColor : barConfig.foeColor;
    int secondary = friend ? barConfig.friendColorSecondary : barConfig.foeColorSecondary;

    float barWidth = 1.0f;
    float barHeight = 0.1f;

    drawQuad(matrix, vertexConsumer, -barWidth / 2, -barHeight / 2, barWidth, barHeight, 0.2f,
        0.2f, 0.2f, 1.0f);

    if (previousPercent > healthPercent) {
      drawQuad(matrix, vertexConsumer, -barWidth / 2, -barHeight / 2, barWidth * previousPercent,
          barHeight, Colors.red(secondary), Colors.green(secondary), Colors.blue(secondary),
          Colors.alpha(secondary));
    }

    if (healthPercent > 0) {
      drawQuad(matrix, vertexConsumer, -barWidth / 2, -barHeight / 2, barWidth * healthPercent,
          barHeight, Colors.red(primary), Colors.green(primary), Colors.blue(primary),
          Colors.alpha(primary));
    }
  }

  private static void drawQuad(Matrix4f matrix, VertexConsumer vertexConsumer, float x, float y,
      float width, float height, float red, float green, float blue, float alpha) {
    float x2 = x + width;
    float y2 = y + height;

    //? if >=1.21 {
    vertexConsumer.addVertex(matrix, x, y, 0).setColor(red, green, blue, alpha);
    vertexConsumer.addVertex(matrix, x2, y, 0).setColor(red, green, blue, alpha);

    vertexConsumer.addVertex(matrix, x2, y, 0).setColor(red, green, blue, alpha);
    vertexConsumer.addVertex(matrix, x2, y2, 0).setColor(red, green, blue, alpha);

    vertexConsumer.addVertex(matrix, x2, y2, 0).setColor(red, green, blue, alpha);
    vertexConsumer.addVertex(matrix, x, y2, 0).setColor(red, green, blue, alpha);

    vertexConsumer.addVertex(matrix, x, y2, 0).setColor(red, green, blue, alpha);
    vertexConsumer.addVertex(matrix, x, y, 0).setColor(red, green, blue, alpha);
    //?} else {
    /*vertexConsumer.vertex(matrix, x, y, 0).color(red, green, blue, alpha).endVertex();
    vertexConsumer.vertex(matrix, x2, y, 0).color(red, green, blue, alpha).endVertex();

    vertexConsumer.vertex(matrix, x2, y, 0).color(red, green, blue, alpha).endVertex();
    vertexConsumer.vertex(matrix, x2, y2, 0).color(red, green, blue, alpha).endVertex();

    vertexConsumer.vertex(matrix, x2, y2, 0).color(red, green, blue, alpha).endVertex();
    vertexConsumer.vertex(matrix, x, y2, 0).color(red, green, blue, alpha).endVertex();

    vertexConsumer.vertex(matrix, x, y2, 0).color(red, green, blue, alpha).endVertex();
    vertexConsumer.vertex(matrix, x, y, 0).color(red, green, blue, alpha).endVertex();
    *///?}
  }

}
