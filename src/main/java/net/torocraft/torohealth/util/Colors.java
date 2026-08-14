package net.torocraft.torohealth.util;

/**
 * Colour helpers for the one trap that separates the 26.x cells from every
 * older cell in this matrix, plus the RGB unpacking the in-world bar renderer
 * needs to turn a config colour into vertex colours.
 *
 * <p><b>Why {@link #opaqueIfNoAlpha(int)} exists.</b> Up to and including
 * 1.21.4, {@code net.minecraft.client.gui.Font} ran every text colour through
 * a private {@code adjustColor(int)} that read
 * {@code if ((color & 0xFC000000) == 0) return ARGB.opaque(color);} - i.e. a
 * colour with no meaningful alpha byte was silently promoted to fully opaque.
 * <b>26.x deleted that method.</b> Both statements are {@code javap} facts
 * against the real per-cell jars, not recollection: 1.21.4's {@code Font}
 * carries the constant {@code -67108864} ({@code 0xFC000000}) in
 * {@code adjustColor}, and 26.2's {@code Font} has neither the method nor the
 * constant.
 *
 * <p>The practical consequence is severe and completely silent: an
 * {@code 0x00RRGGBB} colour that has rendered correctly for a decade renders
 * <em>fully transparent</em> on 26.x. Nothing throws, nothing logs, the render
 * hook still fires, and every unit test still passes - the pixels simply are
 * not there. The sibling mod simple-utilities-mod shipped exactly that bug and
 * it was caught only by a client gametest that diffed the framebuffer.
 *
 * <p>Applying the fixup here, at the draw boundary, rather than to the config
 * constants themselves, is deliberate: the config colours are user-supplied
 * and already on disk in {@code config/torohealth.json}, and
 * {@code ColorJsonAdapter} deliberately masks the alpha byte off on read so
 * that {@code read(write(x))} round-trips. Rewriting the stored values would
 * fight that; normalising at the point of use does not.
 *
 * <p>Note the threshold is vanilla's {@code 0xFC000000}, not
 * {@code 0xFF000000}: alpha bytes 1-3 counted as "no alpha given" and were
 * promoted, 4 was the first that counted. This reproduces that exactly,
 * because the goal is to preserve the old behaviour on 26.x, not to invent a
 * better rule.
 *
 * <p>This is applied to text only - {@code PlatformHudCanvas#drawString} - for
 * the same reason vanilla applied it to text only. {@code fill} never had the
 * fixup on any version, so adding it there would change behaviour rather than
 * preserve it. Callers that pass a config colour to {@code fill} must call
 * this themselves, and the bar renderers do.
 */
public class Colors {

  /**
   * Promotes a colour with no meaningful alpha byte to fully opaque,
   * reproducing the fixup {@code Font.adjustColor} performed until 26.x
   * removed it. A colour that already carries real alpha is returned
   * untouched, so a deliberately translucent colour stays translucent.
   */
  public static int opaqueIfNoAlpha(int color) {
    return (color & 0xFC000000) == 0 ? color | 0xFF000000 : color;
  }

  /** Red channel of an {@code 0xAARRGGBB} or {@code 0xRRGGBB} colour, 0-1. */
  public static float red(int color) {
    return (color >> 16 & 0xFF) / 255.0f;
  }

  /** Green channel of an {@code 0xAARRGGBB} or {@code 0xRRGGBB} colour, 0-1. */
  public static float green(int color) {
    return (color >> 8 & 0xFF) / 255.0f;
  }

  /** Blue channel of an {@code 0xAARRGGBB} or {@code 0xRRGGBB} colour, 0-1. */
  public static float blue(int color) {
    return (color & 0xFF) / 255.0f;
  }

  /**
   * Alpha channel of a colour, 0-1, applying {@link #opaqueIfNoAlpha(int)}
   * first - so the {@code 0xRRGGBB} form the config stores (alpha masked off
   * by {@code ColorJsonAdapter}) yields 1.0 rather than an invisible 0.0.
   */
  public static float alpha(int color) {
    return (opaqueIfNoAlpha(color) >>> 24 & 0xFF) / 255.0f;
  }
}
