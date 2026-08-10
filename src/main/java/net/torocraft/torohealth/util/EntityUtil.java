package net.torocraft.torohealth.util;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.player.Player;

public class EntityUtil {

  public enum Relation {
    FRIEND, FOE, UNKNOWN
  }

  public static Relation determineRelation(Entity entity) {
    if (entity instanceof Monster) {
      return Relation.FOE;
    } else if (entity instanceof Slime) {
      return Relation.FOE;
    } else if (entity instanceof Ghast) {
      return Relation.FOE;
    } else if (entity instanceof Animal) {
      return Relation.FRIEND;
    } else if (entity instanceof Squid) {
      return Relation.FRIEND;
    } else if (entity instanceof AmbientCreature) {
      return Relation.FRIEND;
    } else if (entity instanceof AgeableMob) {
      return Relation.FRIEND;
    } else if (entity instanceof AbstractFish) {
      return Relation.FRIEND;
    } else {
      return Relation.UNKNOWN;
    }
  }

  public static boolean showHealthBar(Entity entity, Minecraft client) {
    Player player = client.player;
    if (player == null) {
      return false;
    }

    return entity instanceof LivingEntity && !(entity instanceof ArmorStand)
        && (!entity.isInvisibleTo(player) || entity.isCurrentlyGlowing() || entity.isOnFire()
            || entity instanceof Creeper creeper && creeper.isPowered()
            || (entity instanceof LivingEntity livingEntity && hasEquipment(livingEntity)))
        && entity != player && !entity.isSpectator();
  }

  private static boolean hasEquipment(LivingEntity entity) {
    return !entity.getMainHandItem().isEmpty()
        || !entity.getOffhandItem().isEmpty()
        || !entity.getItemBySlot(EquipmentSlot.HEAD).isEmpty()
        || !entity.getItemBySlot(EquipmentSlot.CHEST).isEmpty()
        || !entity.getItemBySlot(EquipmentSlot.LEGS).isEmpty()
        || !entity.getItemBySlot(EquipmentSlot.FEET).isEmpty();
  }
}
