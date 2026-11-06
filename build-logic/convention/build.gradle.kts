plugins {
    `kotlin-dsl`
}

group = "chromahub.rhythm.buildlogic"

// Convention plugins compile against the AGP / KGP APIs only; the consuming build
// resolves the real plugins at runtime (root build.gradle.kts declares them `apply false`).
dependencies {
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin.api)
    compileOnly(libs.compose.compiler.gradle.plugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "rhythm.android.application"
            implementationClass = "chromahub.rhythm.buildlogic.AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "rhythm.android.library"
            implementationClass = "chromahub.rhythm.buildlogic.AndroidLibraryConventionPlugin"
        }
        register("androidLibraryCompose") {
            id = "rhythm.android.library.compose"
            implementationClass = "chromahub.rhythm.buildlogic.AndroidLibraryComposeConventionPlugin"
        }
        register("hilt") {
            id = "rhythm.hilt"
            implementationClass = "chromahub.rhythm.buildlogic.HiltConventionPlugin"
        }
    }
}
