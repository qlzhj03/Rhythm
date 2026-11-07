// Playback core: MediaPlaybackService, player engine, DSP/EQ, ReplayGain,
// media workers, and the media-flavored utils. Streaming URL resolution goes
// through StreamingPlaybackResolver (installed by :app / :feature:streaming).

plugins {
    // Compose convention required: OffMainComputation (moved here in PR-4) carries
    // @Composable code — without the compose compiler plugin it compiles but crashes
    // at runtime when invoked from a real composition (start-up crash in LocalNavigation).
    id("rhythm.android.library.compose")
}

android {
    namespace = "chromahub.rhythm.app"
}

dependencies {
    api(project(":core:common"))
    api(project(":core:model"))
    api(project(":core:data"))
    api(project(":core:network"))

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.dash)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.datasource.okhttp)
    implementation(libs.androidx.media3.exoplayer.midi)
    implementation(libs.org.jellyfin.media3.ffmpeg.decoder)
    implementation(libs.io.coil.kt.coil.compose)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.com.google.code.gson.gson)
    implementation(libs.org.jetbrains.kotlinx.coroutines.core)
    implementation(libs.androidx.core.ktx)
    implementation(libs.net.jthink.jaudiotagger)
    implementation(libs.taglib)
    implementation(libs.androidx.palette.ktx)
    implementation(libs.androidx.mediarouter)
    implementation(libs.com.google.android.material)
    testImplementation(libs.junit)
}
