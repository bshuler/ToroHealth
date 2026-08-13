# CLAUDE.md — ToroHealth Damage Indicators

## What this mod does

Client-side Minecraft mod. Shows a HUD panel (entity portrait + name + health
bar + armor) for whatever living entity is in the player's crosshairs, pops
floating damage/heal numbers off entities when they take damage, and
optionally draws in-world health bars above entities. Purely visual/client;
no server component, works on vanilla servers.

Config is a JSON file (`config/torohealth.json`) with hot-reload (a
`WatchService` thread reloads it when edited on disk) and, on newer
loader-provided config-screen hooks, an in-game options screen
(`ConfigScreen`).

## Fork provenance — READ BEFORE PORTING

This repo is a fork of **ToroCraft/ToroHealth**, GPL-3.0 licensed (see
`LICENSE` — this means every derivative here **must stay GPL-3.0**, unlike
the MIT/CC0 sibling mods in this workspace). Upstream is `git remote
upstream`, kept intact — **never push to it**.

Upstream has been maintained on **long-lived per-version branches**, not a
single rolling `master`/`main` history:

| Upstream branch | Loader | MC version | Last commit |
|---|---|---|---|
| `1.8.9` … `1.12.1` | Forge (legacy) | 1.8.9 → 1.12.1 | 2016–2017 |
| `forge-1.12.2` | Forge | 1.12.2 | 2018-09 |
| `fabric-1.14.4` | Fabric | 1.14.4 | 2019-10 |
| `fabric-1.15`, `forge-1.15.2` | Fabric / Forge | 1.15.x | 2020 |
| `fabric-1.16.1`/`.3`/`.5`, `forge-1.16.1`…`.4` | Fabric / Forge | 1.16.x | 2020–2021 |
| `forge-1.17.1` | Forge | 1.17.1 | 2021-08 |
| `forge-1.18` | Forge | 1.18 | 2021-12 |
| `forge-1.19` | Forge | 1.19 | 2022-07 |
| `master` (upstream `HEAD`) | **Fabric** (mixin-based) | 1.19 | 2022-06 |
| `neoforge-1.21.8` | **NeoForge** (event-bus based, Java 21, no mixins) | 1.21.8 | **2025-09** |

`neoforge-1.21.8` is by far the newest and most-modernized upstream work —
almost three years newer than `master`/`forge-1.19`, already on Java 21, and
architecturally cleaner (NeoForge's own client render/tick events instead of
mixins into `InGameHud`/`PlayerEntity`/`WorldRenderer`, which is what the old
Fabric ports (`fabric-1.16.5`, `master`) had to do because Fabric API back
then had no stable non-mixin hook for HUD/world rendering).

**This fork's local `main` branch is the result of a normal forward merge of
`upstream/master`** (the Fabric 1.19 mixin port — the closest thing upstream
has to a canonical `HEAD`) into the previously-stale local history (which
had drifted no further than a 1.12.2 Forge snapshot from 2017). That merge
is commit history, not a design decision: **the actual code base for this
modernization is re-derived from `upstream/neoforge-1.21.8`**, because:

- It is the newest upstream logic by ~3 years (health-bar animation timing,
  damage-particle physics, HUD layout math are all inherited from there,
  not from the older Fabric mixin code).
- It is already mixin-free, which matters because Fabric API 1.18+ exposes
  the same hook points NeoForge exposes (`HudRenderCallback`,
  `WorldRenderEvents`, `ClientTickEvents`) — so one event-based design now
  serves Fabric *and* NeoForge *and* Forge, instead of maintaining a
  mixin-based Fabric implementation and an event-based Forge implementation
  as two different architectures.

Old texture-atlas-based skin rendering (`entitystatus.png`,
`skin_template_top/bottom.png`, `default_skin_heavy.png`) from the original
1.12-era mod is **not** carried forward — `neoforge-1.21.8` already replaced
it with simple `GuiGraphics.fill()`-drawn bars/icons plus one background
texture (`default_skin_basic.png`). This fork keeps that simplification.

