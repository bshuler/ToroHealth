# ToroHealth Damage Indicators

Client-side Minecraft mod. Damage given, received, or mitigated pops off the
entity as a floating number; a HUD panel (entity portrait, name, health bar,
armor) appears for whatever living entity is in the player's crosshairs; and
optional in-world health bars float above entities. Purely visual — no server
component, works on vanilla servers.

Colors, display style, HUD position, and in-world bar behavior are all
configurable via `config/torohealth.json` (hot-reloaded when edited on disk)
or the in-game config screen.

![Screenshot](https://i.imgur.com/C9oBhZ5.png)

## Supported versions

One codebase builds every cell below via
[Stonecutter](https://stonecutter.kikugie.dev/)/[Stonecraft](https://github.com/meza/stonecraft):

| Minecraft | Fabric | NeoForge | Forge |
|---|---|---|---|
| 26.2 | ✅ | ✅ | — |
| 1.21.4 | ✅ | ✅ | — |
| 1.20.1 | ✅ | — | ✅ |
| 1.19.4 | ✅ | — | ✅ |
| 1.18.2 | ✅ | — | ✅ |

Quilt runs the Fabric jars natively. `./gradlew chiseledBuild` produces all
ten jars; each cell's jar is verified to contain real compiled classes, not
just a green build log.

## Relationship to upstream

This is a fork of [ToroCraft/ToroHealth](https://github.com/ToroCraft/ToroHealth)
(GPL-3.0, preserved here). Upstream maintained one branch per Minecraft
version and went dormant in 2022 (its newest work, a NeoForge 1.21.8 port,
landed in 2025). This fork re-derives the mod from that newest upstream
logic and folds every version back into a **single multi-loader codebase**,
then extends it to Minecraft 26.x — including the 26.2 GUI-pipeline rewrite
(`GuiGraphics` removal) that no upstream branch reaches.

Bugs found and fixed here (none exist in a form upstream could take as a PR —
they are in fork-written or fork-ported code):

- **26.2 entity portrait drawn at the wrong position** (fork's
  `PlatformHudCanvas` port of the rewritten GUI pipeline) — caught by the
  pixel-level client gametest below.
- **Four dead config options**: the friend/foe colour settings parsed but
  were never applied after the port; restored.
- **Config colour round-trip bug**: colours saved in a format the loader
  could not re-read; adapter fixed so the config file survives a
  save/edit/reload cycle.

## Testing

This repo is tested at three tiers, all wired into `./gradlew check` and CI
(`.github/workflows/build.yml` — the fork added CI; upstream had none):

1. **Headless unit tests + 100% line coverage gate** (JUnit 5 + JaCoCo) over
   all pure logic (config, colour adapter, file watcher, health-bar state
   math). The coverage gate is enforced, not aspirational — `check` fails if
   it regresses. The suite runs in **every one of the ten version cells**.
2. **Loaded-game tests** (`fabric-loader-junit`): a real bootstrapped
   Minecraft and a real Fabric loader validate the processed
   `fabric.mod.json`, dependency ranges, and that the mod's math agrees with
   the live game (real registries, real entity max-health values, real
   items). Runs on all five Fabric cells.
3. **Client gametests** (`fabric-client-gametest-api-v1`, 1.21.4 + 26.2):
   boots a **real Minecraft client** — real window, real GL, real world,
   a real pig in the crosshair — screenshots the frame, and asserts the HUD
   pixels actually reached the screen. This tier found the portrait and
   colour bugs above; no headless test can see them. CI runs it under xvfb
   with software GL and uploads the screenshots as artifacts.

Forge/NeoForge cells run tier 1 only: Fabric's test harnesses are
loader-specific, and NeoForge's equivalent is not reachable from this build
toolchain (documented in `PLAN.md`).

## Building

```bash
unset JAVA_HOME            # let Gradle's toolchain resolver pick per-cell JDKs
./gradlew chiseledBuild    # build + test every cell
./gradlew :1.21.4-fabric:build          # one cell
./gradlew "Set active project to 26.2-fabric" && ./gradlew runClient
```

Development notes for humans and AI agents live in `CLAUDE.md`; the full
modernization log and per-cell status in `PLAN.md`.

## License

GPL-3.0, inherited from upstream. See `LICENSE`.
