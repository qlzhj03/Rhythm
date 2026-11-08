import java.util.Properties

val localProperties =
    Properties().also { props ->
        val f = rootProject.file("local.properties")
        if (f.exists()) props.load(f.inputStream())
    }

plugins {
    id("rhythm.android.application")
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    // Note: the androidx.baselineprofile plugin is AGP 8.x-only and is not used;
    // baseline profiles were dropped on 2026-11-07 (stale pre-split profile deleted).
    id("kotlin-parcelize")
    id("rhythm.hilt")
//    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.qlzhj.rhythm.app"
    // compileSdk / minSdk / targetSdk / desugaring / JVM 21 / testOptions /
    // lint-off all come from the rhythm.android.application convention plugin.

    defaultConfig {
        applicationId = "com.qlzhj.rhythm"

        val overrideVersionCode = project.findProperty("versionCodeOverride")?.toString()?.toIntOrNull()
        val overrideVersionName = project.findProperty("versionNameOverride")?.toString()
        versionCode = overrideVersionCode ?: 555001306
        versionName = overrideVersionName ?: "5.5.500.1306 Beta"

        val overrideReleaseDate = project.findProperty("releaseDateOverride")?.toString()
        buildConfigField("String", "RELEASE_DATE", "\"${overrideReleaseDate ?: "2026-10-17"}\"")

        val isNightly = project.findProperty("nightly")?.toString() == "true"
        buildConfigField("boolean", "IS_NIGHTLY", isNightly.toString())

        // Apple Music: fallback token from environment variable (GitHub secrets), local.properties, or fallback
        val appleMusicToken =
            System.getenv("APPLE_MUSIC_FALLBACK_TOKEN")
                ?: localProperties.getProperty("APPLE_MUSIC_FALLBACK_TOKEN")
                ?: "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiIsImtpZCI6IldlYlBsYXlLaWQifQ.eyJpc3MiOiJBTVBXZWJQbGF5IiwiaWF0IjoxNzgxMDMyODU1LCJleHAiOjE3ODQwNTY4NTUsInJvb3RfaHR0cHNfb3JpZ2luIjpbImFwcGxlLmNvbSJdfQ.fiMFcJWkfSlxKP9NVA0UW9CbItD1Rge0SISuepz203XcpU762OqdCpU9M-YkmtKkjRmaIWtjsfGgqZPrlMonpA"
        buildConfigField("String", "APPLE_MUSIC_FALLBACK_TOKEN", "\"$appleMusicToken\"")

        buildConfigField("String", "FLAVOR", "\"github\"")

        // Feature flags (all features enabled)
        buildConfigField("boolean", "ENABLE_YOUTUBE_MUSIC", "true")
        buildConfigField("boolean", "ENABLE_LYRICALLY_API", "true")
        buildConfigField("boolean", "ENABLE_DEEZER", "true")
        buildConfigField("boolean", "ENABLE_LRCLIB", "true")
        buildConfigField("boolean", "ENABLE_BETTERLYRICS", "true")
        buildConfigField("boolean", "ENABLE_SPOTIFY_SEARCH", "true")
        buildConfigField("boolean", "ENABLE_WIKIPEDIA", "true")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    val signingProperties = getProperties("keystore.properties")
    val releaseSigning =
        if (signingProperties != null) {
            signingConfigs.create("release") {
                keyAlias = signingProperties.property("key_alias")
                keyPassword = signingProperties.property("key_password")
                storePassword = signingProperties.property("store_password")
                storeFile = rootProject.file(signingProperties.property("store_file"))
            }
        } else {
            signingConfigs.getByName("debug")
        }

    defaultConfig {
        // Only arm64-v8a is built and packaged. One APK per variant, no ABI splits,
        // and no merge/packaging work for armeabi-v7a, x86 or x86_64.
        ndk {
            abiFilters += "arm64-v8a"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = releaseSigning
//            ndk {
//                debugSymbolLevel = "SYMBOL_TABLE"
//            }
            // Reproducible builds: disable build timestamp
            if (System.getenv("CI") == "true" || System.getenv("BUILD_REPRODUCIBLE") == "true") {
                // Use a fixed timestamp for reproducible builds
                tasks.configureEach {
                    // Disable timestamps in bundle reports for reproducible builds
                    if (name.contains("BundleReport", ignoreCase = true)) {
                        enabled = false
                    }
                }
            }
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            // isMinifyEnabled = false
            // isDebuggable = true
            signingConfig = releaseSigning
        }
        // Required by the macrobenchmark module for baseline profile generation.
        // Mirrors release (fully minified + signed) so the profile reflects production.
        create("benchmark") {
            initWith(buildTypes.getByName("release"))
            signingConfig = releaseSigning
            matchingFallbacks += listOf("release")
            isDebuggable = false
        }
    }
    kotlin {
        // jvmTarget = JVM_21 is set by the convention plugin (afterEvaluate);
        // only the Material3 Expressive opt-ins are app-specific here.
        compilerOptions {
            freeCompilerArgs.addAll(
                "-opt-in=androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
                "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            )
        }
    }
    buildFeatures {
        compose = true
    }

    dependenciesInfo {
        // Disables dependency metadata when building APKs/Bundles
        includeInApk = false
        // Disables dependency metadata when building Android App Bundles
        includeInBundle = false
    }

    packaging {
        resources {
            merges += "/META-INF/INDEX.LIST"
            merges += "**/io.netty.versions.properties"
        }
    }

    // No ABI splits: abiFilters above already limits every variant to a single
    // arm64-v8a APK, so splits would only add packaging/signing passes.
}

androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            output.outputFileName.set(
                "Rhythm-${android.defaultConfig.versionName}-${variant.name}.apk",
            )
        }
    }
}

