package net.torocraft.torohealth.display;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.npc.Villager;
import org.joml.Quaternionf;
import org.joml.Vector3f;

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

  public void draw(GuiGraphics guiGraphics, float scale, float absoluteX, float absoluteY) {
    if (entity != null) {
      try {
        float x = absoluteX + xOffset;
        float y = absoluteY + yOffset;
        // MC 1.21.4's InventoryScreen.renderEntityInInventory takes a center
        // point + a single scale factor (not a bounding box + separate size
        // like older/newer MC releases) - the scale is in the same units as
        // Entity#getScale(), so divide our target render size by it, mirroring
        // InventoryScreen.renderEntityInInventoryFollowsMouse's own math.
        float renderSize = entityScale * scale;
        float entityScaleFactor = renderSize / entity.getScale();

        Vector3f translation = new Vector3f(0.0F, 0.0F, 0.0F);
        Quaternionf rotation = new Quaternionf()
            .rotationZ((float) Math.PI)
            .rotateY(entity.getYRot() * ((float) Math.PI / 180.0f));

        InventoryScreen.renderEntityInInventory(
            guiGraphics,
            x, y,
            entityScaleFactor,
            translation,
            rotation,
            null,
            entity
        );

      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  public void draw(GuiGraphics guiGraphics, float scale) {
    draw(guiGraphics, scale, 0, 0);
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
