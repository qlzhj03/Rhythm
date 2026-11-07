package chromahub.rhythm.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidExtension

/**
 * Shared Android configuration for all rhythm.* convention plugins. Mirrors the
 * settings :app carried before the multi-module split (now applied to :app
 * itself via the application convention plugin).
 */
internal fun Project.configureAndroidBase(common: CommonExtension) {
    val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

    common.compileSdk = libs.findVersion("compileSdk").get().requiredVersion.toInt()

    // AGP 9.x DSL interfaces expose these as properties (no Action overloads in
    // binary plugin code), so configure via direct property access.
    common.defaultConfig.minSdk = libs.findVersion("minSdk").get().requiredVersion.toInt()

    common.compileOptions.isCoreLibraryDesugaringEnabled = true
    common.compileOptions.sourceCompatibility = JavaVersion.VERSION_21
    common.compileOptions.targetCompatibility = JavaVersion.VERSION_21

    // AGP 9.x built-in Kotlin: Kotlin compiler options live on the project-level
    // "kotlin" extension (KGP type: KotlinAndroidExtension). Note that :app's
    // `android { kotlin { ... } }` script block is lexical nesting only — the
    // block resolves to this same project-level extension. Material3 opt-ins are
    // added by the compose convention only, so non-UI modules stay warning-free.
    afterEvaluate {
        extensions.configure<KotlinAndroidExtension>("kotlin") {
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_21)
            }
        }
    }

    common.testOptions.unitTests.isReturnDefaultValues = true

    // Lint is intentionally disabled repo-wide (see AGENTS.md §2): abortOnError off
    // plus a hard kill of all lint* tasks keeps library builds at :app latency.
    common.lint.abortOnError = false
    common.lint.checkReleaseBuilds = false
    tasks.configureEach {
        if (name.startsWith("lint")) {
            enabled = false
        }
    }

    dependencies {
        add("coreLibraryDesugaring", libs.findLibrary("androidx-desugar-jdk-libs").get())
    }
}
