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
| 26.2 (newest stable) | ☐ | — | ☐ |
| 1.21.4 | ☐ | — | ☐ |
| 1.20.1 | ☐ | ☐ | — |
| 1.19.4 | ☐ | ☐ | — |
| 1.18.2 | ☐ | ☐ | — |

Newest stable MC confirmed live against
`https://meta.fabricmc.net/v2/versions/game` at task start: **26.2**
(calendar versioning — this is not a typo for 1.21.4/1.26 etc).

Porting order: **1.21.4-fabric → 1.21.4-neoforge → 1.20.1-fabric →
1.20.1-forge → 1.19.4-fabric → 1.19.4-forge → 1.18.2-fabric →
1.18.2-forge → 26.2-fabric → 26.2-neoforge** (newest-with-most-precedent
first via `neoforge-1.21.8`, then walk backwards; 26.2 last since it is
expected to need its own port per gotcha (c)).

## 26.2

Not yet attempted. Expected blocker per house guidance: `GuiGraphics` is
removed from the render pipeline in 26.2 (HUD/world rendering moved to
`extractRenderState()` populating a `GuiRenderState`/render-state object
consumed later, off the render thread). This is a bigger rewrite than a
version bump — `Hud`, `BarDisplay`, `EntityDisplay`, and the GUI half of
`HealthBarRenderer` would all need a render-state-extraction rewrite, not
just new Stonecutter predicates. Will attempt after 1.21.4 is green;
recording live findings here rather than skipping silently.

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

1. ☐ `CLAUDE.md` + `PLAN.md` written and committed (this commit).
2. ☐ Stonecutter/Stonecraft scaffold (`settings.gradle.kts`,
   `stonecutter.gradle.kts`, `build.gradle.kts`, `gradle.properties`,
   Gradle 9 wrapper, `versions/dependencies/*.properties`) committed;
   legacy pre-Stonecutter build files and old source tree removed.
3. ☐ 1.21.4-fabric green build.
4. ☐ 1.21.4-neoforge green build.
5. ☐ 1.20.1-fabric green build.
6. ☐ 1.20.1-forge green build.
7. ☐ 1.19.4-fabric green build.
8. ☐ 1.19.4-forge green build.
9. ☐ 1.18.2-fabric green build.
10. ☐ 1.18.2-forge green build.
11. ☐ 26.2 attempted; result (green or documented blocker) recorded here.
12. ☐ Forgix re-verified for this repo; decision recorded.
13. ☐ Final report delivered.

## Open problems (live)

- `ConfigLoader`'s constructor currently resolves its own config directory
  via loader APis (`FMLPaths`/`FabricLoader`), which are unavailable on
  `src/main`'s classpath. Plan: change constructor to take a `File
  configDir` parameter, resolved by each loader's client entry point.
- `HoldingWeaponUpdater` uses NeoForge-specific `Tags.Items.MELEE_WEAPON_TOOLS`;
  needs a portable `instanceof`-based rewrite (see `CLAUDE.md`).
- Exact per-loader event class names for 1.18.2/1.19.4 Forge and for
  1.20.1 Forge are design-intent only until confirmed by a real compile —
  do not trust the table in `CLAUDE.md` blindly when porting; update it
  once each cell compiles.
