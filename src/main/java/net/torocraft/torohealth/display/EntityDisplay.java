package net.torocraft.torohealth.display;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
//? if >=26.1 {
/*// 26.2 moved these two entity classes into new dedicated sub-packages
// (confirmed via javap against the real minecraft-merged-deobf-26.2.jar);
// Ghast stayed put.
import net.minecraft.world.entity.animal.chicken.Chicken;
*///?} else {
import net.minecraft.world.entity.animal.Chicken;
//?}
import net.minecraft.world.entity.monster.Ghast;
//? if >=26.1 {
/*import net.minecraft.world.entity.npc.villager.Villager;
*///?} else {
import net.minecraft.world.entity.npc.Villager;
//?}
import net.torocraft.torohealth.render.HudCanvas;

/**
 * No Stonecutter conditionals here - every version-specific detail of
 * {@code InventoryScreen.renderEntityInInventory}'s four incompatible
 * shapes (1.18.2 / 1.19.4 / 1.20.1 / &gt;=1.21) lives behind
 * {@link HudCanvas#renderEntity} in {@code PlatformHudCanvas} instead.
 */
public class EntityDisplay {

  private static final float RENDER_HEIGHT = 30;
  private static final float RENDER_WIDTH = 18;
  private static final float WIDTH = 40;
  private static final float HEIGHT = WIDTH;

  private LivingEntity entity;
  private int entityScale = 1;

  private float xOffset;
  private float yOffset;

  public void setEntity(LivingEntity entity) {
    this.entity = entity;
    updateScale();
  }

  public void draw(HudCanvas canvas, float scale, float absoluteX, float absoluteY) {
    if (entity != null) {
      try {
        float x = absoluteX + xOffset;
        float y = absoluteY + yOffset;
        // HudCanvas#renderEntity's scale is in the same units as
        // Entity#getScale(), so divide our target render size by it, mirroring
        // InventoryScreen.renderEntityInInventoryFollowsMouse's own math.
        float renderSize = entityScale * scale;
        float entityScaleFactor = renderSize / entity.getScale();

        canvas.renderEntity(x, y, entityScaleFactor, entity.getYRot(), entity);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  public void draw(HudCanvas canvas, float scale) {
    draw(canvas, scale, 0, 0);
  }

  private void updateScale() {
    if (entity == null) {
      return;
    }

    int scaleY = Mth.ceil(RENDER_HEIGHT / entity.getBbHeight());
    int scaleX = Mth.ceil(RENDER_WIDTH / entity.getBbHeight());
    entityScale = Math.min(scaleX, scaleY);

    if (entity instanceof Chicken) {
      entityScale *= 0.7;
    }

    if (entity instanceof Villager && entity.isSleeping()) {
      entityScale = entity.isBaby() ? 31 : 16;
    }

    xOffset = WIDTH / 2 - 1;

    yOffset = HEIGHT / 2 + 15;
    if (entity instanceof Ghast) {
      yOffset -= 10;
    }
  }
}
