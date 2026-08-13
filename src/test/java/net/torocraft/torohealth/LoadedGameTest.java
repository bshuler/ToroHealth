package net.torocraft.torohealth;

//? if fabric {
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModDependency;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.TridentItem;
import net.torocraft.torohealth.bars.BarStateMath;
import net.torocraft.torohealth.config.Config;
import net.torocraft.torohealth.config.loader.ConfigLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tier 1 "loaded game" tests: these run against a real, bootstrapped
 * Minecraft and a real Fabric loader rather than mocks, courtesy of
 * fabric-loader-junit (see the dependency comment in build.gradle.kts).
 *
 * <p>{@code BarStateMathTest} and the config tests already cover this mod's
 * pure logic headless, every input hand-written. What only a loaded game can
 * check is whether the <em>game data the health bar describes</em> still
 * behaves: that every living entity the game registers actually yields a
 * usable bar (a positive, finite max health - {@code BarDisplay} divides by
 * it without a guard), that {@code BarStateMath}'s deliberately
 * Minecraft-free {@code ceil} reimplementation still agrees with vanilla's
 * own {@code Mth.ceil} on real health values, that the damage indicator's
 * tick budget is still one real second, that a config round-trips through
 * the loader's real config directory, and that the weapon classification
 * survives 26.x deleting {@code SwordItem} outright.
 *
 * <p>Fabric cells only: NeoForge's equivalent bootstrap (junit-fml) is only
 * usable from ModDevGradle, not from Architectury Loom - see the junit-fml
 * exclusion comment in build.gradle.kts.
 */
public class LoadedGameTest {

