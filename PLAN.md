# PLAN.md — ToroHealth modernization

## Goal

Port ToroHealth to a Stonecutter+Stonecraft multi-version, multi-loader
project (mirroring `EasierVillagerTrading`), covering newest-stable
Minecraft down through 1.18.2, with every loader viable per version, and a
green `./gradlew chiseledBuild`. Base the ported logic on
`upstream/neoforge-1.21.8` (newest, mixin-free upstream code — see
`CLAUDE.md` § Fork provenance) rather than on `upstream/master`. Stay
GPL-3.0. Never push to `upstream`, never touch `.github/workflows/`.

## Loader coverage rule

Every targeted MC version must build for **every loader viable on that
version**:
- 1.20.5 and newer → **Fabric + NeoForge**
- 1.20.4 and older → **Fabric + Forge**
- Quilt is not a separate cell — it runs Fabric jars natively.

A cell may only be marked ⛔ (blocked) with the exact, live-verified reason
recorded below — never silently dropped from the matrix.

## Status legend

✅ builds green &nbsp;&nbsp; 🔶 in progress / partially working &nbsp;&nbsp; ☐ not started &nbsp;&nbsp; ⛔ blocked (reason recorded)

## Version × loader matrix

| MC version | Fabric | Forge | NeoForge |
|---|---|---|---|
| 26.2 (newest stable) | ✅ | — | ✅ |
| 1.21.4 | ✅ | — | ✅ |
| 1.20.1 | ✅ | ✅ | — |
| 1.19.4 | ✅ | ✅ | — |
| 1.18.2 | ✅ | ✅ | — |

10/10 target cells build green, each spot-checked via
`unzip -l <jar> | grep -c "\.class$"` / `grep "net/torocraft"` to confirm
real mod classes are present (31-42 classes per jar depending on cell), not
just a green-but-empty jar. 26.2-fabric (41 classes) and 26.2-neoforge (42
classes) are the newest additions — see "26.2" below for the full API-delta
list. All work is committed and pushed to `origin/main` — see "Parked
commits" below for how the earlier signing outage was resolved.

Newest stable MC confirmed live against
`https://meta.fabricmc.net/v2/versions/game` at task start: **26.2**
(calendar versioning — this is not a typo for 1.21.4/1.26 etc).

Porting order: **1.21.4-fabric → 1.21.4-neoforge → 1.20.1-fabric →
1.20.1-forge → 1.19.4-fabric → 1.19.4-forge → 1.18.2-fabric →
1.18.2-forge → 26.2-fabric → 26.2-neoforge** (newest-with-most-precedent
first via `neoforge-1.21.8`, then walk backwards; 26.2 last since it is
expected to need its own port per gotcha (c)).

## 26.2 — ✅ both cells green (fabric + neoforge)

Confirmed a genuinely bigger rewrite than a version bump, per the original
expectation below, but fully resolved. Every delta below was confirmed
against real jar content via `javap`/`unzip -l` before writing the fix —
none guessed from memory or extrapolated from 1.21.4.

**Entity package moves** (`net.minecraft.world.entity.*` reorganization —
confirmed via `unzip -l` against the real
`minecraft-merged-deobf-26.2.jar`):
- `Chicken`: `...animal.Chicken` → `...animal.chicken.Chicken`
- `Villager`: `...npc.Villager` → `...npc.villager.Villager`
- `AbstractFish`: `...animal.AbstractFish` → `...animal.fish.AbstractFish`
- `Squid`: `...animal.Squid` → `...animal.squid.Squid`
- `Slime`: `...monster.Slime` → `...monster.cubemob.Slime`
- Unchanged: `Monster`, `Ghast`, `Animal`, `AmbientCreature`, `AgeableMob`,
  `ArmorStand`, `Creeper`.
- Affected: `util/EntityUtil.java`, `display/EntityDisplay.java`.

**`SwordItem` removed entirely** (confirmed via `unzip -l` — no survivor
anywhere in the jar; `DiggerItem`/`TieredItem` also gone). Swords are now a
plain `Item` identified only via the `ItemTags.SWORDS` tag. `ItemStack`
also lost its `is(TagKey<Item>)` convenience overload (only
`is(Predicate<Holder<Item>>)` remains) — the replacement pattern is
`item.builtInRegistryHolder().is(ItemTags.SWORDS)`, since
`Item.builtInRegistryHolder()` returns `Holder$Reference<Item>` and
`Holder.is(TagKey<T>)` survives. `AxeItem`/`BowItem`/`CrossbowItem`/
`TridentItem`/`PotionItem` unchanged. Affected:
`util/HoldingWeaponUpdater.java`.