### Community forks (reference only, not a code source)

`gh api repos/ToroCraft/ToroHealth/forks` lists 58 forks; the vast majority
are stale/abandoned mirrors. None were pushed more recently than upstream's
own `neoforge-1.21.8` (Sep 2025) except `MinecraftIsTooEasy/ToroHealth`
(pushed 2026-08-09) — not inspected in depth for this pass; if picked up
later, treat as reference only per the house fork rule (license-compatible
GPL-3.0 code may be read for porting decisions, but everything committed
here must be written/ported by the agent working this repo).

## Architecture

Multi-version, multi-loader via **Stonecutter** (`dev.kikugie.stonecutter`)
wrapped by **Stonecraft** (`gg.meza.stonecraft`), mirroring the house
templates `critical-orientation` and `EasierVillagerTrading`.

```
ToroHealth/
├── settings.gradle.kts        # stonecutter{} block declares every version×loader cell
├── stonecutter.gradle.kts     # "active" version for IDE/runClient
├── build.gradle.kts           # shared Stonecraft config (applies to every cell) - just
│                               # `plugins { id("gg.meza.stonecraft") }`, 4 lines total;
│                               # no splitEnvironmentSourceSets() call anywhere
├── gradle.properties          # mod.id / mod.version / mod.group
├── versions/dependencies/*.properties   # per-MC-version loader/API versions
├── src/main/java/net/torocraft/torohealth/
│   ├── ToroHealth.java              # shared statics + per-loader entry point (Stonecutter-conditioned)
│   ├── ClientEventHandler.java      # per-loader event registration (HUD/world render/tick)
│   ├── config/                      # POJO config + loader
│   ├── client/ConfigScreen.java     # vanilla Screen, loader-agnostic
│   ├── util/{RayTrace,EntityUtil,HoldingWeaponUpdater}.java
│   ├── bars/{BarState,BarStates,BarParticle,HealthBarRenderer,ParticleRenderer}.java
│   ├── display/{Hud,BarDisplay,EntityDisplay}.java
│   └── render/{HudCanvas,PlatformHudCanvas}.java   # cross-era rendering-arg abstraction, see below
├── src/main/resources/
│   ├── fabric.mod.json
│   ├── META-INF/mods.toml            # Forge cells only
│   ├── META-INF/neoforge.mods.toml   # NeoForge cells only
│   ├── pack.mcmeta
│   └── assets/torohealth/{icon.png,lang/*.json,textures/gui/*.png}
└── versions/                  # Stonecutter-generated per-cell subprojects (git-ignored)
```

### There is no `src/client` split — everything lives under `src/main`

An earlier draft of this document assumed Stonecraft's convention calls
`splitEnvironmentSourceSets()` by default and that this mod would therefore
need a `main`/`client` source-set split (with only loader-and-Minecraft-free
config classes staying in `src/main/java`). **That assumption was checked
against this repo's actual `build.gradle.kts` and found to be wrong**:
`build.gradle.kts` is 4 lines (`plugins { id("gg.meza.stonecraft") }`), with
no `splitEnvironmentSourceSets()` call, no `sourceSets`/`client` reference
anywhere, and no `src/client` directory exists anywhere in the tree. Every
class — including the purely client-rendering ones — lives under
`src/main/java`, and `src/main`'s compile classpath does have the full
Minecraft/loader API available. Do not reintroduce a `src/client` split
unless a real build error demands it; if one ever is added, re-verify with
`unzip -l` that Forge/NeoForge jars still contain real classes (a
`splitEnvironmentSourceSets()` misconfiguration is a known trap that
produces a green build but a silently-empty jar).

`ConfigLoader`'s constructor takes the config directory as a `File`
parameter from its caller rather than resolving it itself via loader APIs
(`FabricLoader.getConfigDir()` / `FMLPaths.CONFIGDIR`) — this was originally
done because of the (incorrect) `src/main` classpath-restriction assumption
above, but is kept regardless as good practice: it keeps `ConfigLoader`
loader-agnostic and testable, with the per-loader entry point in
`ToroHealth.java` doing the one loader-specific lookup.

