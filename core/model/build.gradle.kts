// Pure domain models (Song/Album/Artist/Playlist, streaming models, toLibrary*
// mappers). Parcelable models require kotlin-parcelize; api(:core:common) because
// model public signatures expose helper types from :core:common.

plugins {
    id("rhythm.android.library")
    id("kotlin-parcelize")
}

android {
    namespace = "chromahub.rhythm.app"
}

dependencies {
    api(project(":core:common"))
    // Model presets serialize via Gson and carry @Immutable UI-state annotations.
    implementation(libs.com.google.code.gson.gson)
    implementation(libs.androidx.core.ktx)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.runtime)
}