**Renames** (each confirmed via `javap -p` on the extracted class — old name
has zero survivors):
- `GameRenderer.getMainCamera()` → `mainCamera()`.
- `Camera.getPosition()` → `position()`.
- `Minecraft.setScreen(Screen)` → `setScreenAndShow(Screen)`.
- Affected: `bars/BarParticle.java`, `client/ConfigScreen.java`.

**`Screen.render(GuiGraphics,...)` restructured into
`extractRenderState(GuiGraphicsExtractor,...)`** — no `render(GuiGraphics,
...)` survivor on `Screen` at all. `GuiGraphicsExtractor.drawString(...)` is
now `.text(Font, String, int, int, int)` (and other overloads), matching
the rename `PlatformHudCanvas` already used for its own `>=26.1` branch.
Affected: `client/ConfigScreen.java` (3-way `<1.20` / `elif >=26.1` / `else`
split for its render override).

**NeoForge-specific**: `RenderLevelStageEvent` was restructured in 26.2
(confirmed via `javap` against the real `neoforge-26.2.0.58` jar) — it
dropped its `Stage` enum + `getStage()` in favor of one concrete subclass
per stage, and critically none of those subclasses expose a
`SubmitNodeCollector` anymore. `SubmitCustomGeometryEvent` is the only
NeoForge 26.2 event that still exposes one, so it replaces
`RenderLevelStageEvent.AFTER_PARTICLES` for this mod's world-render hook.
`RegisterGuiLayersEvent`/`GuiLayer` are structurally unchanged apart from
`GuiGraphics` → `GuiGraphicsExtractor`. Affected: `ClientEventHandler.java`
(new `neoforge && >=26.1` arm).

**Fabric-specific**: 26.2's Fabric API replaced `HudRenderCallback`/
`WorldRenderEvents.AFTER_TRANSLUCENT` with `HudElementRegistry` (draw method
takes `GuiGraphicsExtractor`+`DeltaTracker`) and
`LevelRenderEvents.AFTER_TRANSLUCENT_FEATURES` (`LevelRenderContext` exposes
`poseStack()`+`submitNodeCollector()` but, like every other 26.1+
world-render hook, no `Camera` accessor — fetched separately via
`gameRenderer.mainCamera()`). `ClientTickEvents` unchanged. Affected:
`ClientEventHandler.java` (new `fabric && >=26.1` arm).

### Gotcha (e): Stonecutter's live-sync file watcher is not safe for arms mixing bare `//` prose outside a `/* */`-wrapped code body

Switching active project via `./gradlew "Set active project to <mc>-<loader>"`
makes Stonecutter rewrite `src/main/java` in place to toggle which
`//? if/elif/else` arm is live vs `/* */`-wrapped. This is normally safe,
but one switch (`26.2-fabric` → `26.2-neoforge`) corrupted
`ClientEventHandler.java`: the `neoforge && >=26.1` arm had its
explanatory "why" comment written as bare `// ...` lines sitting just
*before* the `/* */` wrapper boundary rather than fully inside it, and the
toggle stripped the leading `//` off those lines without adjusting the
`/* */` boundary to match, turning valid comment text into bare tokens the
Java parser choked on (`Unclosed scope`, then ~69 cascading parse errors).
The fix was to re-add the stripped `//` prefixes — the actual code
underneath was untouched. **Lesson**: keep explanatory comments fully
inside the arm's own `/* */` wrapper (or fully inside the live arm's plain
`//` styling) rather than straddling the boundary, and re-`Read` any file
with mixed comment styles after every active-project switch before trusting
the next build. (Separately: this file's pre-existing nested `/^ ... ^/`
delimiter — used for an inner if/else nested inside an already-inactive
outer arm, since Java can't nest `/* */` — is intentional, not corruption;
it predates this session and was not touched.)

## Single merged jar (Forgix) — independently re-verified, decision: per-loader jars

