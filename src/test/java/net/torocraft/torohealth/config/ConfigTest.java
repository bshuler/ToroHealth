package net.torocraft.torohealth.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.torocraft.torohealth.config.Config.AnchorPoint;
import net.torocraft.torohealth.config.Config.Mode;
import net.torocraft.torohealth.config.Config.NumberType;
import org.junit.jupiter.api.Test;

class ConfigTest {

  @Test
  void defaultConstructor_populatesAllNestedSectionsWithTheirOwnDefaults() {
    Config config = new Config();

    assertTrue(config.watchForChanges);
    assertEquals(60, config.hud.distance);
    assertEquals(4f, config.hud.x);
    assertEquals(4f, config.hud.y);
    assertEquals(1f, config.hud.scale);
    assertEquals(20, config.hud.hideDelay);
    assertEquals(AnchorPoint.TOP_LEFT, config.hud.anchorPoint);
    assertTrue(config.hud.showEntity);
    assertTrue(config.hud.showBar);
    assertTrue(config.hud.showSkin);
    assertEquals(false, config.hud.onlyWhenHurt);

    assertTrue(config.particle.show);
    assertEquals(0xff0000, config.particle.damageColor);
    assertEquals(0x00ff00, config.particle.healColor);
    assertEquals(60, config.particle.distance);
    assertEquals(0, config.particle.distanceSquared, "update() has not run yet");

    assertEquals(NumberType.LAST, config.bar.damageNumberType);
    assertEquals(0x00ff00, config.bar.friendColor);
    assertEquals(0x008000, config.bar.friendColorSecondary);
    assertEquals(0xff0000, config.bar.foeColor);
    assertEquals(0x800000, config.bar.foeColorSecondary);

    assertEquals(Mode.NONE, config.inWorld.mode);
    assertEquals(60f, config.inWorld.distance);
    assertEquals(false, config.inWorld.onlyWhenLookingAt);
    assertEquals(false, config.inWorld.onlyWhenHurt);
  }

  @Test
  void update_computesParticleDistanceSquaredFromDistance() {
    Config config = new Config();
    config.particle.distance = 12;

    config.update();

    assertEquals(144, config.particle.distanceSquared);
  }

  @Test
  void update_recomputesDistanceSquaredEachCall() {
    Config config = new Config();

    config.particle.distance = 10;
    config.update();
    assertEquals(100, config.particle.distanceSquared);

    config.particle.distance = 5;
    config.update();
    assertEquals(25, config.particle.distanceSquared);
  }

  @Test
  void shouldWatch_reflectsTheWatchForChangesFlag() {
    Config config = new Config();

    assertTrue(config.shouldWatch());

    config.watchForChanges = false;
    assertEquals(false, config.shouldWatch());
  }

  @Test
  void enums_containExpectedConstantsInDeclarationOrder() {
    assertEquals("[NONE, WHEN_HOLDING_WEAPON, ALWAYS]", java.util.Arrays.toString(Mode.values()));
    assertEquals("[NONE, LAST, CUMULATIVE]", java.util.Arrays.toString(NumberType.values()));
    assertEquals(
        "[TOP_LEFT, TOP_CENTER, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT]",
        java.util.Arrays.toString(AnchorPoint.values()));
  }
}