### HudCanvas: one rendering-argument abstraction instead of per-class conditionals

`Hud`, `BarDisplay`, and `EntityDisplay` render against a `HudCanvas`
interface (`render/HudCanvas.java`) implemented by the single Stonecutter-
branched class `render/PlatformHudCanvas.java`, which wraps `PoseStack`
pre-1.20 and `GuiGraphics` from 1.20 on. This mirrors the sibling mod
FlightHud's `FlightCanvas`/`PlatformCanvas` split. The effect: only
`Hud`'s public `draw(...)` entry point (two per-era overloads, picking the
right canvas wrapper for whatever the caller — `ClientEventHandler` — was
handed) and `PlatformHudCanvas` itself carry Stonecutter conditionals for
the rendering-argument type; `BarDisplay`, `EntityDisplay`, and the rest of
`Hud`'s body render purely against `HudCanvas` and are conditional-free.
Prefer extending `HudCanvas` over adding new per-class `//? if <1.20 { ... }`
branches when adding new rendering code.

### GUI rendering API split at 1.20

`GuiGraphics` (the class the modern rendering code
`Hud`/`BarDisplay`/`EntityDisplay`/`HealthBarRenderer` is written against,
following `neoforge-1.21.8`) **did not exist before Minecraft 1.20**. On
1.18.2 and 1.19.4, `Screen`/HUD drawing used a `PoseStack matrices` argument
and static-ish helper methods (`fill(matrices, ...)`,
`drawString(matrices, ...)`, `blit(matrices, ...)`) directly on
`Screen`/`AbstractGui`/`DrawableHelper` (name varies Yarn vs Mojmap). This is
a real rendering-API rewrite, not a rename — see `PLAN.md` for the
version-conditional (`//? if >=1.20 { ... } else { ... }`) approach and its
current state per cell.

### Per-loader event registration differences

| Concern | Fabric | NeoForge (1.21.4) | Forge 1.19.4–1.20.1 | Forge 1.18.2 |
|---|---|---|---|---|
| Entry point | `ClientModInitializer.onInitializeClient()` | `@Mod(MODID)` constructor `(IEventBus)` | `@Mod(MODID)` constructor, `FMLJavaModLoadingContext.get().getModEventBus()` | same as 1.19.4–1.20.1 |
| HUD render | `HudRenderCallback.EVENT` | `RegisterGuiLayersEvent` (register above `VanillaGuiLayers.EFFECTS`) | `RenderGuiOverlayEvent.Pre`, filtered to `event.getOverlay().id() == VanillaGuiOverlay.POTION_ICONS.id()`; `event.getPoseStack()` (1.19.4, no `GuiGraphics` yet) / `event.getGuiGraphics()` (1.20.1) | `RenderGameOverlayEvent.Post`, filtered to `event.getType() == RenderGameOverlayEvent.ElementType.ALL` — **no `VanillaGuiOverlay` registry exists yet**; `RenderGuiOverlayEvent` itself doesn't exist until Forge 1.19 (confirmed via `javap` against the real `forge-1.18.2-40.3.12-universal.jar`) |
| World render (bars/particles) | `WorldRenderEvents.AFTER_TRANSLUCENT` (or `.LAST`) | `RenderLevelStageEvent`, `Stage.AFTER_PARTICLES` | `RenderLevelStageEvent`, `Stage.AFTER_PARTICLES` (same shape as NeoForge — Forge added the staged render event pre-NeoForge-split) | `RenderLevelStageEvent`, `Stage.AFTER_PARTICLES` (unchanged from 1.19.4/1.20.1 — confirmed via `javap`, this one class did *not* shift at 1.19) |
| Client tick | `ClientTickEvents.END_CLIENT_TICK` | `PlayerTickEvent.Pre` / `ClientTickEvent.Post` | `TickEvent.PlayerTickEvent`, `event.phase == TickEvent.Phase.START` | same as 1.19.4–1.20.1 |
| Config screen | `ModMenu`-style factory is optional; wire directly if trivial | `IConfigScreenFactory` extension point | `ConfigScreenHandler.setClientConfigScreen` (varies by Forge version) | same family |

