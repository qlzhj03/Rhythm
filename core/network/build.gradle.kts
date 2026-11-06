// Network stack (NetworkClient, API services, NetworkLog) + AppWorkerRegistry.
// Settings access goes through NetworkSettingsProvider (implemented by AppSettings
// in :core:data) so this module never depends on :core:data.

plugins {
    id("rhythm.android.library")
}

android {
    namespace = "chromahub.rhythm.app"
}

dependencies {
    api(project(":core:common"))
    implementation(libs.com.squareup.retrofit2.retrofit)
    implementation(libs.com.squareup.retrofit2.converter.gson)
    implementation(libs.com.squareup.okhttp3.okhttp)
    implementation(libs.com.squareup.okhttp3.logging.interceptor)
    implementation(libs.com.google.code.gson.gson)
    implementation(libs.org.jetbrains.kotlinx.coroutines.core)
    implementation(libs.androidx.work.runtime.ktx)
}
