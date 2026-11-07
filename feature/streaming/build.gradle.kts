// Self-hosted streaming feature: Subsonic/Navidrome/Jellyfin/NetEase providers,
// streaming repository, StreamingMusicViewModel and the streaming screens.

plugins {
    id("rhythm.android.library.compose")
}

android {
    namespace = "chromahub.rhythm.app"
}

dependencies {
    api(project(":core:model"))
    api(project(":core:designsystem"))
    api(project(":core:common"))
    implementation(project(":core:data"))
    implementation(project(":core:network"))
    implementation(project(":core:media"))
    implementation(project(":core:ui"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.io.coil.kt.coil.compose)
    implementation(libs.com.google.code.gson.gson)
    implementation(libs.com.google.zxing.core)
    // Providers build OkHttp calls directly (Jellyfin/Subsonic headers, artwork fetch).
    implementation(libs.com.squareup.okhttp3.okhttp)
    implementation(libs.org.jetbrains.kotlinx.coroutines.android)
    testImplementation(libs.junit)
    testImplementation(libs.com.squareup.okhttp3.mockwebserver3)
    testImplementation(libs.org.json)
}