Re-checked directly for this repo rather than carried over from
`EasierVillagerTrading`: as of this pass, neither side has closed the gap.
Stonecraft's own docs (`stonecraft.meza.gg/docs/releasing-mods`,
`github.com/meza/Stonecraft`) document a **per-loader** publishing flow
with no mention of Forgix/merged/universal jars anywhere in the README,
docs site, or issue tracker. Forgix (`github.com/PacifistMC/Forgix`)'s own
README only shows configuration against static, hand-named subprojects
(e.g. `project(":fabric")`) and has no mention of Stonecutter/Stonecraft in
its README or issues either. This is a persistent tooling mismatch, not a
recently-introduced or recently-fixed one. **Decision**: ship per-loader
jars from `versions/*/build/libs/` (matching Stonecraft's own documented
flow) rather than attempting a Forgix merge.

## Per-version porting notes

### 1.21.4 (fabric, neoforge)

Base: `upstream/neoforge-1.21.8` (one patch version newer; API is expected
to be compatible or need only trivial adjustments — will confirm against
real compiler errors). `GuiGraphics`-based rendering carries over close to
verbatim. NeoForge event names/types from `neoforge-1.21.8` should apply
directly (same major NeoForge line). Fabric side needs the event-callback
translation table in `CLAUDE.md` (`HudRenderCallback`,
`WorldRenderEvents`, `ClientTickEvents`) and a `fabric.mod.json` written
from the Stonecraft template vars.

### 1.20.1 (fabric, forge)

Still has `GuiGraphics` (introduced 1.20) so display/bars code should not
need the legacy-rendering branch. Forge (not NeoForge) at this version —
event names/types must be re-derived against Forge 47.x, not assumed equal
to NeoForge's (NeoForge forked from Forge after 1.20.1, so 1.20.1-forge is
architecturally the closest match to what upstream's own `forge-1.19`
branch used, just needs a version bump check).

### 1.19.4 (fabric, forge)

No `GuiGraphics` — pre-1.20 legacy rendering (`PoseStack`/`MatrixStack` +
direct `Font`/`fill`/`blit` calls) required for `Hud`, `BarDisplay`,
`EntityDisplay`, and `HealthBarRenderer`'s GUI half. Base off upstream's
`forge-1.19` branch content directly for the legacy-rendering reference
implementation, restructured behind Stonecutter version predicates
alongside the modern branch.

### 1.18.2 (fabric, forge)

Same legacy-rendering family as 1.19.4; upstream's `forge-1.18` branch is
the closest direct reference. Confirm Fabric API surface for this version
still exposes `WorldRenderEvents`/`ClientTickEvents`/`HudRenderCallback`
(it does, per Fabric API's changelog history — Fabric's core client
lifecycle/rendering events have been stable since well before 1.18).

## Phase 2: Test coverage

Bert's directive: 100% test coverage, enforced (not aspirational). JUnit 5 +
JaCoCo wired into `build.gradle.kts` mirroring the canonical pattern from
`critical-orientation`/`FlightHud`/`critical-flight-details`/
`EasierVillagerTrading`/`simple-utilities-mod`: `jacoco` plugin,
`tasks.test { useJUnitPlatform(); finalizedBy(jacocoTestReport) }`, a shared
`jacocoExcludes` glob list applied to both `jacocoTestReport` and
`jacocoTestCoverageVerification`'s `classDirectories`, a `LINE
COVEREDRATIO 1.00` violation rule, `tasks.check { dependsOn(...) }`, the
NeoForge `junit-fml` classpath exclusion (`if (mod.isNeoforge)`), and the
Forge `compileTestJava dependsOn generatePackMCMetaJson` fix. All work done
against the single active Stonecutter node (`1.21.4-fabric`, matching
`vcsVersion`) — never across the matrix, never by hand-toggling `//? if`
markers.

**Result: 100% LINE coverage on the included scope — 178/178 lines, ratio
1.00, `./gradlew :1.21.4-fabric:check` green.**

### Coverage table

| Class | Lines | Why testable |
|---|---|---|
| `config.Config` (+ nested `Hud`/`Particle`/`Bar`/`InWorld`, enums) | 43 | Plain POJO + enums, zero `net.minecraft` imports |
| `config.loader.Defaulter` | 16 | Pure reflection helper, zero MC imports |
| `config.loader.ColorJsonAdapter` | 12 | Pure Gson `TypeAdapter<Integer>`, no MC types |
| `config.loader.ConfigLoader` | 34 | Takes a plain `java.io.File`; loader-specific config-dir resolution lives at the `ToroHealth.java` call site instead |
| `config.loader.FileWatcher` | 33 | Wraps `java.nio.file.WatchService` only; no MC/loader types |
| `bars.BarStateMath` | 40 | Extracted pure health/damage-delta state machine (see below) |
| **Total** | **178** | **178/178 covered — 1.00 ratio** |

