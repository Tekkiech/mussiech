/*
 * Mussiech (2026)
 * GPL-3.0 License | Contributors: see git history
 */

package tech.tekkiech.mussiech.models

import androidx.compose.runtime.Immutable

@Immutable
data class NavidromeAlbum(
    val id: String,
    val title: String,
    val artistName: String?,
    val songCount: Int,
    val thumbnailUrl: String?,
)

@Immutable
data class NavidromePlaylist(
    val id: String,
    val name: String,
    val owner: String?,
    val songCount: Int,
    val thumbnailUrl: String?,
)
