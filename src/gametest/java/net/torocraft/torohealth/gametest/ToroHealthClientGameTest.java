package net.torocraft.torohealth.gametest;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import javax.imageio.ImageIO;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.TitleScreen;
import net.torocraft.torohealth.ToroHealth;
import net.torocraft.torohealth.config.Config;
import net.torocraft.torohealth.display.Hud;

/**
 * Tier 3: this mod running inside a <strong>real Minecraft client</strong> --
 * real window, real GL context, real render thread, real world, a real mob in
 * the crosshair -- asserted against <strong>the pixels that actually reached
 * the screen</strong>.
 *
 * <p>It exists because of a hole neither other tier can reach. The headless
 * suite covers every pure branch (config, the bar state machine, the colour
 * helpers) hand-fed. Tier 1 ({@code LoadedGameTest}, via fabric-loader-junit)
 * proves a genuine Fabric loader discovers this mod and that its dependency
 * ranges resolve. Neither renders a frame, which is why every class under
 * {@code display/**} and {@code render/**} is excluded from the enforced
 * coverage bundle. So neither can answer the only question that matters to a
 * HUD mod: when a player looks at a mob in a running client, does anything
 * appear? A HUD that silently never draws still compiles, still packages,
 * still loads, and still passes every other test in this repo.
 *
 * <p><strong>That is not hypothetical here.</strong> Two real, shipped defects
 * in this fork sat inside exactly that blind spot, and both were found while
 * building this test rather than by it:
 *
 * <ul>
 *   <li>{@code ConfigScreen} drew seven of its field labels at
 *       {@code 0xFFFFFF} -- no alpha byte. Through 1.21.4 vanilla's
 *       {@code Font.adjustColor} silently promoted such colours to opaque; 26.x
 *       deleted that method, so on 26.2 those labels rendered fully
 *       transparent. See {@code Colors#opaqueIfNoAlpha} for the javap
 *       evidence.</li>
 *   <li>The port of {@code HealthBarRenderer} into this fork replaced
 *       upstream's relation-based bar colouring with a fixed health ramp,
 *       leaving {@code bar.friendColor}, {@code bar.friendColorSecondary},
 *       {@code bar.foeColor} and {@code bar.foeColorSecondary} wired to
 *       nothing -- four config options, plus their lang strings and config
 *       screen rows, that could not change a single pixel. See PLAN.md
 *       "Restored: friend/foe bar colours".</li>
 * </ul>
 *
 * <p>Both are invisible to a green build, and both are the kind of thing only
 * a screenshot can catch. So this test reads its screenshots back.
 *
 * <h2>Making the HUD appear at all</h2>
 *
 * <p>Unlike the sibling simple-utilities mod, whose overlay draws
 * unconditionally, this HUD only appears when a living entity is in the
 * player's crosshair -- {@code ClientEventHandler.clientTick} ray-traces every
 * tick and hands the result to {@code Hud.setEntity}. The world therefore has
 * to be staged: a pig is summoned three blocks in front of the player and the
 * player is turned to face it. A pig specifically because
 * {@code EntityUtil.determineRelation} classifies any {@code Animal} as
 * {@code FRIEND}, which routes the bar through the friend colours -- the pair
 * that the port had left dead.
 *
 * <p>Commands are issued through {@link TestServerContext#runCommand}, which
 * dispatches via {@code Commands.performPrefixedCommand} on the server's own
 * source. That method swallows command failures: a typo, or a selector that
 * matches nothing, logs and returns rather than throwing. Nothing here trusts
 * a command to have worked -- {@link #assertPigIsInTheCrosshair} reads the
 * live {@code Hud} back on the client thread and fails loudly if the staging
 * did not take, so a broken setup can never be mistaken for a broken mod.
 *
 * <h2>How the pixel assertion works</h2>
 *
 * <p>Three screenshots, all from the same standing position with the pig in
 * the crosshair, {@value #SETTLE_TICKS} ticks apart:
 *
 * <ol>
 *   <li><b>off-a</b> and <b>off-b</b>, with the HUD suppressed;
 *   <li><b>on</b>, with it enabled again.
 * </ol>
 *
 * <p>{@code diff(off-a, off-b)} is the <em>ambient noise floor</em>: how much
 * the frame changes on its own over one interval while the mod draws nothing.
 * {@code diff(off-b, on)} is that same noise <em>plus</em> whatever the mod
 * drew. Subtracting estimates the mod's own pixels with no golden image, no
 * fixed resolution, no fixed GUI scale and no per-version reference art --
 * which matters because this one file is shared by two Minecraft versions and
 * runs both on a Mac laptop and on a software-rendered xvfb CI box, where
 * nothing about the framebuffer matches.
 *
 * <p>The suppression is done with {@code hud.onlyWhenHurt}, a real user-facing
 * option, against a pig at full health: a true value makes {@code Hud.draw}
 * return before drawing anything. Using a real option rather than reaching in
 * to null the target entity keeps the "off" state one the mod itself produces,
 * and means the off/on pair also exercises that option's branch.
 *
 * <p>Only the top-left quadrant is measured. That is where the HUD lives --
 * {@code hud.anchorPoint} defaults to {@code TOP_LEFT} at (4, 4), and the
 * panel is 160x60 -- and it excludes the hotbar, the health and hunger bars
 * and the chat line, all of which animate on their own.
 *
 * <p>If the noise floor itself is implausibly large the test fails rather than
 * passing on a coin flip: a noisy reading cannot be honestly interpreted
 * either way, and silently accepting one is exactly the vacuous pass this tier
 * exists to eliminate. The world is frozen first (fixed time, no weather
 * cycle, no random ticks, no mob spawning, every non-player entity cleared)
 * to keep that floor near zero.
 *
 * <p>What it asserts, in order:
 *
 * <ol>
 *   <li>the client reaches the title screen at all -- the mod's client
 *       entrypoint ran without killing startup;</li>
 *   <li>the loader running that client really has {@code torohealth} loaded
 *       (a guard on the harness: without it everything below could pass with
 *       the mod jar absent from the classpath);</li>
 *   <li>the ray trace actually put the pig in the crosshair, read off the live
 *       {@code Hud} -- otherwise the pixel comparison below would be measuring
 *       an empty screen against an empty screen and reporting a clean zero;</li>
 *   <li>the HUD <strong>gets past its own guards and draws</strong>, read off
 *       {@link Hud#hasRendered()}. Kept separate from the pixel check purely
 *       for diagnosis: it distinguishes "the render hook is wrong for this
 *       version" from "every draw call ran but produced nothing visible";</li>
 *   <li>the HUD <strong>changes the framebuffer</strong> by at least
 *       {@value #MIN_HUD_PIXELS} pixels above the ambient noise floor -- the
 *       assertion an alpha-0 regression would fail;</li>
 *   <li>the whole draw path survives sustained rendering. Nothing here
 *       swallows exceptions, so anything thrown while drawing propagates out
 *       of the render thread and takes the client down, failing this test by
 *       crash rather than by assertion.</li>
 * </ol>
 *
 * <p>All screenshots are written to
 * {@code versions/<cell>/build/run/clientGameTest/screenshots/} on every run
 * as evidence, and their paths are quoted in the failure messages so a CI
 * failure can be diagnosed from the uploaded artifacts alone.
 *
 * <p><strong>No version branching, though Stonecutter would allow it.</strong>
 * This source set <em>is</em> preprocessed (Loom's {@code createSourceSet}
 * leaves {@code gametest} on Gradle's default convention and Stonecutter picks
 * it up from there), so {@code //? if} markers would work. There are none
 * because none are needed: the file is shared by the 1.21.4 and 26.2 Fabric
 * cells (see {@code build.gradle.kts}, {@code clientGameTestSupported}), which
 * ship client-gametest API v4.1.1 and v6.0.0 respectively, and only the
 * surface those two have in common is used -- verified with javap against both
 * jars. In particular {@code TestSingleplayerContext.getClientWorld()} (v4
 * only) and {@code getConnection()} (v6 only) are both avoided. The
 * screenshots are decoded with {@link ImageIO} rather than Minecraft's own
 * {@code NativeImage} for the same reason: {@code NativeImage}'s pixel
 * accessors were renamed across this range, the JDK's were not.
 */