dependencies {
    // Multi-module split (PR-2): leaf modules. :app remains the composition root.
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:network"))
    implementation(project(":core:data"))
    implementation(project(":core:media"))
    implementation(project(":core:ui"))
    // PR-5 feature modules (:app is the only module allowed to see them all).
    implementation(project(":feature:local"))
    implementation(project(":feature:streaming"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:widget"))

    implementation(libs.androidx.compose.ui.unit)

    // Core Android dependencies
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.core)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)

    // Compose dependencies
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)

    // Material 3 dependencies
    implementation(libs.androidx.material3.android)
    implementation(libs.androidx.material3.window.size)
    implementation(libs.androidx.window)
    implementation(libs.com.google.android.material)

    // Media3 dependencies
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.dash)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.datasource.okhttp)
    implementation(libs.androidx.media3.exoplayer.midi)
    implementation(libs.org.jellyfin.media3.ffmpeg.decoder)

    // Icons - Material Symbols variable font (res/font/material_symbols_outlined.ttf)
    // Replaces the deprecated material-icons-extended library for faster build times
    implementation(libs.androidx.palette.ktx)

    // Glance for modern widgets
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

    // Physics-based animations
    implementation(libs.androidx.compose.animation)
    //noinspection GradleDependency
    implementation(libs.androidx.compose.animation.graphics)
    implementation(libs.androidx.compose.animation.core)
    implementation(libs.reorderables)
    implementation(libs.com.google.zxing.core)

    // Navigation
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    // Permissions
    implementation(libs.com.google.accompanist.accompanist.permissions)

    // Fragment
    implementation(libs.androidx.fragment.ktx)

    // MediaRouter for Android media output switching
    implementation(libs.androidx.mediarouter)

    // Coil for image loading
    implementation(libs.io.coil.kt.coil.compose)

    // Audio metadata editing
    implementation(libs.net.jthink.jaudiotagger)
    implementation(libs.taglib)

    // Network
    implementation(libs.com.squareup.retrofit2.retrofit)
    implementation(libs.com.squareup.retrofit2.converter.gson)
    implementation(libs.com.squareup.okhttp3.okhttp)
    implementation(libs.com.squareup.okhttp3.logging.interceptor)
    implementation(libs.com.google.code.gson.gson)
//    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
//    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")

    // Coroutines for async operations
    implementation(libs.org.jetbrains.kotlinx.coroutines.core)
    implementation(libs.org.jetbrains.kotlinx.coroutines.android)
    implementation(libs.androidx.foundation.layout)

    // WorkManager for background tasks
    implementation(libs.androidx.work.runtime.ktx)

    // Room database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.paging)
    ksp(libs.androidx.room.compiler)

    // Jetpack Paging 3
    implementation(libs.androidx.paging.runtime.ktx)
    implementation(libs.androidx.paging.compose)

    // Profile installer: no-op without a baseline profile asset (profiles dropped 2026-11-07).
    implementation(libs.androidx.profileinstaller)

    // Testing
    testImplementation(libs.junit)
    // Benchmark variant only: merges androidx.benchmark IsolationActivity into
    // the manifest so AndroidBenchmarkRunner (targeting this app) can launch it
    // during macrobenchmark runs. Kept after baseline profile generation was
    // dropped (2026-11-07); the benchmark buildType serves startup measurement.
    add("benchmarkImplementation", libs.androidx.benchmark.junit4)
    testImplementation(libs.com.squareup.okhttp3.mockwebserver3)
    // Real org.json for JVM tests; android.jar only has stubs.
    testImplementation(libs.org.json)
    // No androidTest source set exists (AGENTS.md §1); only debug preview tooling.
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

composeCompiler {
    includeComposeMappingFile.set(false)
}

fun getProperties(fileName: String): Properties? {
    val file = rootProject.file(fileName)
    return if (file.exists()) {
        Properties().also { properties ->
            file.inputStream().use { properties.load(it) }
        }
    } else {
        null
    }
}

fun Properties.property(key: String) = this.getProperty(key) ?: "$key missing"
