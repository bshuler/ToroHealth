package net.torocraft.torohealth.bars;

import net.minecraft.world.entity.LivingEntity;
import net.torocraft.torohealth.ToroHealth;

/**
 * Thin wrapper feeding the live entity's health into {@link BarStateMath}
 * each tick and performing the one side effect that class deliberately
 * excludes: reading the live particle-enabled config flag and constructing
 * a {@link BarParticle} against the live entity/camera. See
 * {@link BarStateMath}'s class doc and PLAN.md ("Phase 2: Test coverage")
 * for why the split exists -- this class touches
 * {@code Minecraft}/{@code LivingEntity} state and is excluded from JaCoCo;
 * {@link BarStateMath} is fully unit tested.
 */
public class BarState {

  public final LivingEntity entity;
  public final BarStateMath math = new BarStateMath();

  public BarState(LivingEntity entity) {
    this.entity = entity;
  }

  public void tick() {
    float clampedHealth = Math.min(entity.getHealth(), entity.getMaxHealth());
    boolean changed = math.tick(clampedHealth);
    if (changed && ToroHealth.CONFIG.particle.show) {
      BarStates.PARTICLES.add(new BarParticle(entity, math.lastDmg));
    }
  }

}
