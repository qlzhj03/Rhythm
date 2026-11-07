package chromahub.rhythm.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

/**
 * Hilt convention: official Gradle plugin (for the aggregating task and the ASM
 * superclass rewrite it performs via AGP's transform API) + KSP compiler.
 *
 * Spike-verified with AGP 9.4.1 built-in Kotlin: plugin 2.60.1 does not touch
 * org.jetbrains.kotlin.android (it keys off com.android.base + Variant API), so
 * the historical incompatibility with AGP 9 no longer applies.
 */
class HiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.google.devtools.ksp")
            pluginManager.apply("com.google.dagger.hilt.android")

            val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
            dependencies {
                add("implementation", libs.findLibrary("hilt-android").get())
                add("ksp", libs.findLibrary("hilt-compiler").get())
            }
        }
    }
}
