package net.torocraft.torohealth.display;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.torocraft.torohealth.bars.BarStates;
import net.torocraft.torohealth.config.Config.Bar;
import net.torocraft.torohealth.render.HudCanvas;
import net.torocraft.torohealth.util.Colors;
import net.torocraft.torohealth.util.EntityUtil;
import net.torocraft.torohealth.util.EntityUtil.Relation;

/**
 * No Stonecutter conditionals here - drawing goes entirely through
 * {@link HudCanvas}, whose one implementation ({@code PlatformHudCanvas})
 * absorbs every version-specific {@code fill}/{@code drawString} shape.
 */
public class BarDisplay {

  private String getEntityName(LivingEntity entity) {
    return entity.getDisplayName().getString();
  }

  public void draw(HudCanvas canvas, LivingEntity entity, Bar barConfig) {
    int xOffset = 0;

    drawHealthBar(canvas, entity, barConfig, 1, 14, 130);

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

  /**
   * Background, then the trailing "ghost" bar at the entity's previous
   * displayed health in the secondary colour, then current health in the
   * primary colour - the same three layers, from the same four config
   * options, as the in-world bar in {@code HealthBarRenderer}. Upstream drew
   * both bars through one method and one pair of colours; this fork's port
   * split them and left the HUD bar on a hardcoded green/yellow/red ramp that
   * ignored the config entirely. See PLAN.md "Restored: friend/foe bar
   * colours".
   *
   * <p>{@link Colors#opaqueIfNoAlpha} is required, not decorative: config
   * colours arrive as {@code 0xRRGGBB} with a zero alpha byte, and
   * {@code fill} has honoured alpha on every Minecraft version - so passing
   * one straight through draws nothing anywhere.
   */
  private void drawHealthBar(HudCanvas canvas, LivingEntity entity, Bar barConfig, int x, int y,
      int width) {
    float maxHealth = entity.getMaxHealth();
    float healthPercent = Mth.clamp(entity.getHealth() / maxHealth, 0.0f, 1.0f);
    float previousPercent =
        Mth.clamp(BarStates.getState(entity).math.previousHealthDisplay / maxHealth, 0.0f, 1.0f);

    boolean friend = EntityUtil.determineRelation(entity) == Relation.FRIEND;
    int primary = Colors.opaqueIfNoAlpha(friend ? barConfig.friendColor : barConfig.foeColor);
    int secondary = Colors
        .opaqueIfNoAlpha(friend ? barConfig.friendColorSecondary : barConfig.foeColorSecondary);

    canvas.fill(x, y, x + width, y + 4, 0xFF555555);

    if (previousPercent > healthPercent) {
      canvas.fill(x, y, x + (int) (width * previousPercent), y + 4, secondary);
    }

    canvas.fill(x, y, x + (int) (width * healthPercent), y + 4, primary);
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