### 26.2 event shapes (both loaders confirmed via real compile + `javap`)

| Concern | Fabric 26.2 | NeoForge 26.2 |
|---|---|---|
| HUD render | `HudElementRegistry.addLast(id, ...)` — element method takes `GuiGraphicsExtractor`+`DeltaTracker`, replacing `HudRenderCallback` | `RegisterGuiLayersEvent` unchanged in shape, just `GuiGraphics` → `GuiGraphicsExtractor` in the registered method's signature |
| World render (bars/particles) | `LevelRenderEvents.AFTER_TRANSLUCENT_FEATURES`, replacing `WorldRenderEvents.AFTER_TRANSLUCENT`; `LevelRenderContext` exposes `poseStack()`+`submitNodeCollector()` but **no `Camera` accessor** — fetch via `Minecraft.getInstance().gameRenderer.mainCamera()` | `SubmitCustomGeometryEvent`, replacing `RenderLevelStageEvent.AFTER_PARTICLES` — `RenderLevelStageEvent` dropped its `Stage` enum entirely in 26.2 (one concrete subclass per stage now) and **none of those subclasses expose a `SubmitNodeCollector`**; `SubmitCustomGeometryEvent` is the only 26.2 NeoForge event that still does |
| Client tick | `ClientTickEvents.END_CLIENT_TICK` — unchanged | `PlayerTickEvent.Pre`, filtered to `client.player == event.getEntity()` — unchanged shape from 1.21.4 |

Neither loader's 26.2 world-render hook exposes a `Camera` directly anymore
— both fetch it the same way, via `gameRenderer.mainCamera()`
(`GameRenderer.getMainCamera()` was renamed; see below).

Because these names shift release to release even within one loader — the
1.18.2-vs-1.19.4+ Forge HUD-render row above is a real example, not a
hypothetical: the original two-way Stonecutter split (`forge && <1.20` /
`forge`) had only ever been verified against 1.19.4 and silently didn't
cover 1.18.2, and was only caught by a real `:1.18.2-forge:build` compile
failure (`cannot find symbol: class RenderGuiOverlayEvent`) — **resolve
every one of these against a real compile of that version/loader cell**,
not from memory or from what an adjacent version needed. `PLAN.md` records
what actually compiled; when in doubt, extract the real loader jar for that
exact MC version and run `javap -p` against it rather than trusting this
table or extrapolating from a neighboring version.

### Testing & coverage (JaCoCo)

JUnit 5 + JaCoCo are wired into `build.gradle.kts`, mirroring the canonical
pattern from the sibling `critical-orientation`/`FlightHud`/
`critical-flight-details`/`EasierVillagerTrading`/`simple-utilities-mod`
repos (including the NeoForge `junit-fml`/`mainargs.txt` classpath-exclusion
guard, gated `if (mod.isNeoforge)`, and the Forge
`compileTestJava`-depends-on-`generatePackMCMetaJson` fix).

```bash
./gradlew test                             # runs the suite on EVERY cell in the matrix
./gradlew :1.21.4-fabric:test              # one cell only
./gradlew jacocoTestReport                 # HTML+XML report, active project
./gradlew check                            # test + 100% coverage gate
./gradlew "Set active project to <mc>-<loader>"   # switch the IDE/runClient node
```

A bare `./gradlew test` runs the suite on **every** cell, not just the
active one — so any new test must compile and pass on all ten, 1.18.2
included. (An earlier revision of this document claimed tests run only
against the active node and "**never** across the full matrix"; that was
wrong, and the Tier 1 work below depends on it being wrong.) The active
node still matters for `jacocoTestReport`/`check` and for `runClient`, and
it is still switched only via the Gradle task — never by hand-toggling
`//? if` markers to reach another node's code.

