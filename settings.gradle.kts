pluginManagement {
    // Convention plugins for the multi-module build live in this included build
    // (see docs/multi-module-refactor-plan.md). AGP 9.x built-in Kotlin only —
    // never declare org.jetbrains.kotlin.android anywhere in the toolchain.
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "Rhythm"
include(":app")
include(":macrobenchmark")