### Exclusion table (honest, per class)

| Class | Reason excluded |
|---|---|
| `ToroHealth`, `ClientEventHandler` | Loader entrypoints — construct/register against live `Minecraft`/event-bus singletons at class-load time |
| `bars.BarState` | Thin wrapper over `BarStateMath` that reads a live `LivingEntity` and the live particle-enabled config flag, and constructs a `BarParticle` against the live entity/camera |
| `bars.BarStates`, `bars.BarParticle`, `bars.HealthBarRenderer`, `bars.ParticleRenderer` | Hold/render against live entities and `GuiGraphics`/`PoseStack` rendering primitives |
| `client.ConfigScreen` | Vanilla `Screen` subclass — needs a running game client to construct/render |
| `display/**`, `render/**` | Render straight to `HudCanvas`/`GuiGraphics`/`PoseStack` |
| `util.RayTrace` | Calls `Minecraft.getInstance()`, raytraces against a live `Level` |
| `util.HoldingWeaponUpdater` | Calls `Minecraft.getInstance()`, reads the live player's held `ItemStack`s |
| `util.EntityUtil` | Takes a live `Entity`/`Minecraft` and calls `entity.isInvisibleTo(player)` etc. — real per-instance entity state, not headless-safe |

None of the excluded classes have any extractable pure logic left inside
them beyond what's already been pulled into `BarStateMath` — each one's
entire body is either a loader/event-bus registration call or a direct
read of live entity/rendering state.

### `BarStateMath` extraction

`bars.BarState`'s `tick()`/`reset()`/`incrementTimers()`/
`handleHealthChange()`/`updateAnimations()` methods were pure numeric state
machine logic (health/damage deltas, decay-and-snap animation timing) with
no `net.minecraft` dependency of their own — only the one side effect
(constructing a `BarParticle`, which needs the live entity) tied `BarState`
to Minecraft types. Extracted that logic verbatim into a new
`bars.BarStateMath` class (`tick(float health)` returns whether the health
changed, exposes the same public fields `previousHealth`,
`previousHealthDisplay`, `previousHealthDelay`, `lastDmg`,
`lastDmgCumulative`, `lastHealth`, `lastDmgDelay`); `BarState` now holds a
`BarStateMath math` field, delegates `tick()` to it, and only performs the
particle side effect when `math.tick(...)` reports a change. Same pattern as
`FlightComputerMath`/`GameInfoMath` in the sibling mods.

**Gotcha for whoever writes float-arithmetic tests next**: a health/damage
decay test written by hand-computing expected values in double precision
(or in a Python re-implementation) can be *wrong* against the real `float`
arithmetic the algorithm actually runs in — repeated `float` subtraction
(here, `previousHealthDisplay -= animationSpeed` where `animationSpeed` is a
`float` `0.8f`) accumulates rounding error that can shift which branch fires
by one extra tick versus a double-precision prediction. Caught this while
writing `BarStateMathTest.tick_previousHealthSnapsToCurrentHealthOnceDisplayCatchesUp`:
a double-precision simulation predicted the decay-to-snap transition
happened after 18 same-health ticks landing exactly on `12.0f`; the real
compiled class actually lands on `12.000003f` (still fractionally *above*
12) after 18 ticks, decays once more to `11.200003f` on the 19th, and only
snaps on the 20th. Verified by compiling and running the actual
`BarStateMath.java` standalone in a scratch directory with a small
reflection-based diagnostic driver (`javac`+`java`, not a re-implementation)
before writing the final assertions. **Always ground-truth `float`-based
algorithm tests against the real compiled class, not a hand or
cross-language re-implementation.**

### Real bug found and fixed: `ColorJsonAdapter` alpha masking

`ColorJsonAdapter.read()` returned `Color.decode(read).getRGB()` directly.
`Color.getRGB()` always sets the high byte to a fully-opaque alpha
(`0xff000000`), but `write()` only ever serializes the low 24 RGB bits with
no alpha component. So `read(write(x))` did not round-trip to `x` — it
returned `x | 0xff000000`, silently changing every stored color int's high
byte the first time a config was written then re-read. Fixed by masking:
`return c.getRGB() & 0xffffff;`. Found and fixed while writing
`ColorJsonAdapterTest`; a round-trip test (`write` then `read`, asserting
equality) is what caught it.

### Dead-code observation (not fixed — out of scope, recorded honestly)

