// Cross-feature shared UI: bottom sheets, dialogs, generic list/sort menus,
// settings primitives (Tuner switches, search bar) and media-quality badges.
// Files keep their original packages (chromahub.rhythm.app.shared.presentation.*)
// so every import site stays untouched — see docs/multi-module-refactor-plan.md
// PR-5 "zero import churn" policy established in PR-3.

plugins {
    id("rhythm.android.library.compose")
    id("rhythm.hilt")
}

android {
    namespace = "chromahub.rhythm.app"
}

dependencies {
    api(project(":core:model"))
    api(project(":core:designsystem"))
    api(project(":core:common"))
    // Shared sheets read AppSettings and surface playback-domain helpers
    // (AudioFormatDetector, QueueEntry, MediaUtils) — see PR-5 design notes.
    implementation(project(":core:data"))
    implementation(project(":core:media"))

    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.io.coil.kt.coil.compose)
    // EditAlbumSheet consumes Deezer search responses; QueueBottomSheet reorders
    // via sh.calvin.reorderable — both come from libraries their old host pulled
    // transitively, so declare them here.
    implementation(libs.com.squareup.retrofit2.retrofit)
    implementation(libs.com.squareup.okhttp3.okhttp)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.reorderables)
    implementation(libs.com.google.accompanist.accompanist.permissions)
    implementation(libs.com.google.code.gson.gson)
    implementation(libs.org.jetbrains.kotlinx.coroutines.android)
    testImplementation(libs.junit)
}