`tasks.check` depends on `jacocoTestCoverageVerification`, which enforces
`LINE COVEREDRATIO 1.00` (100%) over the classes below. This is a real,
enforced gate, not aspirational — `./gradlew check` fails the build if
coverage regresses. Confirmed: `178`/`178` lines covered (`1.00` ratio) on
`1.21.4-fabric` at the time this was landed.

**Coverage scope** (see `PLAN.md` "Phase 2: Test coverage" for the full
class-by-class breakdown, refactors, and the real bugs this pass found):

| In scope (tested to 100%) | Excluded (documented reason in `PLAN.md`) |
|---|---|
| `config.Config` (+ nested `Hud`/`Particle`/`Bar`/`InWorld`, enums `Mode`/`NumberType`/`AnchorPoint`) | `ToroHealth`, `ClientEventHandler` — loader entrypoints, live singletons |
| `config.loader.Defaulter` | `bars.BarState`, `bars.BarStates`, `bars.BarParticle`, `bars.HealthBarRenderer`, `bars.ParticleRenderer` — touch a live `LivingEntity`/`Minecraft`/rendering primitive |
| `config.loader.ColorJsonAdapter` | `client.ConfigScreen` — vanilla `Screen` subclass |
| `config.loader.ConfigLoader` | `display/**`, `render/**` — render straight to `HudCanvas`/live rendering primitives |
| `config.loader.FileWatcher` | `util.RayTrace`, `util.HoldingWeaponUpdater`, `util.EntityUtil` — call `Minecraft.getInstance()`/live entity types directly |
| `bars.BarStateMath` | |

`bars.BarStateMath` is the extracted pure health/damage-delta state machine
that used to live inline inside `bars.BarState` (which still exists as a
thin wrapper doing the one side effect `BarStateMath` deliberately excludes
— reading the live particle-enabled config flag and constructing a
`BarParticle` against the live entity). `ConfigLoader` and `FileWatcher` are
pure enough to unit test headless specifically because they take a plain
`java.io.File` and don't touch any loader singleton themselves — that
resolution lives at the `ToroHealth.java` call site instead (excluded, per
above). `FileWatcher`'s full `run()` lifecycle is exercised against a
**real** `WatchService`/filesystem write in addition to hand-rolled fakes
for the pure `pollEvents()` branch logic.

### Loaded-game tests (Tier 1)

`src/test/java/net/torocraft/torohealth/LoadedGameTest.java` runs against a
**real, bootstrapped Minecraft and a real Fabric loader**, not mocks, via
`net.fabricmc:fabric-loader-junit:0.19.3`. Nine tests, verified green on all
five Fabric cells (1.18.2, 1.19.4, 1.20.1, 1.21.4, 26.2). The whole file is
wrapped in `//? if fabric { … //?}` and the dependency is gated
`if (mod.isFabric)`, because NeoForge's equivalent bootstrap (`junit-fml`) is
reachable only from ModDevGradle, not from Architectury Loom — see the long
`junit-fml` comment in `build.gradle.kts`. The five Forge/NeoForge cells keep
only the headless tests.

What the loaded game buys over the headless suite (which already covers every
pure branch, hand-fed):

| Test | What only a loaded game can check |
|---|---|
| `gameDataIsActuallyLoaded` | harness guard — if the bootstrap ever silently no-ops, every other assertion here goes vacuous |
| `modIsDiscoveredByARealFabricLoader` | the *processed* `fabric.mod.json` (Stonecraft templating applied) is discoverable by a genuine loader |
| `declaredDependencyRangesAreSatisfiableInThisCell` | each `DEPENDS` range actually matches the provider version present in that cell |
| `everyLivingEntityTypeYieldsAUsableHealthBar` | every registered living type has a positive, finite max health — `BarDisplay:50` divides by it with no guard |
| `damageDeltaAgreesWithVanillaCeil` | `BarStateMath`'s deliberately Minecraft-free `ceil` copy still agrees with vanilla `Mth.ceil` |
| `damageDeltaMatchesRealEntityMaxHealthValues` | same, driven off real registry max-health values rather than invented ones |
| `damageIndicatorLingersExactlyOneRealSecond` | `HEALTH_INDICATOR_DELAY * 2` is still one real `SharedConstants.TICKS_PER_SECOND` |
| `configRoundTripsThroughTheRealLoaderConfigDir` | write/read/`update()` round trip through `FabricLoader.getConfigDir()`, not a temp dir |
| `realWeaponItemsAreClassifiedAsWeapons` | the weapon check still recognises real vanilla items after 26.x deleted `SwordItem` |