public class ToroHealthClientGameTest implements FabricClientGameTest {
    /** Ticks to let the world settle between screenshots. */
    private static final int SETTLE_TICKS = 20;

    /**
     * How many pixels the HUD must change, over and above the ambient noise
     * floor, before it counts as visible.
     *
     * <p>Deliberately a floor, not a target. The HUD is a 160x60 panel with a
     * background texture, a rotating entity portrait, a health bar and two
     * lines of text, so the real reading is far above this; an alpha-0
     * regression of the kind described in this class's javadoc produces a
     * reading of nothing at all. Anything in between is not a legible HUD
     * either way, so a low bar costs nothing and keeps the test from becoming
     * a brittle assertion about font metrics, GUI scale or framebuffer size --
     * all three of which differ between a Mac laptop and the software-rendered
     * CI box.
     */
    private static final int MIN_HUD_PIXELS = 300;

    /**
     * Per-channel difference at which two pixels count as different.
     *
     * <p>Set from measurement taken in the sibling simple-utilities repo, not
     * from taste, and the measurement is why this needs a comment. A low
     * threshold does not measure what it looks like it measures: between two
     * consecutive frames of an idle world, ~91k pixels differ by more than 8
     * and ~18k by more than 16, because the sky and terrain lighting drift as
     * a smooth whole-frame gradient. That is not a per-pixel-independent noise
     * source, so a low threshold sums it into tens of thousands of pixels and
     * drowns the signal. Its amplitude is bounded and small though: ~100
     * pixels differ by more than 32 and <strong>zero</strong> by more than 64.
     * HUD pixels are the opposite shape -- few pixels, enormous amplitude.
     * A threshold of 64 separates the two with an order of magnitude of
     * headroom on each side.
     */
    private static final int CHANNEL_DELTA = 64;

