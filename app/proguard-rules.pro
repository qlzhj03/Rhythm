# Preserve line numbers and source file names for readable crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Preserve Signature and Annotations for Retrofit, Room, and Gson reflection
-keepattributes Signature, *Annotation*, InnerClasses, EnclosingMethod

# ──────────────────────────────
# Gson serialization models
# ──────────────────────────────

# Keep model classes used for Gson serialization in shared settings, playlists, etc.
-keep class com.qlzhj.rhythm.core.data.shared.data.model.** { *; }
-keep class com.qlzhj.rhythm.core.model.** { *; }

# Keep PlaylistImportExportUtils inner data classes (PlaylistExportData, PlaylistSongEntry)
# used for JSON playlist export/import via Gson reflection
-keep class com.qlzhj.rhythm.core.data.util.PlaylistImportExportUtils$* { *; }

# Keep GitHub API, Rhythm lyrics API, and other network response models
-keep class com.qlzhj.rhythm.core.network.network.** { *; }

# Keep ColorExtractor models used for Theme parsing (Gson targets)
-keep class com.qlzhj.rhythm.core.designsystem.utils.ExtractedColors { *; }

# Keep updater local state models (Gson targets)
-keep class com.qlzhj.rhythm.core.ui.shared.presentation.viewmodel.DownloadState { *; }

# Keep the streaming catalog cache (persisted to disk with Gson): without this R8 renames its
# fields to a, b, c..., so the file's JSON keys depend on the R8 naming of each release build.
-keep class com.qlzhj.rhythm.feature.streaming.data.repository.StreamingCatalogCache { *; }
# Same for the checkpoint of an interrupted library fetch and the provider songs it stores.
-keep class com.qlzhj.rhythm.feature.streaming.data.repository.LibraryFetchCheckpoint { *; }
-keep class com.qlzhj.rhythm.feature.streaming.data.provider.ProviderSong { *; }

# Keep PlaybackEvent inside PlaybackStatsRepository (nested Gson serialization target)
-keep class com.qlzhj.rhythm.core.data.shared.data.repository.PlaybackStatsRepository$PlaybackEvent { *; }

# Keep classes and fields annotated with @SerializedName to prevent Gson reflection issues
-keep class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ──────────────────────────────
# Room Database Configuration
# ──────────────────────────────

# Keep database entities and DAOs specifically so Room can generate and link them
-keep class com.qlzhj.rhythm.core.data.features.local.data.database.entity.** { *; }
-keep class * implements androidx.room.RoomDatabase
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Dao class * { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Database class * { *; }

# ──────────────────────────────
# Third-Party Libraries (No native rules in AAR/JAR)
# ──────────────────────────────

# jaudiotagger (metadata extraction relies heavily on reflection and dynamic class loading)
-keep class org.jaudiotagger.** { *; }
-dontwarn org.jaudiotagger.**

# Netty/Ktor (Netty has complex runtime lookups)
-keep class io.netty.** { *; }
-dontwarn io.netty.**
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# ──────────────────────────────
# Warnings Suppression
# ──────────────────────────────
-dontwarn java.lang.invoke.**
-dontwarn javax.annotation.**
-dontwarn org.codehaus.mojo.animal_sniffer.**
-dontwarn java.awt.**
-dontwarn javax.sound.**
-dontwarn javax.swing.**
# kotlinx.serialization (PR-7: :core:network DTOs migrated from Gson).
# The library ships consumer rules; these keep our DTO serializers against R8
# field renaming, mirroring the official kotlinx.serialization template.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep,includedescriptorclasses class com.qlzhj.rhythm.core.network.network.**$$serializer { *; }
-keepclassmembers class com.qlzhj.rhythm.core.network.network.** {
    *** Companion;
}
-keepclasseswithmembers class com.qlzhj.rhythm.core.network.network.** {
    kotlinx.serialization.KSerializer serializer(...);
}
