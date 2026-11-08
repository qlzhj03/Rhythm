package chromahub.rhythm.app.features.local.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.core.net.toUri

@Entity(tableName = "artists")
data class ArtistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val artworkUri: String?,
    val numberOfAlbums: Int,
    val numberOfTracks: Int,
    val groupByAlbumArtist: Boolean
)

fun ArtistEntity.toArtist(): com.qlzhj.rhythm.core.model.Artist {
    return com.qlzhj.rhythm.core.model.Artist(
        id = id,
        name = name,
        artworkUri = artworkUri?.let { (it).toUri() },
        numberOfAlbums = numberOfAlbums,
        numberOfTracks = numberOfTracks
    )
}