    /**
     * Largest ambient noise floor, as a fraction of the measured region, that
     * still permits an honest verdict. Past this the subtraction is measuring
     * the weather, not the mod.
     */
    private static final double MAX_NOISE_FRACTION = 0.10;

    private static final String MOD_ID = "torohealth";

    /**
     * Staging for the world: freeze everything that moves, clear everything
     * that was already there, then put one pig in front of the player.
     *
     * <p>The freezing is what keeps the ambient noise floor near zero, and
     * near zero is what makes the differential in
     * {@link #assertHudIsVisible} readable. {@code sendCommandFeedback} goes
     * first so the rest of these do not each print a fading line into chat.
     * The kill runs before the summon so the pig is the only mob in the
     * world, and {@code NoAI}/{@code NoGravity} stop it from wandering out of
     * the crosshair between screenshots.
     *
     * <p>Each is wrapped in {@code execute at @p} where it needs the player's
     * position: {@link TestServerContext#runCommand} dispatches from the
     * server's own command source, whose position is the world spawn, so a
     * bare {@code ~ ~ ~} would refer to spawn rather than to wherever the
     * player actually is.
     */
    private static final String[] WORLD_SETUP = {
        // 26.2 renamed every game rule to snake_case and moved the registry
        // from net.minecraft.world.level.GameRules to
        // net.minecraft.world.level.gamerules.GameRules. Three of these six
        // also changed identity rather than just spelling: doDaylightCycle ->
        // advance_time, doWeatherCycle -> advance_weather, doMobSpawning ->
        // spawn_mobs, and doFireTick (boolean) became
        // fire_spread_radius_around_player (integer, default 128, minimum -1),
        // so "off" is 0 rather than false. Read off the real 26.2 jar's
        // GameRules.<clinit>, where each id string is followed by the
        // putstatic naming its field.
        //
        // This block was silently wrong on 26.2 until the sibling repo
        // EasierVillagerTrading hit it: runCommand swallows command failures,
        // so all six came back "Incorrect argument for command" on the server
        // console while the test went green. The pixel differential that this
        // test actually asserts on does not depend on them - they only pin
        // down ambient noise, and a 20-second test barely gives daylight or
        // weather time to move - which is exactly why nothing caught it. It
        // is fixed here rather than left alone because the noise floor is
        // load-bearing for the threshold, and a floor that quietly stopped
        // being enforced is the kind of thing that only shows up as a flake
        // months later.
        //? if <26.2 {
        "gamerule sendCommandFeedback false",
        "gamerule doDaylightCycle false",
        "gamerule doWeatherCycle false",
        "gamerule doMobSpawning false",
        "gamerule randomTickSpeed 0",
        "gamerule doFireTick false",
        //?} else {
        /*"gamerule send_command_feedback false",
        "gamerule advance_time false",
        "gamerule advance_weather false",
        "gamerule spawn_mobs false",
        "gamerule random_tick_speed 0",
        "gamerule fire_spread_radius_around_player 0",
        *///?}
        "weather clear",
        "time set noon",
        "difficulty peaceful",
        "kill @e[type=!minecraft:player]",
        // Level the pitch: the ^ ^1 ^3 below is a local offset along the
        // player's own facing, so summoning while they happen to be looking at
        // the sky or their feet would put the pig somewhere unreachable.
        "execute as @p at @s run tp @s ~ ~ ~ ~ 0",
        // Float the pig one block up, and let NoGravity hold it there.
        //
        // This offset is the whole aiming strategy, so it is worth stating why
        // rather than leaving it as a magic 1. RayTrace#getEntityInCrosshair
        // casts from getEyePosition, which is 1.62 above the player's feet. A
        // pig is 0.9 tall, so summoning it at the local origin (feet on the
        // ground, body spanning 0.0 to 0.9) leaves it entirely below the ray;
        // lifting it by 1 puts its body across 1.0 to 1.9 and the eye line at
        // 1.62 passes through its upper torso with clearance either side.
        //
        // Because ^ ^1 ^3 is expressed in the player's own local frame and the
        // pitch was just zeroed, the pig also lands exactly on the yaw axis --
        // dead centre horizontally, no aiming needed in that plane at all.
        //
        // The lift buys one more thing. The ray is now horizontal 1.62 above a
        // flat world, so it strikes no block along its whole length, and
        // getEntityInCrosshair's block-occlusion check takes its MISS branch
        // and returns the entity outright. The alternative branch compares a
        // block-hit distance against the entity distance, which is exactly the
        // sort of near-tie this test should not be resting on.
        "execute as @p at @s run summon minecraft:pig ^ ^1 ^3 "
                + "{NoGravity:1b,NoAI:1b,Silent:1b,PersistenceRequired:1b}",
        // Deliberately NOT `tp @s ~ ~ ~ facing entity <pig> eyes`. That reads
        // like the obvious way to aim and it silently does not work: vanilla's
        // teleport command hardcodes the *self* anchor of its facing
        // calculation to FEET (the anchor argument applies to the target, not
        // the source), while the crosshair ray casts from the eyes. Over three
        // blocks that 1.62 discrepancy tips the aim about 30 degrees skyward
        // and the ray passes clean over the pig -- observed, not theorised:
        // the run before this one reported pitch=-30.5 with the pig 3.16m away
        // and dead ahead. Re-levelling to a known-zero pitch avoids the
        // anchor question entirely, which is why the geometry above was built
        // to work at pitch zero in the first place.
        "execute as @p at @s run tp @s ~ ~ ~ ~ 0",
    };

