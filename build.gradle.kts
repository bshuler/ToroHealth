import gg.meza.stonecraft.mod

plugins {
    id("gg.meza.stonecraft")
    jacoco
}

dependencies {
    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Tier 1 "loaded game" testing (Fabric cells only). fabric-loader-junit stands a real
    // Fabric loader up inside the JUnit worker, which is what makes it legal to call
    // SharedConstants.tryDetectVersion() + Bootstrap.bootStrap() and then assert against
    // genuinely loaded game data - see src/test/java/.../LoadedGameTest.java and PLAN.md.
    // NeoForge/Forge have no Loom-compatible equivalent (see the junit-fml note below).
    if (mod.isFabric) {
        testImplementation("net.fabricmc:fabric-loader-junit:0.19.3")
    }
}

// Tier 3 "real client" testing: Fabric client gametests.
//
// Tier 1 (fabric-loader-junit, above) stands a loader and a bootstrapped Minecraft up
// inside a JUnit worker, but there is no window, no render thread and no game loop - so
// it cannot answer the question that matters most for this mod: when the player actually
// looks at a mob in a running client, does the HUD reach the screen? Everything under
// display/** and render/** is excluded from the coverage bundle for exactly that reason.
// Tier 3 launches a real client, creates a world, spawns a mob, aims at it and diffs the
// framebuffer. See PLAN.md "Tier 3".
//
// Gated `>=1.21.4` because fabric-client-gametest-api-v1 does not exist in the Fabric API
// bundles for 1.20.1/1.19.4/1.18.2 (the module first appears around fabric-api 0.106 /
// MC 1.21.2), and `isFabric` because it is Fabric-only - the NeoForge equivalent needs
// ModDevGradle, which this repo does not use (same reason as the junit-fml note below).
// The two supported cells ship v4.1.1 (1.21.4) and v6.0.0 (26.2); the test source uses
// only the API surface those two have in common, verified with javap against both jars,
// so one un-preprocessed file compiles on both. Note 4.1.1's TestSingleplayerContext has
// getClientWorld() and 6.0.0's has getConnection() instead - neither is used here.
val clientGameTestSupported = mod.isFabric &&
    stonecutter.eval(stonecutter.current.version, ">=1.21.4")

