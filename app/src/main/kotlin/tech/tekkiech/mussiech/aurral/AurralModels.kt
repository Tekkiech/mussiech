/*
 * Mussiech (2026)
 * GPL-3.0 License | Contributors: see git history
 */

package tech.tekkiech.mussiech.aurral

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AurralSearchResponse(
    val scope: String,
    val query: String,
    val count: Int,
    val offset: Int,
    val items: List<AurralSearchItem> = emptyList(),
)

@Serializable
data class AurralSearchItem(
    val type: String,
    val id: String,
    val name: String,
    val sortName: String? = null,
    val image: String? = null,
    val imageUrl: String? = null,
    val artistType: String? = null,
    val disambiguation: String? = null,
    val tags: List<String> = emptyList(),
    val genres: List<String> = emptyList(),
    val inLibrary: Boolean = false,
    val score: Int = 0,
)

/**
 * A single row from `GET /api/requests` - a flat, genuinely heterogeneous feed merging
 * aurral's own activity log with live Lidarr queue state (distinguished by [source]/[type]/
 * [kind], not a single consistent record shape). Every field beyond [id] is nullable/optional
 * on purpose: only two real example rows have been observed (one `type:"activity"`, one
 * `type:"album"`), so this stays a flat permissive shape rather than a sealed hierarchy that
 * would need an exhaustive (and easily wrong) `when` over every possible [type]/[kind].
 */
@Serializable
data class AurralRequestRow(
    val id: String,
    val source: String? = null,
    val type: String? = null,
    val kind: String? = null,
    val title: String? = null,
    val name: String? = null,
    val subtitle: String? = null,
    val status: String? = null,
    val statusLabel: String? = null,
    val requestedAt: String? = null,
    val href: String? = null,
    val playlistId: String? = null,
    val playlistName: String? = null,
    val jobId: String? = null,
    val trackName: String? = null,
    val artistId: String? = null,
    val artistMbid: String? = null,
    val artistName: String? = null,
    val albumId: String? = null,
    val albumMbid: String? = null,
    val albumName: String? = null,
    val mbid: String? = null,
    val image: String? = null,
    val requestedBy: AurralRequestedBy? = null,
    val sourceFilename: String? = null,
    val inQueue: Boolean = false,
    val canReSearch: Boolean = false,
) {
    /** Whichever of [title]/[name] is populated for this row. */
    val displayTitle: String get() = title ?: name ?: artistName.orEmpty()
}

@Serializable
data class AurralRequestedBy(
    val id: Int,
    val username: String,
)

@Serializable
data class AurralRequestArtistBody(
    val foreignArtistId: String,
    val artistName: String,
    val monitorOption: String = "all",
    val rootFolderPath: String,
    val qualityProfileId: Int,
    @SerialName("releaseGroupMbid")
    val releaseGroupMbid: String? = null,
)
