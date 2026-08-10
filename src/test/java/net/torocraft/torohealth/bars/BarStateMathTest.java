package net.torocraft.torohealth.bars;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Exercises {@link BarStateMath} -- the pure health/damage-delta state
 * machine extracted from {@link BarState} -- by driving whole tick()
 * sequences and asserting the exact numbers the algorithm should produce at
 * each step (independently verified with a Python re-implementation before
 * writing these assertions). {@link BarState} itself is excluded from
 * coverage since it touches a live {@code LivingEntity}/{@code BarStates}.
 */
class BarStateMathTest {

  @Test
  void tick_firstCallResetsAndSnapsPreviousHealthDisplayToCurrentHealth() {
    BarStateMath math = new BarStateMath();

    boolean changed = math.tick(20f);

    assertFalse(changed, "first tick should reset(), not report a health change");
    assertEquals(20f, math.lastHealth);
    assertEquals(0, math.lastDmg);
    assertEquals(0, math.lastDmgCumulative);
    assertEquals(20f, math.previousHealthDisplay);
    assertEquals(20f, math.previousHealth);
    assertEquals(BarStateMath.HEALTH_INDICATOR_DELAY, math.previousHealthDelay);
  }

  @Test
  void tick_healthDecrease_reportsChangeAndComputesPositiveDamage() {
    BarStateMath math = new BarStateMath();
    math.tick(20f);

    boolean changed = math.tick(12f);

    assertTrue(changed, "a health decrease must be reported as a change");
    assertEquals(8, math.lastDmg);
    assertEquals(8, math.lastDmgCumulative);
    assertEquals(12f, math.lastHealth);
    assertEquals(BarStateMath.HEALTH_INDICATOR_DELAY * 2, math.lastDmgDelay);
  }

  @Test
  void tick_healthIncrease_heal_reportsChangeAndComputesNegativeDamage() {
    BarStateMath math = new BarStateMath();
    math.tick(10f);

    boolean changed = math.tick(18f);

    assertTrue(changed, "a heal must also be reported as a change");
    assertEquals(-8, math.lastDmg);
    assertEquals(-8, math.lastDmgCumulative);
    assertEquals(18f, math.lastHealth);
  }

  @Test
  void tick_damageCumulativeAccumulatesAcrossConsecutiveHits() {
    BarStateMath math = new BarStateMath();
    math.tick(20f);
    math.tick(15f); // lastDmg = ceil(20)-ceil(15) = 5

    assertEquals(5, math.lastDmg);
    assertEquals(5, math.lastDmgCumulative);

    math.tick(11f); // lastDmg = ceil(15)-ceil(11) = 4

    assertEquals(4, math.lastDmg);
    assertEquals(9, math.lastDmgCumulative, "cumulative should add the new hit to the running total");
  }

  @Test
  void tick_noChangeWhileDamageDelayStillCounting_doesNotReportChangeOrReset() {
    BarStateMath math = new BarStateMath();
    math.tick(20f);
    math.tick(12f); // damage: lastDmgDelay = 20

    boolean changed = math.tick(12f); // same health, delay not yet expired

    assertFalse(changed);
    assertEquals(8, math.lastDmg, "lastDmg from the prior hit should be untouched while delay counts down");
    assertEquals(19, math.lastDmgDelay, "incrementTimers() should have decremented the delay by exactly one");
  }

  @Test
  void tick_noChangeAfterDamageDelayExpires_resetsWithoutTreatingItAsAChange() {
    BarStateMath math = new BarStateMath();
    math.tick(20f);
    math.tick(12f); // lastDmgDelay = 20

    // Tick with unchanged health until lastDmgDelay counts down to exactly 0.
    boolean changed = false;
    for (int i = 0; i < 20; i++) {
      changed = math.tick(12f);
    }

    assertFalse(changed, "the reset tick itself must not be reported as a health change");
    assertEquals(0, math.lastDmg, "reset() clears lastDmg once the delay expires with no new hit");
    assertEquals(0, math.lastDmgCumulative);
    assertEquals(12f, math.lastHealth);
  }

