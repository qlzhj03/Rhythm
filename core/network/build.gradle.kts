// Network stack (NetworkClient, API services, NetworkLog) + AppWorkerRegistry.
// Settings access goes through NetworkSettingsProvider (implemented by AppSettings
// in :core:data) so this module never depends on :core:data.

plugins {
    alias(libs.plugins.kotlin.serialization)
    id("rhythm.android.library")
}

android {
    namespace = "chromahub.rhythm.app"
}

dependencies {
    api(project(":core:common"))
    implementation(libs.com.squareup.retrofit2.retrofit)
    implementation(libs.com.squareup.okhttp3.okhttp)
    implementation(libs.com.squareup.okhttp3.logging.interceptor)
    // PR-7: DTOs are @Serializable; the shared Gson instance in :core:data is separate.
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.com.squareup.retrofit2.converter.kotlinx.serialization)
    implementation(libs.org.jetbrains.kotlinx.coroutines.core)
    implementation(libs.androidx.work.runtime.ktx)
}
