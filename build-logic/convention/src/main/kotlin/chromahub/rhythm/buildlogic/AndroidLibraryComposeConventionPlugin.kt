package chromahub.rhythm.buildlogic

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidExtension

/**
 * Compose convention for library modules. Applies the base library convention and
 * injects the exact Compose dependency set :app used before the split, including
 * the deliberate pins (material3 1.5.0-beta01, foundation-layout / animation 1.12.1).
 */
class AndroidLibraryComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("rhythm.android.library")
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

            extensions.configure<LibraryExtension>("android") {
                buildFeatures.compose = true
            }
            extensions.configure<ComposeCompilerGradlePluginExtension>("composeCompiler") {
                includeComposeMappingFile.set(false)
            }

            // Material3 Expressive opt-ins only make sense in Compose modules; adding
            // them in the base convention produces "unresolved marker" warnings elsewhere.
            afterEvaluate {
                extensions.configure<KotlinAndroidExtension>("kotlin") {
                    compilerOptions {
                        freeCompilerArgs.addAll(
                            "-opt-in=androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
                            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
                        )
                    }
                }
            }

            val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
            dependencies {
                add("implementation", platform(libs.findLibrary("androidx-compose-bom").get()))
                add("implementation", libs.findLibrary("androidx-ui").get())
                add("implementation", libs.findLibrary("androidx-ui-graphics").get())
                add("implementation", libs.findLibrary("androidx-ui-tooling-preview").get())
                add("implementation", libs.findLibrary("androidx-compose-ui-unit").get())
                add("implementation", libs.findLibrary("androidx-material3-android").get())
                add("implementation", libs.findLibrary("androidx-material3-window-size").get())
                add("implementation", libs.findLibrary("androidx-foundation-layout").get())
                add("implementation", libs.findLibrary("androidx-compose-animation").get())
                add("implementation", libs.findLibrary("androidx-compose-animation-core").get())
                add("implementation", libs.findLibrary("androidx-compose-animation-graphics").get())
                add("debugImplementation", libs.findLibrary("androidx-ui-tooling").get())
            }
        }
    }
}
