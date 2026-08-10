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
list. Work is committed locally but **not yet pushed** — see "Parked
commits" below; 1Password commit signing is currently unavailable in this
environment.

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

## Single merged jar (Forgix)

Not yet independently re-verified for ToroHealth in this session. Prior
finding from the sibling `EasierVillagerTrading` port: Forgix
(`PacifistMC/Forgix`) is actively maintained, but its usage/setup assumes
static, hand-declared per-loader subprojects, which sits awkwardly against
Stonecutter/Stonecraft's dynamically generated `versions/<mc>-<loader>`
subprojects — EVT shipped per-loader jars instead of a merged jar. Default
assumption for ToroHealth is the same (ship per-loader jars from
`versions/*/build/libs/`) unless re-investigation below finds Stonecraft
added first-class Forgix support since then. Will re-check before the
final report and update this section with a live verification, not just a
carry-over assumption.

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
13. ☐ Forgix re-verified for this repo; decision recorded.
14. ☐ Final report delivered.

## Parked commits (1Password commit signing unavailable)

`git commit` failed twice in this environment with two different 1Password
agent errors (`agent returned an error`, then `failed to fill whole
buffer`) — the signing agent is genuinely down right now, not a one-off.
Per the binding git-signing rule, signing was never bypassed; instead the
already-staged work was parked as commit messages + exact retry commands
in the scratchpad, to be replayed on a future human turn once signing
recovers:

1. `/private/tmp/claude-501/-Users-bshuler-code/3309436f-5239-4605-ab94-a3e38563bb44/scratchpad/torohealth-parked-commit-canvas-abstraction.txt` —
   `refactor: introduce HudCanvas abstraction for cross-era HUD rendering`
   (`render/HudCanvas.java`, `render/PlatformHudCanvas.java`,
   `display/BarDisplay.java`, `display/EntityDisplay.java`,
   `display/Hud.java`). Land **first**.
2. `/private/tmp/claude-501/-Users-bshuler-code/3309436f-5239-4605-ab94-a3e38563bb44/scratchpad/torohealth-parked-commit-1182-cross-version-fixes.txt` —
   `fix: port cross-version API deltas so 1.18.2 and 1.19.4/1.20.1-forge build green`
   (`ClientEventHandler.java`, `ToroHealth.java`,
   `bars/HealthBarRenderer.java`, `bars/ParticleRenderer.java`,
   `client/ConfigScreen.java`, `util/RayTrace.java`,
   `stonecutter.gradle.kts`). Land **second** (depends on #1's
   `Hud.java`/`render/` state).
3. `/private/tmp/claude-501/-Users-bshuler-code/3309436f-5239-4605-ab94-a3e38563bb44/scratchpad/torohealth-parked-commit-1.20.1-fabric.txt` —
   an older, narrower parked commit from an earlier session, predates both
   of the above and never landed. Diff its listed fixes
   (`ConfigScreen.java`/`ClientEventHandler.java`/`HealthBarRenderer.java`)
   against the current working tree before replaying — it may already be
   superseded by #1/#2 above.

Once landed, `git push origin main` directly (no PR, per the house git
rule) — nothing here needs review, only unblocking.

## Open problems (live)

- Git commits are currently **parked, not pushed** — see "Parked commits"
  above. Retry on a future turn; do not poll or bypass signing.
- 26.2 is now green on both cells (see "26.2" section above for the full
  applied API-delta list and the Stonecutter live-sync corruption gotcha).
- Forgix composability with Stonecraft not yet independently re-verified
  for this repo (see "Single merged jar (Forgix)" above).
- `ConfigLoader`'s constructor takes its config directory as a `File`
  parameter (resolved) — this was the original open problem and is
  resolved; kept here as a historical note since `CLAUDE.md` still
  documents the resulting design.
- Exact per-loader event class names are now confirmed by real compiles
  for every currently-active cell (1.18.2/1.19.4 Forge, 1.20.1 Forge,
  1.21.4 NeoForge) — see the newly-discovered 1.18.2-forge
  `RenderGameOverlayEvent` delta folded into `CLAUDE.md`'s event-registration
  table. 26.2's NeoForge event shape is not yet confirmed.
