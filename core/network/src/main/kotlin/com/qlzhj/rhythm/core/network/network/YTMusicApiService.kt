package com.qlzhj.rhythm.core.network.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Service interface for YouTube Music API
 * This service provides fallback image fetching for artists, albums, and tracks
 * when other services fail or when local artwork is not available.
 */
interface YTMusicApiService {
    
    /**
     * Search for content on YouTube Music
     */
    @POST("youtubei/v1/search")
    suspend fun search(
        @Query("key") apiKey: String = "AIzaSyC9XL3ZjWddXya6X74dJoCTL-WEYFDNX30",
        @Body request: YTMusicSearchRequest
    ): Response<YTMusicSearchResponse>
    
    /**
     * Get artist information including thumbnails
     */
    @POST("youtubei/v1/browse")
    suspend fun getArtist(
        @Query("key") apiKey: String = "AIzaSyC9XL3ZjWddXya6X74dJoCTL-WEYFDNX30",
        @Body request: YTMusicBrowseRequest
    ): Response<YTMusicArtistResponse>
    
    /**
     * Get album information including cover art
     */
    @POST("youtubei/v1/browse")
    suspend fun getAlbum(
        @Query("key") apiKey: String = "AIzaSyC9XL3ZjWddXya6X74dJoCTL-WEYFDNX30",
        @Body request: YTMusicBrowseRequest
    ): Response<YTMusicAlbumResponse>
}

// -------------------- Request DTOs --------------------

@Serializable
data class YTMusicSearchRequest(
    val context: YTMusicContext,
    val query: String = "",
    val params: String? = null // For filtering search results (artists, albums, songs)
)

@Serializable
data class YTMusicBrowseRequest(
    val context: YTMusicContext,
    val browseId: String = "")

@Serializable
data class YTMusicContext(
    val client: YTMusicClient
)

@Serializable
data class YTMusicClient(
    val clientName: String = "WEB_REMIX",
    val clientVersion: String = "1.20241211.01.00",
    val hl: String = "en",
    val gl: String = "US"
)

// -------------------- Response DTOs --------------------

@Serializable
data class YTMusicSearchResponse(
    val contents: YTMusicSearchContents? = null)

@Serializable
data class YTMusicSearchContents(
    val tabbedSearchResultsRenderer: YTMusicTabbedSearchResults? = null)

@Serializable
data class YTMusicTabbedSearchResults(
    val tabs: List<YTMusicSearchTab>? = null)

@Serializable
data class YTMusicSearchTab(
    val tabRenderer: YTMusicTabRenderer? = null)

@Serializable
data class YTMusicTabRenderer(
    val content: YTMusicSectionListRenderer? = null)

@Serializable
data class YTMusicSectionListRenderer(
    val sectionListRenderer: YTMusicSectionListContents? = null)

@Serializable
data class YTMusicSectionListContents(
    val contents: List<YTMusicMusicShelfRenderer>? = null)

@Serializable
data class YTMusicMusicShelfRenderer(
    val musicShelfRenderer: YTMusicShelfContents? = null)

@Serializable
data class YTMusicShelfContents(
    val contents: List<YTMusicResponsiveListItemRenderer>? = null)

@Serializable
data class YTMusicResponsiveListItemRenderer(
    val musicResponsiveListItemRenderer: YTMusicListItem? = null)

@Serializable
data class YTMusicListItem(
    val flexColumns: List<YTMusicFlexColumn>? = null,
    val thumbnail: YTMusicThumbnailContainer? = null,
    val navigationEndpoint: YTMusicNavigationEndpoint? = null,
    val menu: YTMusicMenu? = null)

@Serializable
data class YTMusicFlexColumn(
    val musicResponsiveListItemFlexColumnRenderer: YTMusicFlexColumnContent? = null)

@Serializable
data class YTMusicFlexColumnContent(
    val text: YTMusicText? = null)

@Serializable
data class YTMusicText(
    val runs: List<YTMusicTextRun>? = null)

@Serializable
data class YTMusicTextRun(
    val text: String? = null,
    val navigationEndpoint: YTMusicNavigationEndpoint? = null)

