package net.torocraft.torohealth.util;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.torocraft.torohealth.ToroHealth;
import net.torocraft.torohealth.config.Config.Mode;

/**
 * Determines whether the player is currently holding something "weapon-like"
 * for {@code Config.InWorld.Mode.WHEN_HOLDING_WEAPON}.
 *
 * <p>Upstream's neoforge-1.21.8 branch checked
 * {@code item.is(Tags.Items.MELEE_WEAPON_TOOLS)}, a NeoForge-registered
 * common item tag that does not exist on Fabric or Forge. This is rewritten
 * as a portable {@code instanceof} check against vanilla item classes so the
 * same logic works unmodified on every loader (see CLAUDE.md).
 */
public class HoldingWeaponUpdater {
  public static void update() {
    if (Mode.NONE.equals(ToroHealth.CONFIG.inWorld.mode)) {
      return;
    }
    Minecraft client = Minecraft.getInstance();
    Player player = client.player;
    if (player == null) {
      ToroHealth.IS_HOLDING_WEAPON = false;
      return;
    }
    ToroHealth.IS_HOLDING_WEAPON =
        isWeapon(player.getMainHandItem()) || isWeapon(player.getOffhandItem());
  }

  private static boolean isWeapon(ItemStack stack) {
    var item = stack.getItem();
    return item instanceof SwordItem
        || item instanceof AxeItem
        || item instanceof TridentItem
        || item instanceof BowItem
        || item instanceof CrossbowItem
        || item instanceof PotionItem;
  }
}
