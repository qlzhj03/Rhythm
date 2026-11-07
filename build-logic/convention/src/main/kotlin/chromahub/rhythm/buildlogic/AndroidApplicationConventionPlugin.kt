package chromahub.rhythm.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType

/**
 * Convention for the :app module (adopted by :app in the post-PR-7 cleanup —
 * the standalone script previously mirrored [configureAndroidBase] by hand).
 * Compose for :app comes from the compose plugin directly; a matching
 * application-compose convention can be added later if duplication grows.
 */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")
            val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
            extensions.configure<ApplicationExtension>("android") {
                configureAndroidBase(this)
                defaultConfig.targetSdk = libs.findVersion("targetSdk").get().requiredVersion.toInt()
                buildFeatures.buildConfig = true
            }
        }
    }
}