@Serializable
data class YTMusicNavigationEndpoint(
    val browseEndpoint: YTMusicBrowseEndpoint? = null)

@Serializable
data class YTMusicBrowseEndpoint(
    val browseId: String? = null)

@Serializable
data class YTMusicThumbnailContainer(
    val musicThumbnailRenderer: YTMusicThumbnailRenderer? = null)

@Serializable
data class YTMusicThumbnailRenderer(
    val thumbnail: YTMusicThumbnails? = null)

@Serializable
data class YTMusicThumbnails(
    val thumbnails: List<YTMusicThumbnail>? = null)

@Serializable
data class YTMusicThumbnail(
    val url: String? = null,
    val width: Int? = null,
    val height: Int? = null)

@Serializable
data class YTMusicMenu(
    val menuRenderer: YTMusicMenuRenderer? = null)

@Serializable
data class YTMusicMenuRenderer(
    val items: List<YTMusicMenuItem>? = null)

@Serializable
data class YTMusicMenuItem(
    val menuNavigationItemRenderer: YTMusicMenuNavigationItemRenderer? = null)

@Serializable
data class YTMusicMenuNavigationItemRenderer(
    val navigationEndpoint: YTMusicNavigationEndpoint? = null)

@Serializable
data class YTMusicArtistResponse(
    val contents: YTMusicArtistContents? = null)

@Serializable
data class YTMusicArtistContents(
    val singleColumnBrowseResultsRenderer: YTMusicSingleColumnRenderer? = null)

@Serializable
data class YTMusicSingleColumnRenderer(
    val tabs: List<YTMusicArtistTab>? = null)

@Serializable
data class YTMusicArtistTab(
    val tabRenderer: YTMusicArtistTabRenderer? = null)

@Serializable
data class YTMusicArtistTabRenderer(
    val content: YTMusicSectionListRenderer? = null)

@Serializable
data class YTMusicAlbumResponse(
    val contents: YTMusicAlbumContents? = null)

@Serializable
data class YTMusicAlbumContents(
    val singleColumnBrowseResultsRenderer: YTMusicSingleColumnRenderer? = null)

/**
 * Extract artist image URL from search response
 */
