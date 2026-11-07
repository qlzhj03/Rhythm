import java.util.Properties

// The benchmark build type must be signed with the same key as the :app
// benchmark variant (release signing) or the instrumentation is denied
// ("signature matching the target") — PR-7 baseline profile regeneration.
val signingProperties = Properties().also { props ->
    val f = rootProject.file("keystore.properties")
    if (f.exists()) props.load(f.inputStream())
}

plugins {
    id("com.android.test")
    // Note: org.jetbrains.kotlin.android is NOT applied — AGP 9.0+ has built-in Kotlin support.
    // Note: androidx.baselineprofile plugin omitted — incompatible with AGP 9.x.
    // The BaselineProfileRule in benchmark-macro-junit4 generates the profile at
    // runtime without requiring the Gradle plugin on either module.
}

android {
    namespace = "chromahub.rhythm.app.macrobenchmark"
    compileSdk = 37

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }


    defaultConfig {
        minSdk = 26
        targetSdk = 37
        // AndroidBenchmarkRunner's isolation flow (IsolationActivity + target
        // process kill) conflicts with non-self-instrumenting generation and
        // aggressive OEM task killers; AndroidJUnitRunner works for
        // BaselineProfileRule, which only dumps the ART reference profile (PR-7).
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Points the test module at the :app module it instruments. Without
    // targetApplicationId the instrumentation never associates with the target
    // app, so BaselineProfileRule only ever captured library classes (PR-7 fix).
    targetProjectPath = ":app"

    // NOTE (PR-7): android.experimental.self-instrumenting was removed. It made
    // android:targetPackage point at the test module itself, so
    // BaselineProfileRule dumped the test app's profile and captured only
    // androidx/kotlin library classes — never the app's own classes. Without it
    // AGP derives the target package from targetProjectPath's applicationId,
    // which is what manual (no baselineprofile-gradle-plugin) generation needs.

    buildTypes {
        // The benchmark build type instruments the :app's "benchmark" variant
        create("benchmark") {
            isDebuggable = true
            if (signingProperties.isNotEmpty()) {
                signingConfigs.create("release") {
                    keyAlias = signingProperties.property("key_alias")
                    keyPassword = signingProperties.property("key_password")
                    storePassword = signingProperties.property("store_password")
                    storeFile = rootProject.file(signingProperties.property("store_file"))
                }
                signingConfig = signingConfigs.getByName("release")
            } else {
                signingConfig = getByName("debug").signingConfig
            }
            matchingFallbacks += listOf("release")
        }
    }

}

dependencies {
    // benchmark-macro-junit4 doesn't transitively include benchmark-junit4 in 1.3.4;
    // AndroidBenchmarkRunner lives in benchmark-junit4 and must be added explicitly.
    implementation(libs.androidx.benchmark.junit4)
    // Align lifecycle with :app (2.11): the test APK's older ReportFragment
    // otherwise shadows the app's version in the target process and crashes
    // IsolationActivity with NoSuchFieldError (PR-7).
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.uiautomator)
    implementation(libs.androidx.junit)
}

// Only enable the benchmark build type; disable all others to keep build fast
androidComponents {
    beforeVariants(selector().all()) {
        it.enable = it.buildType == "benchmark"
    }
}

fun Properties.property(key: String) = this.getProperty(key) ?: "$key missing"