if (clientGameTestSupported) {
    // Loom is not applied through this script's `plugins {}` block, so there is no
    // generated `fabricApi { }` accessor; configure the extension by type.
    extensions.configure<net.fabricmc.loom.api.fabricapi.FabricApiExtension>("fabricApi") {
        configureTests {
            createSourceSet.set(true)
            modId.set("torohealth-gametest")
            // enableGameTests would wire `check` to dependsOn(runGameTest), dragging a
            // real *server* launch into every `./gradlew check`. This mod is a client
            // HUD with no server gametests, so it stays off.
            enableGameTests.set(false)
            enableClientGameTests.set(true)
            // eula deliberately left at its default (false): that is Mojang's EULA and is
            // not accepted automatically here. Only the dedicated-server variant needs it.
        }
    }

    // Loom's `mods` container starts empty and createSourceSet adds an entry for the
    // gametest mod, which would otherwise be the *only* classpath group - leaving the
    // mod under test ungrouped in the dev run. Register it explicitly; the test asserts
    // FabricLoader can see it, so a regression here fails loudly rather than silently
    // running the harness against no mod at all.
    extensions.configure<net.fabricmc.loom.api.LoomGradleExtensionAPI>("loom") {
        mods.create(mod.prop("id", "torohealth")).sourceSet(sourceSets["main"])
    }

    // Stonecraft configures `loom.runs.all { }`, and because that is an `all` hook it also
    // catches the `clientGameTest` config Loom creates later. Two collisions result, both
    // corrected here for that one run config only:
    //
    // 1. Stonecraft appends `--username=developer` to every client-environment run while
    //    Loom's configureTests passes `--username Player0`. Minecraft's joptsimple parser
    //    treats `username` as single-valued, so the client dies with
    //    MultipleArgumentsForOptionException before it starts. Loom's value is kept: this
    //    is not a human's dev session. `runClient` still logs in as `developer`.
    //
    // 2. The same hook calls setRunDir on every config, overwriting the
    //    `build/run/clientGameTest` Loom had just set with the repo-root `../../run` - the
    //    developer's own dev directory. That is destructive, not untidy: GameTestSettings'
    //    clearRunDirectory conventions to `true` and Loom registers a
    //    `deleteGameTestRunDir` Delete task over runConfig.getRunDir(), so launching the
    //    gametest would wipe the developer's dev world, options and screenshots. Restoring
    //    Loom's value also keeps the run dir per-cell, so 1.21.4 and 26.2 don't overwrite
    //    each other's screenshots. The Delete task reads getRunDir() lazily at realization,
    //    so correcting it here redirects the deletion too.
    //
    // afterEvaluate because Loom creates the run config while this script body is still
    // executing, and Stonecraft's `all` action fires at creation time.
    //
    // Written against the Provider API (`programArguments` / `runDirectory`), not the older
    // `programArgs` / `runDir` pair the sibling FlightHud repo uses: this repo is on
    // Architectury Loom 1.17.491 (FlightHud is on 1.14.476), where `getProgramArgs()`
    // returns an immutable copy, so `programArgs.removeAll { }` fails at configuration time
    // with a bare UnsupportedOperationException out of ImmutableList.set.
    afterEvaluate {
        extensions.configure<net.fabricmc.loom.api.LoomGradleExtensionAPI>("loom") {
            runs.named("clientGameTest") {
                programArguments.set(
                    programArguments.get().filterNot { it.startsWith("--username=") }
                )
                runDirectory.set(layout.buildDirectory.dir("run/clientGameTest"))
            }
        }
    }

    // No explicit fabric-client-gametest-api-v1 dependency: Loom's configureTests adds
    // none, but Stonecraft already puts the whole fabric-api bundle on the compile
    // classpath and that bundle depends on every module including this one, so both cells
    // resolve it transitively. Declaring it again would risk pinning a second version.

    // Nothing in the normal build graph compiles a `gametest` source set, so a compile
    // error there would sit undetected until someone launched the game. Compiling it is
    // cheap and needs no display; make `check` do it. Running it stays opt-in via
    // runClientGameTest.
    tasks.named("check") {
        dependsOn(tasks.named("compileGametestJava"))
    }
}