fun YTMusicSearchResponse.extractArtistImageUrl(): String? {
    NetworkLog.d("YTMusicExtract", "Starting artist image extraction")
    
    // Navigate through the search response structure based on actual API format
    val tabs = contents?.tabbedSearchResultsRenderer?.tabs
    NetworkLog.d("YTMusicExtract", "Found ${tabs?.size ?: 0} tabs")
    
    tabs?.forEachIndexed { tabIndex, tab ->
        // The structure is: tabRenderer.content.sectionListRenderer.contents
        val sectionContents = tab.tabRenderer?.content?.sectionListRenderer?.contents
        NetworkLog.d("YTMusicExtract", "Tab $tabIndex has ${sectionContents?.size ?: 0} section contents")
        
        sectionContents?.forEachIndexed { sectionIndex, section ->
            val items = section.musicShelfRenderer?.contents
            NetworkLog.d("YTMusicExtract", "Section $sectionIndex has ${items?.size ?: 0} items")
            
            items?.forEachIndexed { itemIndex, item ->
                val listItem = item.musicResponsiveListItemRenderer
                
                // Check if this is an artist result by looking for navigationEndpoint with browseId starting with "UC"
                val directBrowseId = listItem?.navigationEndpoint?.browseEndpoint?.browseId
                val flexColumnBrowseIds = listItem?.flexColumns?.mapNotNull { column ->
                    column.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.mapNotNull { run ->
                        run.navigationEndpoint?.browseEndpoint?.browseId
                    }
                }?.flatten()
                
                val isMostLikelyArtist = directBrowseId?.startsWith("UC") == true ||
                    flexColumnBrowseIds?.any { it.startsWith("UC") } == true
                
                NetworkLog.d("YTMusicExtract", "Item $itemIndex: directBrowseId=$directBrowseId, flexBrowseIds=$flexColumnBrowseIds, isArtist=$isMostLikelyArtist")
                
                if (isMostLikelyArtist) {
                    // Extract thumbnail URL - try different paths
                    val thumbnails = listItem.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails
                    NetworkLog.d("YTMusicExtract", "Found ${thumbnails?.size ?: 0} thumbnails for artist item")
                    
                    // Filter and prioritize higher quality thumbnails (resolution >200px for artists)
                    val thumbnail = thumbnails
                        ?.filter { !it.url.isNullOrEmpty() && (it.width ?: 0) >= 200 }
                        ?.maxByOrNull { it.width ?: 0 }
                        ?: thumbnails?.filter { !it.url.isNullOrEmpty() }?.maxByOrNull { it.width ?: 0 }
                    
                    NetworkLog.d("YTMusicExtract", "Best thumbnail: ${thumbnail?.url} (${thumbnail?.width}x${thumbnail?.height})")
                    
                    if (thumbnail?.url?.isNotEmpty() == true) {
                        // Enhance image quality by modifying URL parameters if it's a YouTube image
                        val enhancedUrl = if (thumbnail.url.contains("googleusercontent.com") || thumbnail.url.contains("ytimg.com")) {
                            // Remove size restrictions and request high quality
                            thumbnail.url.replace("=w\\d+-h\\d+".toRegex(), "=w800-h800")
                                .replace("=s\\d+".toRegex(), "=s800")
                                .replace("-c-k-c0x00ffffff-no-rj", "-c-k-c0x00ffffff-no-rj-mo")
                        } else {
                            thumbnail.url
                        }
                        NetworkLog.d("YTMusicExtract", "Enhanced thumbnail URL: $enhancedUrl")
                        return enhancedUrl
                    }
                } else {
                    // Even if not an artist, let's log what we have for debugging
                    val thumbnails = listItem?.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails
                    if (!thumbnails.isNullOrEmpty()) {
                        NetworkLog.d("YTMusicExtract", "Non-artist item $itemIndex has ${thumbnails.size} thumbnails: ${thumbnails.first().url}")
                    }
                }
            }
        }
    }
    
    NetworkLog.d("YTMusicExtract", "No artist image found")
    return null
}

/**
 * Extract album cover art URL from search response
 */
fun YTMusicSearchResponse.extractAlbumImageUrl(): String? {
    val thumbnails = contents?.tabbedSearchResultsRenderer?.tabs
        ?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents
        ?.firstOrNull()?.musicShelfRenderer?.contents
        ?.firstOrNull()?.musicResponsiveListItemRenderer
        ?.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails
    
    // Filter for higher quality album covers
    val thumbnail = thumbnails
        ?.filter { !it.url.isNullOrEmpty() && (it.width ?: 0) >= 300 }
        ?.maxByOrNull { it.width ?: 0 }
        ?: thumbnails?.filter { !it.url.isNullOrEmpty() }?.maxByOrNull { it.width ?: 0 }
    
    return thumbnail?.url?.let { url ->
        // Enhance image quality for album covers
        if (url.contains("googleusercontent.com") || url.contains("ytimg.com")) {
            url.replace("=w\\d+-h\\d+".toRegex(), "=w1000-h1000")
                .replace("=s\\d+".toRegex(), "=s1000")
                .replace("-c-k-c0x00ffffff-no-rj", "-c-k-c0x00ffffff-no-rj-mo")
        } else {
            url
        }
    }
}

/**
 * Extract browse ID for detailed artist information
 */