    private static final String MOD_ID = "torohealth";

    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        // 26.x only, and a real behavioural change rather than test plumbing:
        // an item's data components used to be baked into the Item instance at
        // construction, so Bootstrap alone was enough to read one. In 26.x they
        // are produced by BuiltInRegistries.DATA_COMPONENT_INITIALIZERS from a
        // HolderLookup.Provider - i.e. from loaded registry data - and bound
        // onto each Holder.Reference afterwards. Until that runs, constructing
        // any ItemStack throws "Components not bound yet" from
        // Holder.Reference.components(). The server does this during
        // ReloadableServerResources' load; VanillaRegistries.createLookup() is
        // the equivalent built-in-only provider available without a server.
        // (Guarded at 26.1 because that is where this matrix's 26.x line
        // starts - there is no cell between 1.21.4 and 26.2 to narrow it
        // further, and 1.21.4 demonstrably does not need it.)
        //? if >=26.1 {
        /*net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_INITIALIZERS
                .build(net.minecraft.data.registries.VanillaRegistries.createLookup())
                .forEach(net.minecraft.core.component.DataComponentInitializers.PendingComponents::apply);
        *///?}
        bindVanillaItemTags();
    }

    /**
     * Binds vanilla's item tags onto the already-bootstrapped item registry.
     *
     * <p>{@code Bootstrap.bootStrap()} does <em>not</em> do this, and the
     * omission is not an oversight: tags are datapack content, not code, so
     * they only exist once something has read {@code data/minecraft/tags/...}
     * out of a pack. Until then every {@code Holder.Reference.is(TagKey)} call
     * throws {@code IllegalStateException: Tags not bound} - which is exactly
     * how 26.x's tag-based sword check failed the first time this class ran.
     *
     * <p>The fix is to do headless what the dedicated server does at startup:
     * open vanilla's own built-in data pack (it ships inside the Minecraft jar
     * that is already on the test classpath - {@code unzip -l} shows ~9000
     * {@code data/minecraft/**} entries, including
     * {@code tags/item/swords.json}) and run the real {@code TagLoader} over
     * it. No fixture, no hand-written tag list: the assertions downstream see
     * the same tag contents a running game would.
     *
     * <p>Three API traps, all found the hard way and all silent, recorded so
     * nobody re-derives them:
     * <ul>
     *   <li>{@code TagLoader.loadTagsForRegistry(ResourceManager,
     *       WritableRegistry)} - the void overload - loads the tags and then
     *       <em>discards</em> the result; its whole bytecode body ends in a
     *       {@code pop}. Use the three-argument overload that returns the map.
     *   <li>{@code WritableRegistry.bindTags(Map)} only fills in the named
     *       {@code HolderSet}s. It does not touch the per-holder tag
     *       membership that {@code Holder.Reference.is(TagKey)} reads, so on
     *       its own it leaves the registry looking loaded while every
     *       {@code is(...)} still throws {@code Tags not bound}. The private
     *       {@code refreshTagsInHolders()} is what closes that gap, and
     *       {@code freeze()} is the public call that runs it.
     *   <li>The other public route to {@code refreshTagsInHolders()},
     *       {@code Registry.prepareTagReload(...).apply()}, is the
     *       <em>reload</em> path and asserts the registry is already frozen
     *       ({@code IllegalStateException: Invalid method used for tag
     *       loading}). After {@code Bootstrap.bootStrap()} alone it is not, so
     *       first-time binding has to go through bindTags + freeze.
     * </ul>
     *
     * <p>Only the item registry is bound, because that is all this mod's
     * weapon check reads. Guarded at 26.1 like the components block above:
     * pre-26 cells never touch a tag, so paying for a resource-pack open there
     * would be dead weight, and {@code TagLoader}'s signature is not stable
     * that far back.
     */
    private static void bindVanillaItemTags() {
        //? if >=26.1 {
        /*net.minecraft.core.WritableRegistry<Item> items =
                (net.minecraft.core.WritableRegistry<Item>) (net.minecraft.core.Registry<Item>)
                        net.minecraft.core.registries.BuiltInRegistries.ITEM;
        java.util.Map<net.minecraft.tags.TagKey<Item>, java.util.List<net.minecraft.core.Holder<Item>>> tags;
        try (net.minecraft.server.packs.resources.CloseableResourceManager vanillaData =
                     new net.minecraft.server.packs.resources.MultiPackResourceManager(
                             net.minecraft.server.packs.PackType.SERVER_DATA,
                             java.util.List.of(net.minecraft.server.packs.repository.ServerPacksSource
                                     .createVanillaPackSource()))) {
            tags = net.minecraft.tags.TagLoader.loadTagsForRegistry(
                    vanillaData,
                    net.minecraft.core.registries.Registries.ITEM,
                    net.minecraft.tags.TagLoader.ElementLookup.fromWritableRegistry(items));
        }
        assertFalse(tags.isEmpty(), "no vanilla item tags were read out of the built-in data pack");
        assertTrue(tags.containsKey(net.minecraft.tags.ItemTags.SWORDS),
                "the built-in data pack yielded item tags but not minecraft:swords");
        items.bindTags(tags);
        items.freeze();
        *///?}
    }

    /** The live entity-type registry; the class moved package at 1.19.3. */
    private static Iterable<EntityType<?>> entityTypes() {
        //? if >=1.19.3 {
        return net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE;
        //?} else
        /*return net.minecraft.core.Registry.ENTITY_TYPE;*/
    }

    @Test
    void gameDataIsActuallyLoaded() {
        // Guard on the harness itself: if the bootstrap above ever silently
        // no-ops, every other assertion in this class becomes vacuous.
        assertNotNull(Items.DIAMOND_SWORD, "Items.DIAMOND_SWORD should be a real loaded game object");
        int registered = 0;
        for (EntityType<?> ignored : entityTypes()) {
            registered++;
        }
        assertTrue(registered > 50,
                "the real entity-type registry should hold the full vanilla entity set, found " + registered);
    }

    @Test
    void modIsDiscoveredByARealFabricLoader() {
        // The processed fabric.mod.json (Stonecraft templating already
        // applied) is on the test classpath, so a real loader discovers this
        // mod exactly as the game would. Malformed or mis-templated metadata
        // fails here instead of at first launch.
        var self = FabricLoader.getInstance().getModContainer(MOD_ID).orElseThrow(
                () -> new AssertionError("a real Fabric loader did not discover mod id '" + MOD_ID + "'"));
        assertEquals(MOD_ID, self.getMetadata().getId());
        assertFalse(self.getMetadata().getVersion().getFriendlyString().isBlank(),
                "mod version must survive resource templating");
    }

    @Test
    void declaredDependencyRangesAreSatisfiableInThisCell() {
        // The real drift hazard in a Stonecutter matrix: a cell can compile and
        // package flawlessly while declaring a minecraft range that excludes
        // the very version it was built for, producing a jar that ships and
        // then refuses to load. Here the loader resolves the ranges against the
        // actually-loaded versions, per cell.
        var loader = FabricLoader.getInstance();
        var self = loader.getModContainer(MOD_ID).orElseThrow();
        for (ModDependency dependency : self.getMetadata().getDependencies()) {
            if (dependency.getKind() != ModDependency.Kind.DEPENDS) {
                continue;
            }
            var provider = loader.getModContainer(dependency.getModId());
            assertTrue(provider.isPresent(),
                    "fabric.mod.json requires '" + dependency.getModId() + "' but nothing provides it");
            assertTrue(dependency.matches(provider.get().getMetadata().getVersion()),
                    "fabric.mod.json requires " + dependency + " but this cell loads "
                            + dependency.getModId() + " "
                            + provider.get().getMetadata().getVersion().getFriendlyString());
        }
    }

    @Test
    void everyLivingEntityTypeYieldsAUsableHealthBar() {
        // BarDisplay computes `health / maxHealth` with no guard at all, and
        // `Mth.ceil(maxHealth)` as the heart count. Both are unsafe if any
        // living entity the game registers can report a zero or non-finite max
        // health - so assert it against every one of them, from the game's own
        // default-attribute data rather than a hand-picked list.
        int checked = 0;
        for (EntityType<?> type : entityTypes()) {
            if (!DefaultAttributes.hasSupplier(type)) {
                continue;
            }
            float maxHealth = (float) maxHealthOf(type);
            assertTrue(maxHealth > 0f && Float.isFinite(maxHealth),
                    type + " reports an unusable max health of " + maxHealth);
            assertTrue(Mth.ceil(maxHealth) >= 1, type + " would render zero hearts");
            for (float health : new float[]{0f, maxHealth / 2f, maxHealth}) {
                float fraction = health / maxHealth;
                assertTrue(Float.isFinite(fraction) && fraction >= 0f && fraction <= 1f,
                        type + " at " + health + "/" + maxHealth + " gives bar fraction " + fraction);
            }
            checked++;
        }
        assertTrue(checked > 40,
                "expected the full vanilla living-entity set to carry default attributes, checked " + checked);
    }

    @Test
    void damageDeltaAgreesWithVanillaCeil() {
        // BarStateMath deliberately reimplements Mth.ceil privately so it can
        // stay Minecraft-free and unit-testable headless (see its javadoc and
        // PLAN.md GOTCHA cc). That is only safe while the copy still behaves
        // like the original - which nothing but a loaded game can check.
        // Driven through tick(), since the copy itself is private:
        // lastDmg == ceil(previous) - ceil(current).
        for (float before = 0.5f; before <= 20f; before += 0.25f) {
            for (float after = 0.25f; after < before; after += 1.75f) {
                var state = new BarStateMath();
                state.tick(before);
                boolean changed = state.tick(after);
                assertTrue(changed, "a health change from " + before + " to " + after + " went unnoticed");
                assertEquals(Mth.ceil(before) - Mth.ceil(after), state.lastDmg,
                        "damage delta disagreed with Mth.ceil for " + before + " -> " + after);
            }
        }
    }

    @Test
    void damageDeltaMatchesRealEntityMaxHealthValues() {
        // The same check, but driven off every living entity type's real
        // default max health instead of a synthetic sweep - fractional values
        // like the axolotl's or the bat's are exactly where an off-by-one in a
        // hand-copied ceil would surface.
        for (EntityType<?> type : entityTypes()) {
            if (!DefaultAttributes.hasSupplier(type)) {
                continue;
            }
            float full = (float) maxHealthOf(type);
            float hurt = Math.max(0.25f, full - 1.5f);
            // Below 0.1 the state machine treats the previous health as
            // "uninitialised" and resets instead of reporting a delta; no
            // vanilla entity is anywhere near that, but don't assume it.
            if (hurt == full || full < 0.1f) {
                continue;
            }
            var state = new BarStateMath();
            state.tick(full);
            state.tick(hurt);
            assertEquals(Mth.ceil(full) - Mth.ceil(hurt), state.lastDmg,
                    "damage delta disagreed with Mth.ceil for " + type + " (" + full + " -> " + hurt + ")");
            assertEquals(state.lastDmg, state.lastDmgCumulative,
                    "a single hit should leave the cumulative counter equal to the last hit");
        }
    }

    @Test
    void damageIndicatorLingersExactlyOneRealSecond() {
        // HEALTH_INDICATOR_DELAY is expressed in ticks and doubled when a hit
        // lands, so the number the player actually experiences is a duration -
        // pin it to the game's own tick rate rather than to a literal 20, and
        // prove the state machine really does clear on that tick and not one
        // either side of it.
        int oneSecond = SharedConstants.TICKS_PER_SECOND;
        assertEquals(oneSecond, (int) (BarStateMath.HEALTH_INDICATOR_DELAY * 2),
                "the damage indicator's tick budget is no longer one real second");

        var state = new BarStateMath();
        state.tick(20f);
        assertTrue(state.tick(14f), "the hit should have been detected");
        assertEquals(6, state.lastDmg);

        for (int tick = 1; tick < oneSecond; tick++) {
            state.tick(14f);
            assertEquals(6, state.lastDmg,
                    "the indicator cleared early, at tick " + tick + " of " + oneSecond);
        }
        state.tick(14f);
        assertEquals(0, state.lastDmg, "the indicator should clear exactly one second after the hit");
    }

    @Test
    void configRoundTripsThroughTheRealLoaderConfigDir() {
        // ConfigLoader is deliberately loader-agnostic (it takes a directory
        // rather than calling FabricLoader itself), which means nothing in the
        // headless tests ever proves the directory the mod actually hands it
        // exists and is writable. A real loader can answer that.
        File configDir = FabricLoader.getInstance().getConfigDir().toFile();
        assertTrue(configDir.isDirectory() || configDir.mkdirs(),
                "the real loader config dir should exist or be creatable: " + configDir);
        File file = new File(configDir, "torohealth-loadedgametest.json");
        file.delete();
        try {
            Config defaults = new Config();
            // Keep the FileWatcher thread out of the test: this asserts the
            // load/save round trip, not filesystem-event delivery (FileWatcherTest
            // covers that headless).
            defaults.watchForChanges = false;
            defaults.bar.foeColor = 0x123456;
            defaults.particle.distance = 42;

            AtomicReference<Config> loaded = new AtomicReference<>();
            new ConfigLoader<>(defaults, configDir, file.getName(), loaded::set).load();

            assertTrue(file.isFile(), "load() should have written a default config into the real config dir");
            Config result = loaded.get();
            assertNotNull(result, "the onLoad callback never fired");
            // Proves the file really was written, re-read and deserialized -
            // including ColorJsonAdapter's hex handling - rather than the
            // defaults object simply being handed back.
            assertNotSame(defaults, result, "load() should hand back the deserialized config, not the defaults");
            assertEquals(0x123456, result.bar.foeColor);
            assertEquals(42, result.particle.distance);
            // update() derives this and Gson never sees it (it is transient).
            assertEquals(42 * 42, result.particle.distanceSquared,
                    "IConfig.update() should have run on the loaded config");
        } finally {
            file.delete();
        }
    }

    @Test
    void realWeaponItemsAreClassifiedAsWeapons() {
        // 26.x deleted SwordItem outright - swords are a plain Item identified
        // only by the ItemTags.SWORDS tag - so HoldingWeaponUpdater carries a
        // version split for exactly this check. Nothing headless can tell
        // whether the replacement still recognises a real sword; a loaded game
        // can. Non-swords are checked too, so a branch that started returning
        // true for everything would not pass.
        assertTrue(isWeapon(Items.DIAMOND_SWORD), "a real diamond sword must count as a weapon");
        assertTrue(isWeapon(Items.DIAMOND_AXE), "a real diamond axe must count as a weapon");
        assertTrue(isWeapon(Items.TRIDENT));
        assertTrue(isWeapon(Items.BOW));
        assertTrue(isWeapon(Items.CROSSBOW));
        assertTrue(isWeapon(Items.POTION));

        assertFalse(isWeapon(Items.DIRT), "a block item must not count as a weapon");
        assertFalse(isWeapon(Items.DIAMOND_PICKAXE), "a pickaxe is a tool, not a weapon, in this mod's terms");
        assertFalse(isWeapon(Items.APPLE));
    }

    /**
     * Byte-for-byte copy of {@code HoldingWeaponUpdater#isWeapon}, which is
     * private and whose only public entry point ({@code update()}) needs a live
     * {@code Minecraft} instance and a {@code Player}. Keep the two in sync:
     * if the version split there changes, change it here.
     */
    private static boolean isWeapon(Item item) {
        //? if >=26.1 {
        /*boolean isSword = item.builtInRegistryHolder().is(net.minecraft.tags.ItemTags.SWORDS);
        *///?} else {
        boolean isSword = item instanceof net.minecraft.world.item.SwordItem;
        //?}
        return isSword
                || item instanceof AxeItem
                || item instanceof TridentItem
                || item instanceof BowItem
                || item instanceof CrossbowItem
                || item instanceof PotionItem;
    }

    /** The game's own default max health for an entity type that has one. */
    @SuppressWarnings("unchecked")
    private static double maxHealthOf(EntityType<?> type) {
        AttributeSupplier supplier = DefaultAttributes.getSupplier((EntityType<? extends LivingEntity>) type);
        assertTrue(supplier.hasAttribute(Attributes.MAX_HEALTH),
                type + " has default attributes but no max health");
        return supplier.getValue(Attributes.MAX_HEALTH);
    }
}
//?}
