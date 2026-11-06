// Design system: theme (ui.theme + festive/), RhythmIcons/MaterialSymbolIcon,
// Expressive base components, and the shared resource center (strings/drawable/
// font). Namespace duplicates :app so `chromahub.rhythm.app.R` keeps resolving.

plugins {
    id("rhythm.android.library.compose")
}

android {
    namespace = "chromahub.rhythm.app"
}

dependencies {
    api(project(":core:common"))

    implementation(libs.androidx.core.ktx)
    // Material color science (Hct/dynamic color) used by theme + ColorExtractor
    implementation(libs.com.google.android.material)
    // ColorExtractor caches ExtractedColors via Gson; ExpressiveComponents
    // surfaces media3 Player state in player controls
    implementation(libs.com.google.code.gson.gson)
    implementation(libs.androidx.media3.exoplayer)
    // DevicePosture foldable posture reads androidx.window layout info
    implementation(libs.androidx.window)
    // DragDropList reorderable lazy lists
    implementation(libs.reorderables)
}