    @Override
    public void runTest(ClientGameTestContext context) {
        context.waitForScreen(TitleScreen.class);
        context.takeScreenshot("torohealth-title-screen");

        if (!FabricLoader.getInstance().isModLoaded(MOD_ID)) {
            throw new AssertionError("the client booted without the '" + MOD_ID
                    + "' mod loaded - everything below this point would have passed vacuously");
        }

        try (TestSingleplayerContext singleplayer =
                     context.worldBuilder().setUseConsistentSettings(true).create()) {
            for (String command : WORLD_SETUP) {
                singleplayer.getServer().runCommand(command);
            }

            context.waitTicks(SETTLE_TICKS);

            // Taken before the guard below rather than after it, so that when
            // the guard does fire there is a picture of the world it fired in.
            // The staging is geometry - where the pig ended up relative to the
            // crosshair - and a screenshot answers that in one glance where the
            // server log can only say the commands returned success.
            context.takeScreenshot("torohealth-staged-world");
            assertPigIsInTheCrosshair(context);

            // HUD suppressed: two frames one interval apart, to measure how
            // much this world changes on its own while the mod draws nothing.
            setHudSuppressed(context, true);
            Hud.resetRendered();
            context.waitTicks(SETTLE_TICKS);
            Path offA = context.takeScreenshot("torohealth-hud-off-a");
            context.waitTicks(SETTLE_TICKS);
            Path offB = context.takeScreenshot("torohealth-hud-off-b");

            if (Hud.hasRendered()) {
                throw new AssertionError("hud.onlyWhenHurt was set but the HUD drew anyway, so the "
                        + "'off' screenshots are not blank and the differential below would "
                        + "understate the HUD by exactly as much as it is drawing. This is a fault "
                        + "in Hud.draw's onlyWhenHurt guard, not in the test's premise.");
            }

            // HUD enabled: same interval again, same viewpoint, same pig.
            setHudSuppressed(context, false);
            context.waitTicks(SETTLE_TICKS);
            Path on = context.takeScreenshot("torohealth-hud-on");

            if (!Hud.hasRendered()) {
                throw new AssertionError("the HUD never drew: after " + (3 * SETTLE_TICKS)
                        + " in-world ticks with a pig confirmed in the crosshair, "
                        + "Hud.hasRendered() is still false, which means Hud.draw never got past "
                        + "its own guards. Either the render hook registered by "
                        + "ClientEventHandler.register() is not one this Minecraft version fires, "
                        + "or the target entity was lost between the check above and now.");
            }

            assertHudIsVisible(offA, offB, on);

            // Keep the singleplayer context referenced until here so it is
            // unambiguous that the assertions above ran with the world open.
            singleplayer.getWorldSave();
        }

        // The client gametest runner requires the test to end on the title
        // screen with no server running and no connection open.
        context.waitForScreen(TitleScreen.class);
    }