Two things this class had to discover about a headless bootstrap, both of
which cost a red build first and are written up at length in the source:

- **26.x data components.** `Bootstrap.bootStrap()` no longer leaves an
  `Item`'s components usable; they are built from
  `BuiltInRegistries.DATA_COMPONENT_INITIALIZERS` and bound afterwards, so
  constructing any `ItemStack` before that throws `Components not bound yet`.
- **Vanilla tags are not bound by `Bootstrap.bootStrap()` at all.** Tags are
  datapack content, so `Holder.Reference.is(TagKey)` throws
  `IllegalStateException: Tags not bound` — which is exactly how 26.x's
  tag-based sword check failed. Rather than skip the assertion, the bootstrap
  now opens vanilla's own built-in data pack (it ships inside the Minecraft
  jar already on the test classpath) and runs the real `TagLoader` over it.
  Three API traps on that path, all silent: the void
  `TagLoader.loadTagsForRegistry(ResourceManager, WritableRegistry)` overload
  discards its result; `WritableRegistry.bindTags(Map)` fills the named
  `HolderSet`s but not per-holder membership; and
  `Registry.prepareTagReload(...).apply()` — the other public route to the
  private `refreshTagsInHolders()` — asserts the registry is already frozen,
  which after a bare bootstrap it is not. The working sequence is
  `bindTags(map)` then `freeze()`.

`realWeaponItemsAreClassifiedAsWeapons` carries a byte-for-byte copy of
`HoldingWeaponUpdater#isWeapon` (private, and its only public entry point
needs a live `Minecraft` and `Player`) including that method's own
`//? if >=26.1` split. **Keep the two in sync.** The 26.2 cell emits one
deprecation note for `Item.builtInRegistryHolder()`; that is deliberate — the
production code calls the same deprecated method, and the copy would stop
being a copy if the test called something else.

What Tier 1 still does *not* cover: anything that needs a window or a GL
context (all of `display/**`, `render/**`, the bar/particle renderers), and
anything on a NeoForge or Forge cell. Those are Tier 3 (Fabric client
gametest under xvfb) and Tier 4 (NeoForge `testframework`, blocked under
Loom) respectively.

## Version matrix (target)

Newest stable Minecraft per `https://meta.fabricmc.net/v2/versions/game`
(checked live) at time of writing: **26.2**.

| MC version | Fabric | NeoForge | Forge |
|---|---|---|---|
| 26.2 (newest stable) | ✅ | ✅ | — (NeoForge only, post-split) |
| 1.21.4 | target | target | — |
| 1.20.1 | target | — | target |
| 1.19.4 | target | — | target |
| 1.18.2 | target | — | target |

**Loader coverage is mandatory**: Fabric + NeoForge for 1.20.5+, Fabric +
Forge for 1.20.4 and older. **Quilt**: not a separate cell — it runs Fabric
jars natively via its Fabric API compatibility layer.

Live status/checklist: `PLAN.md`.

## Build commands

```bash
# Build every version×loader cell
./gradlew chiseledBuild

# Build one cell
./gradlew :1.20.1-fabric:build

# Switch the active version (IDE/runClient) — NEVER hand-edit the
# "DO NOT EDIT" marker in stonecutter.gradle.kts
./gradlew "Set active project to 1.21.4-neoforge"

# Run the client on the active version
./gradlew runClient
```

