package net.torocraft.torohealth.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import net.torocraft.torohealth.config.Config;

/**
 * {@link Colors} is in the enforced 100%-coverage bundle (it is pure - no
 * Minecraft, no loader, no live entity), so these cases are its full truth
 * table rather than a spot check.
 *
 * <p>Getting {@code opaqueIfNoAlpha} wrong does not produce a wrong shade, it
 * produces an <em>invisible</em> HUD on 26.x while every other signal stays
 * green - see {@link Colors}' class doc and PLAN.md "Tier 3".
 */
class ColorsTest {

  @Test
  void constructor_isReachable() {
    // Colors has no explicit constructor, so javac generates an implicit
    // public one and JaCoCo attributes it to the class declaration line.
    // Nothing in production instantiates the class (all methods are static),
    // so this exists purely to cover that synthetic constructor.
    assertNotNull(new Colors());
  }

  @Test
  void opaqueIfNoAlpha_promotesBareRgbToOpaque() {
    assertEquals(0xFF00FF00, Colors.opaqueIfNoAlpha(0x00FF00));
    assertEquals(0xFFFFFFFF, Colors.opaqueIfNoAlpha(0xFFFFFF));
  }

  @Test
  void opaqueIfNoAlpha_leavesRealAlphaAlone() {
    // A genuinely half-transparent colour must survive untouched, otherwise a
    // user asking for a faded bar silently gets a solid one.
    assertEquals(0x80123456, Colors.opaqueIfNoAlpha(0x80123456));
    assertEquals(0xFF123456, Colors.opaqueIfNoAlpha(0xFF123456));
  }

  @Test
  void opaqueIfNoAlpha_usesVanillasOwnThreshold() {
    // Vanilla masked with 0xFC000000, not 0xFF000000: alpha bytes 1-3 read as
    // "no alpha given" and get promoted, 4 is the first that counts.
    assertEquals(0xFF000000, Colors.opaqueIfNoAlpha(0x03000000));
    assertEquals(0x04000000, Colors.opaqueIfNoAlpha(0x04000000));
  }

  @Test
  void opaqueIfNoAlpha_promotesBlack() {
    assertEquals(0xFF000000, Colors.opaqueIfNoAlpha(0x00000000));
  }

  @Test
  void channels_unpackArgb() {
    assertEquals(1.0f, Colors.red(0x00FF0000), 0.0001f);
    assertEquals(1.0f, Colors.green(0x0000FF00), 0.0001f);
    assertEquals(1.0f, Colors.blue(0x000000FF), 0.0001f);
    assertEquals(0.0f, Colors.red(0x0000FFFF), 0.0001f);
    assertEquals(0.0f, Colors.green(0x00FF00FF), 0.0001f);
    assertEquals(0.0f, Colors.blue(0x00FFFF00), 0.0001f);
    assertEquals(0x80 / 255.0f, Colors.red(0x00800000), 0.0001f);
  }

  @Test
  void alpha_treatsTheConfigsRgbFormAsOpaque() {
    // ColorJsonAdapter masks the alpha byte off on read, so every colour that
    // reaches a renderer from config is 0x00RRGGBB. Unpacking that naively
    // gives alpha 0 and draws nothing at all.
    assertEquals(1.0f, Colors.alpha(0x0000FF00), 0.0001f);
    assertEquals(1.0f, Colors.alpha(0xFF00FF00), 0.0001f);
    assertEquals(0x80 / 255.0f, Colors.alpha(0x8000FF00), 0.0001f);
  }

  @Test
  void configDefaults_allSurviveTheAlphaFixup() {
    // The six bar/particle colours ship as 0xRRGGBB with a zero alpha byte.
    // Each one must come out of the fixup opaque and otherwise unchanged;
    // this is the exact set that reaches a draw call.
    Config config = new Config();
    for (int color : new int[] {config.bar.friendColor, config.bar.friendColorSecondary,
        config.bar.foeColor, config.bar.foeColorSecondary, config.particle.damageColor,
        config.particle.healColor}) {
      assertEquals(0xFF000000 | color, Colors.opaqueIfNoAlpha(color));
      assertEquals(1.0f, Colors.alpha(color), 0.0001f);
    }
  }
}
