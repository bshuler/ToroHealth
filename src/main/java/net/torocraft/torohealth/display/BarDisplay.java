package net.torocraft.torohealth.display;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.torocraft.torohealth.render.HudCanvas;

/**
 * No Stonecutter conditionals here - drawing goes entirely through
 * {@link HudCanvas}, whose one implementation ({@code PlatformHudCanvas})
 * absorbs every version-specific {@code fill}/{@code drawString} shape.
 */
public class BarDisplay {

  private String getEntityName(LivingEntity entity) {
    return entity.getDisplayName().getString();
  }

  public void draw(HudCanvas canvas, LivingEntity entity) {
    int xOffset = 0;

    drawHealthBar(canvas, entity, 1, 14, 130);

    String name = getEntityName(entity);
    int healthMax = Mth.ceil(entity.getMaxHealth());
    int healthCur = Math.min(Mth.ceil(entity.getHealth()), healthMax);
    String healthText = healthCur + "/" + healthMax;

    Minecraft client = Minecraft.getInstance();
    canvas.drawString(client.font, name, xOffset, 2, 0xFFFFFFFF, false);
    xOffset += client.font.width(name) + 5;

    renderHeartIcon(canvas, xOffset, 1);
    xOffset += 10;

    canvas.drawString(client.font, healthText, xOffset, 2, 0xFFFFFFFF, false);
    xOffset += client.font.width(healthText) + 5;

    int armor = entity.getArmorValue();
    if (armor > 0) {
      renderArmorIcon(canvas, xOffset, 1);
      xOffset += 10;
      canvas.drawString(client.font, armor + "", xOffset, 2, 0xFFFFFFFF, false);
    }
  }

  private void drawHealthBar(HudCanvas canvas, LivingEntity entity, int x, int y, int width) {
    float health = entity.getHealth();
    float maxHealth = entity.getMaxHealth();
    float healthPercent = health / maxHealth;

    int barWidth = (int) (width * healthPercent);

    canvas.fill(x, y, x + width, y + 4, 0xFF555555);

    int color = healthPercent > 0.5f ? 0xFF00FF00 : (healthPercent > 0.25f ? 0xFFFFFF00 : 0xFFFF0000);
    canvas.fill(x, y, x + barWidth, y + 4, color);
  }

  private void renderArmorIcon(HudCanvas canvas, int x, int y) {
    int color = 0xFFC0C0C0;

    canvas.fill(x + 2, y, x + 6, y + 1, color);
    canvas.fill(x + 1, y + 1, x + 7, y + 6, color);
    canvas.fill(x + 2, y + 6, x + 6, y + 7, color);
    canvas.fill(x + 3, y + 7, x + 5, y + 8, color);
  }

  private void renderHeartIcon(HudCanvas canvas, int x, int y) {
    int color = 0xFFFF0000;

    canvas.fill(x + 1, y + 1, x + 3, y + 3, color);
    canvas.fill(x + 5, y + 1, x + 7, y + 3, color);

    canvas.fill(x, y + 2, x + 8, y + 5, color);

    canvas.fill(x + 1, y + 5, x + 7, y + 6, color);
    canvas.fill(x + 2, y + 6, x + 6, y + 7, color);
    canvas.fill(x + 3, y + 7, x + 5, y + 8, color);
    canvas.fill(x + 4, y + 8, x + 4, y + 9, color);
  }
}
