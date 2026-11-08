// Settings feature: all settings screens plus their dialogs and customization
// pickers (theme/player/widget/miniplayer), settings search and the Tuner
// switch primitives shared with :core:ui consumers.

plugins {
    id("rhythm.android.library.compose")
}

android {
    namespace = "com.qlzhj.rhythm.feature.settings"
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
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.io.coil.kt.coil.compose)
    implementation(libs.com.google.accompanist.accompanist.permissions)
    implementation(libs.com.google.code.gson.gson)
    implementation(libs.org.jetbrains.kotlinx.coroutines.android)
    testImplementation(libs.junit)
}