  @Test
  void tick_previousHealthDisplayDecaysTowardCurrentHealthAfterItsOwnDelayExpires() {
    BarStateMath math = new BarStateMath();
    math.tick(20f); // previousHealthDisplay = 20, previousHealthDelay = 10
    math.tick(12f); // damage: animationSpeed set to (20-12)/10 = 0.8, previousHealthDelay -> 9

    // 8 more same-health ticks count previousHealthDelay down from 9 to 1;
    // while it's still > 0, updateAnimations() keeps recomputing (the same)
    // animationSpeed and leaves previousHealthDisplay untouched.
    for (int i = 0; i < 8; i++) {
      math.tick(12f);
    }
    assertEquals(1f, math.previousHealthDelay);
    assertEquals(20f, math.previousHealthDisplay);

    // The tick where previousHealthDelay counts down to exactly 0 already
    // applies the first decay step in the same call (verified against an
    // independent re-implementation before writing this assertion).
    math.tick(12f);
    assertEquals(0f, math.previousHealthDelay);
    assertEquals(19.2f, math.previousHealthDisplay, 1e-4f);

    math.tick(12f);
    assertEquals(18.4f, math.previousHealthDisplay, 1e-4f);
  }

  @Test
  void tick_previousHealthSnapsToCurrentHealthOnceDisplayCatchesUp() {
    BarStateMath math = new BarStateMath();
    math.tick(20f);
    math.tick(12f); // animationSpeed = 0.8

    // 18 more same-health ticks decay previousHealthDisplay from 20 toward
    // 12 in 0.8 steps, without yet snapping: previousHealth is still 20 and
    // previousHealthDelay is still the expired 0, not the reset value of 10.
    // Verified by compiling and running BarStateMath standalone (real JVM
    // float32 arithmetic, not a hand re-implementation): repeated float
    // subtraction of 0.8 leaves previousHealthDisplay at 12.000003f here,
    // fractionally *above* 12 rather than landing exactly on it.
    for (int i = 0; i < 18; i++) {
      math.tick(12f);
    }

    assertEquals(12f, math.previousHealthDisplay, 1e-4f);
    assertEquals(20f, math.previousHealth, "should not have snapped yet");
    assertEquals(0f, math.previousHealthDelay);

    // Because previousHealthDisplay (12.000003f) is still fractionally
    // *greater than* health (12f), updateAnimations() takes one more decay
    // step instead of snapping yet -- confirmed the same way, against a real
    // run rather than assumed from double-precision math.
    math.tick(12f);

    assertEquals(11.2f, math.previousHealthDisplay, 1e-4f);
    assertEquals(20f, math.previousHealth, "still not snapped: one extra decay step from float accumulation");
    assertEquals(0f, math.previousHealthDelay);

    // Now previousHealthDisplay (11.200003f) is no longer greater than
    // health (12f), so updateAnimations() takes its "snap to current"
    // else-branch, refreshing previousHealth/previousHealthDelay.
    math.tick(12f);

    assertEquals(12f, math.previousHealthDisplay, 1e-4f);
    assertEquals(12f, math.previousHealth);
    assertEquals(BarStateMath.HEALTH_INDICATOR_DELAY, math.previousHealthDelay);
  }

  @Test
  void tick_diffNotPositive_leavesPreviousHealthDisplayUnchanged() {
    BarStateMath math = new BarStateMath();
    math.tick(12f);
    // Heal so previousHealthDisplay (12) is not greater than the new health
    // (20): diff = 12 - 20 = -8, so the `diff > 0` branch inside
    // updateAnimations() must not fire, and previousHealthDisplay is left
    // exactly as-is rather than decaying or snapping.
    math.tick(20f);

    assertEquals(12f, math.previousHealthDisplay);

    float displayBefore = math.previousHealthDisplay;
    math.tick(20f);

    assertEquals(displayBefore, math.previousHealthDisplay,
        "with diff <= 0, previousHealthDisplay must not move");
  }
}
