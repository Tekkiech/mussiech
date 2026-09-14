/*
 * Mussiech (2026)
 * GPL-3.0 License | Contributors: see git history
 */

package tech.tekkiech.mussiech.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import tech.tekkiech.mussiech.R
import tech.tekkiech.mussiech.constants.GridThumbnailCornerRadius
import tech.tekkiech.mussiech.constants.ListThumbnailSize
import tech.tekkiech.mussiech.constants.ThumbnailCornerRadius
import tech.tekkiech.mussiech.models.NavidromeAlbum
import tech.tekkiech.mussiech.models.NavidromePlaylist

@Composable
fun NavidromeAlbumGridItem(
    album: NavidromeAlbum,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    GridItem(
        title = album.title,
        subtitle = album.artistName.orEmpty(),
        thumbnailContent = {
            ItemThumbnail(
                thumbnailUrl = album.thumbnailUrl,
                isActive = false,
                isPlaying = false,
                shape = RoundedCornerShape(GridThumbnailCornerRadius),
            )
        },
        modifier = modifier.clickable(onClick = onClick),
    )
}

@Composable
fun NavidromePlaylistListItem(
    playlist: NavidromePlaylist,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    ListItem(
        title = playlist.name,
        subtitle = pluralStringResource(R.plurals.n_song, playlist.songCount, playlist.songCount),
        thumbnailContent = {
            ItemThumbnail(
                thumbnailUrl = playlist.thumbnailUrl,
                isActive = false,
                isPlaying = false,
                shape = RoundedCornerShape(ThumbnailCornerRadius),
                modifier = Modifier.size(ListThumbnailSize),
            )
        },
        modifier = modifier.clickable(onClick = onClick),
    )
}
