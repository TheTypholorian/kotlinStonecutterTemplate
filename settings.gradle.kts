enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

// This should match the folder name of the project, or else IDEA may complain (see https://youtrack.jetbrains.com/issue/IDEA-317606)
rootProject.name = "kotlinStonecutterTemplate"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.fabricmc.net")
        maven("https://maven.kikugie.dev/snapshots")
        maven("https://maven.kikugie.dev/releases")
    }
}

dependencyResolutionManagement {
    versionCatalogs {
        register("libs") {
            from(files("libs.versions.toml"))
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("dev.kikugie.stonecutter") version "0.9"
}

stonecutter {
    create(rootProject) {
        fun match(loader: String, vararg versions: String) = versions
            .forEach { version("mc${it.replace('.', '_')}_$loader", it).buildscript = "build.$loader.gradle.kts" }

        match("fabric", "1.20", "1.21", "1.21.11", "26.1")
        match("neoforge", "1.21", "1.21.11", "26.1")

        vcsVersion = "mc1_21_fabric"
    }
}