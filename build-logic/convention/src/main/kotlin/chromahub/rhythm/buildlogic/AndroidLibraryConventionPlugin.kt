package chromahub.rhythm.buildlogic

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Base convention for every Android library module: SDK levels, Java/Kotlin 21,
 * desugaring, Material3 Expressive opt-ins, unit-test and lint posture.
 */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.library")
            extensions.configure<LibraryExtension>("android") {
                configureAndroidBase(this)
            }
        }
    }
}