    /**
     * Confirms the staging worked before anything is measured.
     *
     * <p>{@code Hud.getEntity()} is set by {@code ClientEventHandler}'s tick
     * handler from {@code RayTrace.getEntityInCrosshair}, so a non-null pig
     * here proves three separate things at once: the summon command actually
     * ran, the player really is facing the pig, and the client-tick hook this
     * Minecraft version needs is registered and firing. Without this check a
     * mis-staged world would leave the HUD legitimately blank and the pixel
     * differential would report a clean, entirely meaningless zero.
     */
    private static void assertPigIsInTheCrosshair(ClientGameTestContext context) {
        String target = context.computeOnClient(client -> {
            var entity = ToroHealth.HUD.getEntity();
            return entity == null ? null : entity.getType().toString();
        });

        if (target == null) {
            throw new AssertionError("no entity is in the crosshair after the world setup, so "
                    + "there is nothing for the HUD to draw and the pixel comparison below would "
                    + "be measuring an empty screen against an empty screen. Either the summon or "
                    + "the facing command failed silently (runCommand does not throw on a failed "
                    + "command), or the client tick hook that feeds Hud.setEntity is not firing "
                    + "on this Minecraft version. Client-side view of the staging: "
                    + describeStaging(context));
        }

        if (!target.contains("pig")) {
            throw new AssertionError("expected the summoned pig in the crosshair but found "
                    + target + " - the HUD would still draw, but not for the entity this test "
                    + "staged, so the friend-colour path it is meant to exercise is not the one "
                    + "being measured.");
        }
    }

