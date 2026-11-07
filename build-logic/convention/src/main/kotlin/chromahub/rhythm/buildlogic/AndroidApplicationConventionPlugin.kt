package chromahub.rhythm.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType

/**
 * Convention for the :app module. Adopting it in :app is deferred to a later PR —
 * until then :app keeps its standalone build script, which must stay in sync with
 * [configureAndroidBase]. Compose for :app comes from the compose plugin directly
 * (a matching application-compose convention can be added when :app migrates).
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