// NeoForge's transitive net.neoforged.fancymodloader:junit-fml (<=9.0.18) auto-registers a
// LauncherSessionListener that looks up a run-config file "mainargs.txt" via a relative path
// that doesn't resolve under Gradle's test-worker working directory, failing `test` at
// JUnit-launcher startup with NoSuchFileException: mainargs.txt. Forcing the upstream-fixed
// junit-fml 10.0+ doesn't work either (NoClassDefFoundError:
// net/neoforged/fml/startup/StartupArgs - it needs a newer FML core than these NeoForge
// releases ship). This repo's tests are plain pure-logic JUnit tests that need no FML
// bootstrap at all, so junit-fml is excluded from the test runtime classpath. Same approach
// as the sibling critical-orientation / EasierVillagerTrading / FlightHud /
// simple-utilities-mod / ToroHealth repos.
//
// Caveat, recorded 2026-08-13 so nobody re-derives it: excluding junit-fml is the
// right call *for these repos*, not a universal one. junit-fml is precisely
// NeoForge's own loaded-test bootstrap - it is what stands FML up so a test can
// run against a real, loaded game. NeoForge's supported loaded-test path
// (`neoForge { unitTest { enable(); testedMod = ... } }`, the `testframework`
// artifact, `@ExtendWith(EphemeralTestServerProvider.class)`, `runGameTestServer`)
// is ModDevGradle-only, and this repo builds on Architectury Loom via Stonecraft,
// so that path is unavailable here regardless. Excluding junit-fml therefore costs
// nothing today. If a cell is ever migrated to ModDevGradle, this exclusion must
// be revisited before writing any loaded NeoForge test - it would silently disable
// the very bootstrap such a test depends on.
if (mod.isNeoforge) {
    // NeoForge's own POM pulls in fancymodloader's junit-fml, which registers a JUnit
    // Platform LauncherSessionListener that unconditionally expects a mainargs.txt
    // launch-args file (only ever produced for NeoForge's in-game gametest run
    // configs). This project's tests are plain, loader-agnostic math/config tests -
    // not in-game gametests - so pull it back off the test classpath. Mirrors the
    // fix already landed in the sibling FlightHud/critical-flight-details mods;
    // added proactively even though the active test project during Phase 2 is
    // 1.21.4-fabric (unaffected) - this repo does have 1.21.4-neoforge/26.2-neoforge
    // cells, and this keeps `test`/`check` from breaking on them later.
    configurations.named("testRuntimeClasspath") {
        exclude(group = "net.neoforged.fancymodloader", module = "junit-fml")
    }
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

// JaCoCo scope: everything that touches a live Minecraft/loader singleton, GLFW,
// or a rendering primitive (GuiGraphics/PoseStack/HudCanvas) at class-load or call
// time is excluded - merely referencing such a class headless is unsafe/meaningless
// without a running game client. The genuinely pure, loader- and Minecraft-free
// logic (config parsing/persistence, file-watch plumbing, and the extracted
// BarStateMath health/damage-delta state machine) was extracted into (or already
// lived in) classes with zero net.minecraft import and is fully unit-tested; see
// PLAN.md ("Phase 2: Test coverage") for the full per-class reasoning.
val jacocoExcludes = listOf(
    "net/torocraft/torohealth/ToroHealth.class",
    "net/torocraft/torohealth/ToroHealth$*.class",
    "net/torocraft/torohealth/ClientEventHandler.class",
    "net/torocraft/torohealth/ClientEventHandler$*.class",
    "net/torocraft/torohealth/bars/BarState.class",
    "net/torocraft/torohealth/bars/BarState$*.class",
    "net/torocraft/torohealth/bars/BarStates.class",
    "net/torocraft/torohealth/bars/BarStates$*.class",
    "net/torocraft/torohealth/bars/BarParticle.class",
    "net/torocraft/torohealth/bars/BarParticle$*.class",
    "net/torocraft/torohealth/bars/HealthBarRenderer.class",
    "net/torocraft/torohealth/bars/HealthBarRenderer$*.class",
    "net/torocraft/torohealth/bars/ParticleRenderer.class",
    "net/torocraft/torohealth/bars/ParticleRenderer$*.class",
    "net/torocraft/torohealth/client/ConfigScreen.class",
    "net/torocraft/torohealth/client/ConfigScreen$*.class",
    "net/torocraft/torohealth/display/**",
    "net/torocraft/torohealth/render/**",
    "net/torocraft/torohealth/util/RayTrace.class",
    "net/torocraft/torohealth/util/RayTrace$*.class",
    "net/torocraft/torohealth/util/HoldingWeaponUpdater.class",
    "net/torocraft/torohealth/util/HoldingWeaponUpdater$*.class",
    "net/torocraft/torohealth/util/EntityUtil.class",
    "net/torocraft/torohealth/util/EntityUtil$*.class",
)

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
    classDirectories.setFrom(classDirectories.files.map { fileTree(it) { exclude(jacocoExcludes) } })
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.test)
    classDirectories.setFrom(classDirectories.files.map { fileTree(it) { exclude(jacocoExcludes) } })
    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "1.00".toBigDecimal()
            }
        }
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}

// Forge's pack.mcmeta generation task writes into the main source set's resources
// output without declaring that as a tracked task output, which Gradle's task
// validation flags as an undeclared ("implicit") dependency on :compileTestJava
// (which consumes sourceSets.main.output as part of the test compile classpath).
// Declare the dependency explicitly so validation/chiseledBuild doesn't hit a
// task-graph ordering failure. tasks.matching(...) is a live/lazy filter, so
// this is a harmless no-op on loaders that don't have a generatePackMCMetaJson
// task (e.g. Fabric).
tasks.matching { it.name == "compileTestJava" }.configureEach {
    dependsOn(tasks.matching { it.name == "generatePackMCMetaJson" })
}
