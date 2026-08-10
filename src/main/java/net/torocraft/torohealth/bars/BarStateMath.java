package net.torocraft.torohealth.bars;

/**
 * Pure health/damage-delta state machine extracted from {@link BarState} so
 * it can be unit tested with zero Minecraft classes on the compile
 * classpath (see PLAN.md "Phase 2: Test coverage" -- mirrors the sibling
 * mods' FlightComputerMath/GameInfoMath extraction pattern). {@link
 * BarState} stays a thin wrapper: it feeds this class the entity's
 * already-clamped current health each tick and, when {@link #tick(float)}
 * reports a change, is responsible for the one side effect this class
 * deliberately does NOT perform itself -- reading the live particle-enabled
 * config flag and constructing a {@code BarParticle} against the live
 * entity/camera.
 */
public class BarStateMath {

  public static final float HEALTH_INDICATOR_DELAY = 10;

  public float health;
  public float previousHealth;
  public float previousHealthDisplay;
  public float previousHealthDelay;
  public int lastDmg;
  public int lastDmgCumulative;
  public float lastHealth;
  public float lastDmgDelay;
  private float animationSpeed = 0;

  /**
   * Advances the state machine by one tick given the entity's current
   * health (already clamped to at most max health by the caller). Returns
   * {@code true} exactly when a health change was detected this tick (i.e.
   * {@link #lastDmg} was just (re)computed) -- the caller should spawn a
   * damage/heal particle for {@link #lastDmg} in that case, if particles are
   * enabled.
   */
  public boolean tick(float currentHealth) {
    health = currentHealth;
    incrementTimers();

    boolean changed = false;
    if (lastHealth < 0.1) {
      reset();
    } else if (lastHealth != health) {
      handleHealthChange();
      changed = true;
    } else if (lastDmgDelay == 0.0F) {
      reset();
    }

    updateAnimations();
    return changed;
  }

  private void reset() {
    lastHealth = health;
    lastDmg = 0;
    lastDmgCumulative = 0;
  }

  private void incrementTimers() {
    if (this.lastDmgDelay > 0) {
      this.lastDmgDelay--;
    }
    if (this.previousHealthDelay > 0) {
      this.previousHealthDelay--;
    }
  }

  private void handleHealthChange() {
    lastDmg = ceil(lastHealth) - ceil(health);
    lastDmgCumulative += lastDmg;

    lastDmgDelay = HEALTH_INDICATOR_DELAY * 2;
    lastHealth = health;
  }

  private void updateAnimations() {
    if (previousHealthDelay > 0) {
      float diff = previousHealthDisplay - health;
      if (diff > 0) {
        animationSpeed = diff / 10f;
      }
    } else if (previousHealthDelay < 1 && previousHealthDisplay > health) {
      previousHealthDisplay -= animationSpeed;
    } else {
      previousHealthDisplay = health;
      previousHealth = health;
      previousHealthDelay = HEALTH_INDICATOR_DELAY;
    }
  }

  /**
   * Equivalent to {@code net.minecraft.util.Mth.ceil(float)} for the
   * non-negative health values this class ever sees, reimplemented here so
   * this class has zero Minecraft classes on its compile classpath (see
   * GOTCHA cc in PLAN.md).
   */
  private static int ceil(float f) {
    int i = (int) f;
    return f > (float) i ? i + 1 : i;
  }
}
