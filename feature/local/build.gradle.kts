// Local-library + playback UI feature: library screens, player screens,
// player/lyrics components, MusicViewModel and the local data/repository glue.
// NOTE: the plan's :feature:player was merged here (multi-module split PR-5):
// local scaffolds embed the MiniPlayer while player screens consume
// MusicViewModel, which is unresolvable as two separate modules without
// massive interface surgery. Packages are kept unchanged (zero import churn).

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
    implementation(project(":core:data"))
    implementation(project(":core:media"))
    implementation(project(":core:network"))
    implementation(project(":core:ui"))

    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    // ExpressivePlayerScreen dynamic color math (Hct)
    implementation(libs.com.google.android.material)
    implementation(libs.androidx.paging.runtime.ktx)
    implementation(libs.androidx.paging.compose)
    implementation(libs.io.coil.kt.coil.compose)
    implementation(libs.com.google.accompanist.accompanist.permissions)
    implementation(libs.com.google.code.gson.gson)
    // MusicRepositoryImpl consumes Deezer/ iTunes retrofit responses directly.
    implementation(libs.com.squareup.retrofit2.retrofit)
    // LibraryScreen SAF browsing (documentfile) and playlist drag-reorder (reorderables).
    implementation(libs.androidx.documentfile)
    implementation(libs.reorderables)
    implementation(libs.com.squareup.okhttp3.okhttp)
    implementation(libs.net.jthink.jaudiotagger)
    // MediaScanEngine reads metadata via taglib and runs Room transactions.
    implementation(libs.taglib)
    implementation(libs.androidx.room.ktx)
    implementation(libs.org.jetbrains.kotlinx.coroutines.android)
    testImplementation(libs.junit)
    testImplementation(libs.com.squareup.okhttp3.mockwebserver3)
    testImplementation(libs.org.json)
}