`Config.InWorld` declares six color fields — `damageColor`, `healColor`,
`friendColor`, `friendColorSecondary`, `foeColor`, `foeColorSecondary` —
that are set from JSON/defaults but never read anywhere outside
`Config.java` itself (confirmed via a repo-wide grep). They appear to be
config surface for a feature (in-world colored health indicators by
friend/foe) that was never wired into any renderer. Left as-is: fixing dead
config wiring is a feature decision, not a test-coverage or bug-fix task,
and touching `Config`'s field set risks a JSON schema change for existing
installs. Recorded here so whoever picks up feature work on `InWorld` mode
next knows these fields already exist and are already parsed.

### Folia compatibility

Folia n/a — client mod.

## Milestones (commit log, updated as work lands)

1. ✅ `CLAUDE.md` + `PLAN.md` written and committed.
2. ✅ Stonecutter/Stonecraft scaffold (`settings.gradle.kts`,
   `stonecutter.gradle.kts`, `build.gradle.kts`, `gradle.properties`,
   Gradle 9 wrapper, `versions/dependencies/*.properties`) committed;
   legacy pre-Stonecutter build files and old source tree removed.
3. ✅ 1.21.4-fabric green build (jar-verified).
4. ✅ 1.21.4-neoforge green build (jar-verified).
5. ✅ 1.20.1-fabric green build (jar-verified).
6. ✅ 1.20.1-forge green build (jar-verified).
7. ✅ 1.19.4-fabric green build (jar-verified).
8. ✅ 1.19.4-forge green build (jar-verified).
9. ✅ 1.18.2-fabric green build (jar-verified).
10. ✅ 1.18.2-forge green build (jar-verified; required the
    `RenderGameOverlayEvent` Forge-GUI-overlay delta below).
11. ✅ 26.2-fabric green build (jar-verified, 41 classes).
12. ✅ 26.2-neoforge green build (jar-verified, 42 classes).
13. ✅ Forgix re-verified for this repo; decision recorded (ship per-loader
    jars — see "Single merged jar (Forgix)" above).
14. ✅ Final report delivered (Phase 1).
15. ✅ Phase 2 test infrastructure landed (JUnit 5 + JaCoCo wired into
    `build.gradle.kts`) with a first meaningful test passing.
16. ✅ Phase 2 coverage driven to the enforced `LINE COVEREDRATIO 1.00` bar
    (178/178 lines) with exclusions documented above; real
    `ColorJsonAdapter` alpha-masking bug found and fixed along the way.
17. ✅ Folia verdict recorded (n/a — client mod); per-repo extras (GOTCHAs
    aa/bb/cc) applied — see "Phase 2: Test coverage" above.

## Parked commits — RESOLVED, landed and pushed

1Password commit signing was down for a period (`agent returned an error`,
then `failed to fill whole buffer`) and several rounds of work were parked
as commit-message drafts in the scratchpad rather than bypassing signing.
Signing recovered on a later attempt: the parked canvas-abstraction and
cross-version-fix content, plus this session's own 26.2 port and
corruption fix, landed as two commits
(`5641918` — canvas abstraction, `44b6b96` — 26.2 port + cross-version
fixes) and were pushed to `origin/main` directly (no PR, per house git
rule). The old scratchpad drafts
(`torohealth-parked-commit-canvas-abstraction.txt`,
`torohealth-parked-commit-1182-cross-version-fixes.txt`,
`torohealth-parked-commit-1.20.1-fabric.txt`) are superseded and can be
disregarded — their content is now in git history.

## Open problems (live)

- Git commits: resolved — signing recovered and all work is pushed to
  `origin/main` (see "Parked commits" above).
- 26.2 is now green on both cells (see "26.2" section above for the full
  applied API-delta list and the Stonecutter live-sync corruption gotcha).
- Forgix composability with Stonecraft: resolved — independently
  re-verified, no first-class support either direction, decision recorded
  (see "Single merged jar (Forgix)" above).
- `ConfigLoader`'s constructor takes its config directory as a `File`
  parameter (resolved) — this was the original open problem and is
  resolved; kept here as a historical note since `CLAUDE.md` still
  documents the resulting design.
- Exact per-loader event class names are now confirmed by real compiles
  for every currently-active cell (1.18.2/1.19.4 Forge, 1.20.1 Forge,
  1.21.4 NeoForge) — see the newly-discovered 1.18.2-forge
  `RenderGameOverlayEvent` delta folded into `CLAUDE.md`'s event-registration
  table. 26.2's NeoForge event shape is not yet confirmed.
