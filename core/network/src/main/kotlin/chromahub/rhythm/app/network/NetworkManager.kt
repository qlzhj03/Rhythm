package chromahub.rhythm.app.network

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Network manager for handling API configuration and providing API services
 */
object NetworkManager {
    private const val GITHUB_API_BASE_URL = "https://api.github.com/"
    private const val TIMEOUT_SECONDS = 60L
    
    // GitHub API token (optional - for higher rate limits)
    // For public repos, you can still make unauthenticated API requests but with lower rate limits
    // If you have a token, you can add it here
    private const val GITHUB_TOKEN = "" // Leave empty for unauthenticated requests
    
    /**
     * Create an OkHttpClient with logging and timeout configuration
     */
    private fun createOkHttpClient(): OkHttpClient {
        val loggingInterceptor = NetworkLog.createLoggingInterceptor()
        
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val original = chain.request()
                
                // Build request with GitHub API headers
                val requestBuilder = original.newBuilder()
                    .header("Accept", "application/vnd.github.v3+json")
                    .header("User-Agent", "Rhythm-Music-App/${NetworkBuildInfo.appVersion} (Android)") // Using a consistent User-Agent helps with rate limits
                
                // Add authorization if token is provided
                if (GITHUB_TOKEN.isNotEmpty()) {
                    requestBuilder.header("Authorization", "token $GITHUB_TOKEN")
                }
                
                val request = requestBuilder.build()
                chain.proceed(request)
            }
            .addInterceptor(loggingInterceptor)
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }
    
    /**
     * Create and return a GitHubApiService instance
     */
    fun createGitHubApiService(): GitHubApiService {
        val retrofit = Retrofit.Builder()
            .baseUrl(GITHUB_API_BASE_URL)
            .client(createOkHttpClient())
            .addConverterFactory(Json { ignoreUnknownKeys = true; coerceInputValues = true; explicitNulls = false; encodeDefaults = true }
            .asConverterFactory("application/json".toMediaType()))
            .build()
        
        return retrofit.create(GitHubApiService::class.java)
    }
} 