fun YTMusicSearchResponse.extractArtistBrowseId(): String? {
    val tabs = contents?.tabbedSearchResultsRenderer?.tabs
    
    tabs?.forEach { tab ->
        val sectionContents = tab.tabRenderer?.content?.sectionListRenderer?.contents
        
        sectionContents?.forEach { section ->
            val items = section.musicShelfRenderer?.contents
            
            items?.forEach { item ->
                val listItem = item.musicResponsiveListItemRenderer
                
                // Try to get browse ID from navigation endpoint first
                val directBrowseId = listItem?.navigationEndpoint?.browseEndpoint?.browseId
                if (!directBrowseId.isNullOrEmpty() && directBrowseId.startsWith("UC")) {
                    return directBrowseId
                }
                
                // Try to get browse ID from flex columns (artist name link)
                listItem?.flexColumns?.forEach { column ->
                    val runs = column.musicResponsiveListItemFlexColumnRenderer?.text?.runs
                    runs?.forEach { run ->
                        val browseId = run.navigationEndpoint?.browseEndpoint?.browseId
                        if (!browseId.isNullOrEmpty() && browseId.startsWith("UC")) {
                            return browseId
                        }
                    }
                }
                
                // Try to get browse ID from menu items
                listItem?.menu?.menuRenderer?.items?.forEach { menuItem ->
                    val browseId = menuItem.menuNavigationItemRenderer?.navigationEndpoint?.browseEndpoint?.browseId
                    if (!browseId.isNullOrEmpty() && browseId.startsWith("UC")) {
                        return browseId
                    }
                }
            }
        }
    }
    
    return null
}

/**
 * Extract browse ID for detailed album information
 */
fun YTMusicSearchResponse.extractAlbumBrowseId(): String? {
    return contents?.tabbedSearchResultsRenderer?.tabs
        ?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents
        ?.firstOrNull()?.musicShelfRenderer?.contents
        ?.firstOrNull()?.musicResponsiveListItemRenderer
        ?.navigationEndpoint?.browseEndpoint?.browseId
}

/**
 * Extract high-quality artist thumbnail from artist response
 */
fun YTMusicArtistResponse.extractArtistThumbnail(): String? {
    // YouTube Music artist pages often have thumbnails in the header
    val thumbnails = contents?.singleColumnBrowseResultsRenderer?.tabs
        ?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents
        ?.firstOrNull()?.musicShelfRenderer?.contents
        ?.firstOrNull()?.musicResponsiveListItemRenderer
        ?.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails
    
    // Prioritize higher quality thumbnails and enhance URL
    val thumbnail = thumbnails
        ?.filter { !it.url.isNullOrEmpty() && (it.width ?: 0) >= 200 }
        ?.maxByOrNull { it.width ?: 0 }
        ?: thumbnails?.filter { !it.url.isNullOrEmpty() }?.maxByOrNull { it.width ?: 0 }
    
    return thumbnail?.url?.let { url ->
        // Enhance image quality by modifying URL parameters
        if (url.contains("googleusercontent.com") || url.contains("ytimg.com")) {
            url.replace("=w\\d+-h\\d+".toRegex(), "=w800-h800")
                .replace("=s\\d+".toRegex(), "=s800")
                .replace("-c-k-c0x00ffffff-no-rj", "-c-k-c0x00ffffff-no-rj-mo")
        } else {
            url
        }
    }
}

/**
 * Extract high-quality album cover from album response
 */
fun YTMusicAlbumResponse.extractAlbumCover(): String? {
    val thumbnails = contents?.singleColumnBrowseResultsRenderer?.tabs
        ?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents
        ?.firstOrNull()?.musicShelfRenderer?.contents
        ?.firstOrNull()?.musicResponsiveListItemRenderer
        ?.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails
    
    // Prioritize higher quality album covers
    val thumbnail = thumbnails
        ?.filter { !it.url.isNullOrEmpty() && (it.width ?: 0) >= 300 }
        ?.maxByOrNull { it.width ?: 0 }
        ?: thumbnails?.filter { !it.url.isNullOrEmpty() }?.maxByOrNull { it.width ?: 0 }
    
    return thumbnail?.url?.let { url ->
        // Enhance image quality for album covers (higher resolution than artists)
        if (url.contains("googleusercontent.com") || url.contains("ytimg.com")) {
            url.replace("=w\\d+-h\\d+".toRegex(), "=w1000-h1000")
                .replace("=s\\d+".toRegex(), "=s1000")
                .replace("-c-k-c0x00ffffff-no-rj", "-c-k-c0x00ffffff-no-rj-mo")
        } else {
            url
        }
    }
}
