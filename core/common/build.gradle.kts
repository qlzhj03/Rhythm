// Pure Kotlin utilities shared by every module (moved from :app util/utils).
// Namespace deliberately duplicates :app (android.uniquePackageNames=false) so
// existing `import chromahub.rhythm.app.R` sites keep resolving while resources
// migrate to :core:designsystem — see docs/multi-module-refactor-plan.md §二.2.

plugins {
    id("rhythm.android.library")
    id("kotlin-parcelize")
}

android {
    namespace = "chromahub.rhythm.app"
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