### Gotcha: stale generated source tree after editing `src/main/java`

Gradle's incremental up-to-date check for the `stonecutterPrepare`/
`stonecutterMerge` tasks can incorrectly report itself current even right
after a genuine edit to `src/main/java`, leaving that cell's generated tree
(`versions/<cell>/build/generated/stonecutter/main/java/...`) stale — the
subsequent `:<cell>:build` then recompiles the *old* generated code and
either passes for the wrong reason or fails with errors that look like they
should already be fixed. If a build result doesn't match what the current
`src/main/java` content should produce, force regeneration first:

```bash
./gradlew --offline --configure-on-demand :<cell>:stonecutterPrepare :<cell>:stonecutterMerge --rerun-tasks
./gradlew :<cell>:build
```

This is cheap enough to run before every cell's build as a matter of
course when iterating on shared (non-cell-specific) source files.

Only JDK available in this environment is **Temurin 21**
(`/Library/Java/JavaVirtualMachines/temurin-21.jdk`). Older MC versions need
older Java at *runtime* (1.18.2/1.19.4 → Java 17) but Loom/ForgeGradle
toolchains handle that via Gradle's Java toolchain auto-provisioning
(foojay-resolver, downloads into `~/.gradle/jdks`) — never install a system
JDK, never touch Homebrew for this. Gradle 9.x needs
`foojay-resolver-convention` **1.0.0** specifically (0.8.0 throws on
`JvmVendorSpec.IBM_SEMERU`).

## Porting notes for whoever (human or AI) continues this

- **Start from `upstream/neoforge-1.21.8`, not from `upstream/master`.** The
  merge that brought `upstream/master` into local `main` is provenance
  history (fork rule: forward-merge upstream into the default branch); it
  is not the code base for the rendering/animation logic. When in doubt
  about "what should this class do", read `neoforge-1.21.8`'s version of it
  first.
- `HoldingWeaponUpdater` in `neoforge-1.21.8` checks
  `item.is(Tags.Items.MELEE_WEAPON_TOOLS)`, a NeoForge-registered common tag
  that doesn't exist on Fabric or old Forge. Ported version checks
  `instanceof SwordItem || instanceof AxeItem || instanceof TridentItem ||
  instanceof BowItem || instanceof CrossbowItem` instead — loader- and
  version-portable, same practical behavior.
- Stonecraft resource-template variables are exactly `${id}`, `${name}`,
  `${version}`, `${description}`, `${minecraftVersion}` (confirmed against
  the house template repos) — inventing keys like `${mod_id}` leaves them
  unexpanded and breaks `fabric.mod.json`/`mods.toml` at runtime.
- To switch Stonecutter's active node, always use
  `./gradlew "Set active project to <mc>-<loader>"` — never hand-edit the
  `/* [SC] DO NOT EDIT */` marker in `stonecutter.gradle.kts`; a stale active
  node compiles the wrong source tree and passes for the wrong reason.
- `26.2`'s GUI pipeline is rewritten again past what `neoforge-1.21.8` shows
  (`GuiGraphics` itself is gone; HUD rendering goes through
  `extractRenderState(...)` populating a `GuiRenderState`). This has been
  ported (both `26.2-fabric` and `26.2-neoforge` build green and are
  jar-verified) — see `PLAN.md` "26.2" section for the full list of applied
  API deltas (entity package moves, `SwordItem` removal + tag-based
  replacement, `GameRenderer`/`Camera`/`Minecraft.setScreen` renames,
  `Screen.render`→`extractRenderState`, and both loaders' 26.2 event
  shapes) and the newly-discovered Stonecutter live-sync corruption gotcha.
  Treat any *future* version past 26.2 as its own port too, not an
  assumed-trivial bump — re-verify every API surface via `javap` against
  the real jar rather than extrapolating from 26.2's deltas.
- Single merged jar (Forgix): see `PLAN.md` § "Single merged jar (Forgix)".
