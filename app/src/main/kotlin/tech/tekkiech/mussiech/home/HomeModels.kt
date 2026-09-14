/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package tech.tekkiech.mussiech.home

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.google.common.collect.ImmutableList
import tech.tekkiech.mussiech.constants.QuickPicks
import tech.tekkiech.mussiech.constants.QuickPicksDisplayMode
import tech.tekkiech.mussiech.db.entities.LocalItem
import tech.tekkiech.mussiech.db.entities.Song
import moe.rukamori.archivetune.innertube.pages.HomePage
import tech.tekkiech.mussiech.models.NavidromeAlbum
import tech.tekkiech.mussiech.models.NavidromePlaylist
import tech.tekkiech.mussiech.podcast.PodcastPlaybackRequest

sealed interface HomeScreenState {
    data object Loading : HomeScreenState

    @Immutable
    data class Success(
        val uiState: HomeUiState,
    ) : HomeScreenState

    data object Empty : HomeScreenState

    @Immutable
    data class Error(
        @StringRes val messageResId: Int,
    ) : HomeScreenState
}

@Immutable
data class HomeUiState(
    val quickPicks: ImmutableList<Song>,
    val speedDialItems: ImmutableList<LocalItem>,
    val forgottenFavorites: ImmutableList<Song>,
    val keepListening: ImmutableList<LocalItem>,
    val navidromePlaylists: ImmutableList<NavidromePlaylist>,
    val navidromeHomeAlbums: ImmutableList<NavidromeAlbum>,
    val remoteQuickPicks: HomePage.Section?,
    val quickPicksMode: QuickPicks,
    val quickPicksDisplayMode: QuickPicksDisplayMode,
    val showTonalBackdrop: Boolean,
    val isRefreshing: Boolean,
)

sealed interface HomeAction {
    data object Refresh : HomeAction

    data class OpenRemoteItem(
        val itemId: String,
    ) : HomeAction
}

sealed interface HomeEvent {
    data class OpenPodcast(
        val browseId: String,
    ) : HomeEvent

    @Immutable
    data class PlayPodcastEpisode(
        val request: PodcastPlaybackRequest,
    ) : HomeEvent
}