    /**
     * Reports what the client can actually see, for the failure message above.
     *
     * <p>The three failure modes the guard cannot distinguish on its own are
     * "the pig was never summoned", "the pig exists but the player is not
     * looking at it", and "the pig exists and is in front of the player but
     * the ray trace rejected it". Player position and rotation plus the list
     * of nearby entities separates all three, and asking the client rather
     * than the server also proves the entity was replicated - the crosshair
     * trace runs client-side, so a pig that exists only on the server is
     * invisible to it.
     */
    private static String describeStaging(ClientGameTestContext context) {
        return context.computeOnClient(client -> {
            // `var` throughout rather than the concrete types: LocalPlayer and
            // Entity both moved package in 26.x, and this one file has to
            // compile unchanged on 1.21.4 and 26.2 alike. Inference sidesteps
            // the import that would otherwise need a Stonecutter branch.
            var player = client.player;
            if (player == null) {
                return "there is no client player at all";
            }

            StringBuilder nearby = new StringBuilder();
            if (client.level != null) {
                for (var entity : client.level.getEntities(player,
                        player.getBoundingBox().inflate(16.0))) {
                    if (nearby.length() > 0) {
                        nearby.append(", ");
                    }
                    nearby.append(String.format(Locale.ROOT, "%s at (%.2f, %.2f, %.2f) %.2fm away",
                            entity.getType(), entity.getX(), entity.getY(), entity.getZ(),
                            entity.distanceTo(player)));
                }
            }

            return String.format(Locale.ROOT,
                    "player at (%.2f, %.2f, %.2f) eyes y=%.2f looking yaw=%.1f pitch=%.1f; "
                            + "entities within 16m: [%s]",
                    player.getX(), player.getY(), player.getZ(), player.getEyeY(),
                    player.getYRot(), player.getXRot(),
                    nearby.length() == 0 ? "none" : nearby.toString());
        });
    }

    /**
     * Switches the HUD off and on through a real config option rather than by
     * reaching into the renderer.
     *
     * <p>{@code hud.onlyWhenHurt} makes {@code Hud.draw} return before drawing
     * anything while the target is at full health, and the staged pig is
     * untouched. That gives a genuinely blank "off" state produced by the
     * mod's own code path - and exercises the option's branch as a side
     * effect.
     *
     * <p>The config is forced rather than assumed: {@code Hud.draw} re-reads
     * {@code ToroHealth.CONFIG} every frame, and a stale {@code torohealth.json}
     * left in the run directory could otherwise have turned any of these
     * assertions into a no-op. For the same reason the panel's three
     * sub-toggles are forced on, so what the "on" frame shows does not depend
     * on what a previous run happened to write to disk.
     */
    private static void setHudSuppressed(ClientGameTestContext context, boolean suppressed) {
        context.runOnClient(client -> {
            Config config = ToroHealth.CONFIG;
            config.hud.onlyWhenHurt = suppressed;
            config.hud.showEntity = true;
            config.hud.showBar = true;
            config.hud.showSkin = true;
        });
    }

