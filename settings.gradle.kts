pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.kikugie.dev/releases")
        maven("https://maven.kikugie.dev/snapshots")
        maven("https://maven.fabricmc.net/")
        maven("https://maven.architectury.dev/")
        maven("https://maven.minecraftforge.net/")
        maven("https://maven.neoforged.net/releases/")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("gg.meza.stonecraft") version "1.12.+"
    id("dev.kikugie.stonecutter") version "0.9.+"
}

stonecutter {
    kotlinController = true
    centralScript = "build.gradle.kts"

    shared {
        fun mc(version: String, vararg loaders: String) {
            for (loader in loaders) {
                version("$version-$loader", version)
            }
        }

        // Target versions and loaders - see PLAN.md for status/rationale per
        // cell. Loader coverage is mandatory: Fabric + NeoForge for 1.20.5+,
        // Fabric + Forge for 1.20.4 and older (Forge ended at the NeoForge
        // split). Quilt is not a separate cell - it runs Fabric jars natively.

        // Newest stable Minecraft per https://meta.fabricmc.net/v2/versions/game
        // (checked live - do not trust training-data memory) is 26.2. Kept
        // commented pending a dedicated toolchain-support pass - see PLAN.md
        // "26.2" section for the live-verified blocker.
        // mc("26.2", "fabric", "neoforge")

        // 1.21.4 - Fabric + NeoForge (last classic-numbered Yarn-mapped target)
        mc("1.21.4", "fabric", "neoforge")
        // 1.20.1 - Fabric + Forge (last major Forge version before NeoForge split)
        mc("1.20.1", "fabric", "forge")
        // 1.19.4 - Fabric + Forge
        mc("1.19.4", "fabric", "forge")
        // 1.18.2 - Fabric + Forge
        mc("1.18.2", "fabric", "forge")

        vcsVersion = "1.21.4-fabric"
    }
    create(rootProject)
}

rootProject.name = "ToroHealth"
