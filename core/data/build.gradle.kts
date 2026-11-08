// Data layer: AppSettings, Room database, repositories, MediaStore scan and
// backup. Unique package root com.qlzhj.rhythm.core.data (2026-11 rehome).
// Depends on :core:network for API-enabled gating types only.

plugins {
    id("rhythm.android.library")
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.qlzhj.rhythm.core.data"
}

dependencies {
    api(project(":core:common"))
    api(project(":core:model"))
    api(project(":core:network"))
    // BackupRestoreManager resolves user-facing strings through the shared
    // resource center (PR-5 moved it here from :app).
    implementation(project(":core:designsystem"))

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.paging)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.com.google.code.gson.gson)
    // PlaylistImportExportUtils (relocated from :feature:local in B1 PR-8)
    // writes playlist exports through the SAF document provider.
    implementation(libs.androidx.documentfile)
    implementation(libs.org.jetbrains.kotlinx.coroutines.core)
    testImplementation(libs.junit)
}
