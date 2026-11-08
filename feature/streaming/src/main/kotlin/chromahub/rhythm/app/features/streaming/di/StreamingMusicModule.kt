package chromahub.rhythm.app.features.streaming.di

import chromahub.rhythm.app.features.streaming.data.repository.StreamingMusicRepositoryImpl
import com.qlzhj.rhythm.core.data.core.domain.repository.StreamingMusicRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt bindings for the streaming feature (multi-module split PR-6, was a
 * manual double-checked singleton object). StreamingMusicRepositoryImpl
 * exposes its own @Inject constructor.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class StreamingMusicModule {

    @Binds
    @Singleton
    abstract fun bindStreamingMusicRepository(impl: StreamingMusicRepositoryImpl): StreamingMusicRepository
}
