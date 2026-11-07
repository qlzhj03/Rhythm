// Widget feature: Glance widgets (music/lyrics/cookie/stats), their receivers,
// the Glance updater, widget resources and the QS tile service.

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
    implementation(project(":core:media"))
    implementation(project(":core:network"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.io.coil.kt.coil.compose)
    implementation(libs.org.jetbrains.kotlinx.coroutines.android)
}