    /**
     * The pixel assertion described in this class's javadoc: the HUD must move
     * more pixels than the world moves on its own.
     */
    private static void assertHudIsVisible(Path offA, Path offB, Path on) {
        BufferedImage imageOffA = read(offA);
        BufferedImage imageOffB = read(offB);
        BufferedImage imageOn = read(on);

        int width = Math.min(imageOffA.getWidth(),
                Math.min(imageOffB.getWidth(), imageOn.getWidth())) / 2;
        int height = Math.min(imageOffA.getHeight(),
                Math.min(imageOffB.getHeight(), imageOn.getHeight())) / 2;

        int noise = differingPixels(imageOffA, imageOffB, width, height);
        int signal = differingPixels(imageOffB, imageOn, width, height);
        int attributable = signal - noise;

        String evidence = "measured the top-left " + width + "x" + height + " pixels; "
                + "ambient noise floor (HUD off, two frames " + SETTLE_TICKS + " ticks apart) = "
                + noise + " px; HUD-on difference = " + signal + " px; "
                + "attributable to the HUD = " + attributable + " px."
                + System.lineSeparator() + "  HUD off: " + offA
                + System.lineSeparator() + "  HUD off: " + offB
                + System.lineSeparator() + "  HUD on:  " + on;

        // Printed on success too, not only into the failure messages below. A
        // pass that clears the threshold by a handful of pixels and a pass that
        // clears it by an order of magnitude are the same green tick in CI, and
        // they are not the same result: the first is about to start flaking.
        // Putting the margin in the log makes that visible before it breaks.
        System.out.println("[torohealth-gametest] " + evidence);

        int maxNoise = (int) (width * (long) height * MAX_NOISE_FRACTION);
        if (noise > maxNoise) {
            throw new AssertionError("this world is too visually unstable to judge the HUD "
                    + "against: the frame changed by " + noise + " px with the HUD off, above the "
                    + maxNoise + " px ceiling, so subtracting it cannot honestly tell you whether "
                    + "the HUD drew anything. This is a fault in the test's premise, not a verdict "
                    + "on the mod - check the screenshots for weather, a wandering mob in frame, "
                    + "or a moving camera. " + evidence);
        }

        if (attributable < MIN_HUD_PIXELS) {
            throw new AssertionError("the HUD draws but is not visible: enabling it changed fewer "
                    + "than " + MIN_HUD_PIXELS + " pixels above the noise floor. Every draw call "
                    + "ran (Hud.hasRendered() is true) into a frame that does not show them - the "
                    + "known cause is a colour with an empty alpha byte, which vanilla promoted to "
                    + "opaque through 1.21.4 and 26.x does not (see Colors#opaqueIfNoAlpha). "
                    + evidence);
        }
    }

    /** Counts pixels differing by more than {@link #CHANNEL_DELTA} on any channel. */
    private static int differingPixels(BufferedImage a, BufferedImage b, int width, int height) {
        int count = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixelA = a.getRGB(x, y);
                int pixelB = b.getRGB(x, y);

                if (pixelA == pixelB) {
                    continue;
                }

                if (Math.abs(((pixelA >> 16) & 0xFF) - ((pixelB >> 16) & 0xFF)) > CHANNEL_DELTA
                        || Math.abs(((pixelA >> 8) & 0xFF) - ((pixelB >> 8) & 0xFF)) > CHANNEL_DELTA
                        || Math.abs((pixelA & 0xFF) - (pixelB & 0xFF)) > CHANNEL_DELTA) {
                    count++;
                }
            }
        }

        return count;
    }

    /**
     * Reads a screenshot back off disk.
     *
     * <p>Safe to call the moment {@code takeScreenshot} returns: the API
     * writes the file synchronously (via {@code NativeImage.writeToFile})
     * before handing back the path, so there is no partial-write window to
     * poll for.
     *
     * <p>Headless is asserted before AWT is first touched. This runs inside a
     * live GLFW client, and on macOS initialising the AWT toolkit in a process
     * that owns the GLFW main thread can abort the JVM outright. PNG decoding
     * itself needs no toolkit, and this keeps it that way.
     */
    private static BufferedImage read(Path screenshot) {
        System.setProperty("java.awt.headless", "true");

        if (!Files.isRegularFile(screenshot)) {
            throw new AssertionError("the client gametest API returned a screenshot path that is "
                    + "not a file: " + screenshot);
        }

        try {
            BufferedImage image = ImageIO.read(screenshot.toFile());

            if (image == null) {
                throw new AssertionError("no image decoder accepted the screenshot at "
                        + screenshot);
            }

            return image;
        } catch (IOException e) {
            throw new UncheckedIOException("failed to read back the screenshot at " + screenshot, e);
        }
    }
}
