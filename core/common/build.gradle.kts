// Pure Kotlin utilities shared by every module (moved from :app util/utils).
// Unique package root com.qlzhj.rhythm.core.common — enforced by
// android.uniquePackageNames=true (see docs/unique-package-names-design.md).

plugins {
    id("rhythm.android.library")
    id("kotlin-parcelize")
}

android {
    namespace = "com.qlzhj.rhythm.core.common"
}

dependencies {
    implementation(libs.com.google.code.gson.gson)
    implementation(libs.org.jetbrains.kotlinx.coroutines.core)
    implementation(libs.androidx.core.ktx)
    // Lyrics/player helper utilities in this module speak media3 types
    // (MediaMetadata, Player, CommandButton). Full exoplayer/session artifacts
    // come with :core:media in a later PR; these are the shared API jars.
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)

    testImplementation(libs.junit)
}
