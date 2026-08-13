import gg.meza.stonecraft.mod

plugins {
    id("gg.meza.stonecraft")
    jacoco
}

dependencies {
    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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
