/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package tech.tekkiech.mussiech.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import tech.tekkiech.mussiech.LocalPlayerAwareWindowInsets
import tech.tekkiech.mussiech.LocalPlayerConnection
import tech.tekkiech.mussiech.R
import tech.tekkiech.mussiech.constants.GridThumbnailHeight
import moe.rukamori.archivetune.innertube.models.AlbumItem
import moe.rukamori.archivetune.innertube.models.ArtistItem
import moe.rukamori.archivetune.innertube.models.EpisodeItem
import moe.rukamori.archivetune.innertube.models.PlaylistItem
import moe.rukamori.archivetune.innertube.models.PodcastItem
import tech.tekkiech.mussiech.extensions.toMediaItem
import tech.tekkiech.mussiech.playback.queues.ListQueue
import tech.tekkiech.mussiech.ui.component.IconButton
import tech.tekkiech.mussiech.ui.component.LocalMenuState
import tech.tekkiech.mussiech.ui.component.YouTubeGridItem
import tech.tekkiech.mussiech.ui.component.shimmer.GridItemPlaceHolder
import tech.tekkiech.mussiech.ui.component.shimmer.ShimmerHost
import tech.tekkiech.mussiech.ui.menu.YouTubeAlbumMenu
import tech.tekkiech.mussiech.ui.menu.YouTubeArtistMenu
import tech.tekkiech.mussiech.ui.menu.YouTubePlaylistMenu
import tech.tekkiech.mussiech.ui.utils.backToMain
import tech.tekkiech.mussiech.viewmodels.BrowseViewModel

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    browseId: String?,
    viewModel: BrowseViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val title by viewModel.title.collectAsState()
    val items by viewModel.items.collectAsState()

    val coroutineScope = rememberCoroutineScope()

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = GridThumbnailHeight + 24.dp),
        contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
    ) {
        items?.let { items ->
            items(
                items = items.distinctBy { it.id },
                key = { it.id },
            ) { item ->
                YouTubeGridItem(
                    item = item,
                    isPlaying = isPlaying,
                    fillMaxWidth = true,
                    coroutineScope = coroutineScope,
                    modifier =
                        Modifier
                            .combinedClickable(
                                onClick = {
                                    when (item) {
                                        is AlbumItem -> {
                                            navController.navigate("album/${item.id}")
                                        }

                                        is PlaylistItem -> {
                                            navController.navigate("online_playlist/${item.id}")
                                        }

                                        is ArtistItem -> {
                                            navController.navigate("artist/${item.id}")
                                        }

                                        is PodcastItem -> {
                                            navController.navigate("podcast/${android.net.Uri.encode(item.browseId)}")
                                        }

                                        is EpisodeItem -> {
                                            playerConnection.playQueue(
                                                ListQueue(
                                                    title = item.podcast?.name ?: item.title,
                                                    items = listOf(item.toMediaItem()),
                                                ),
                                            )
                                        }

                                        else -> {
                                            // Do nothing
                                        }
                                    }
                                },
                                onLongClick =
                                    if (item is AlbumItem || item is PlaylistItem || item is ArtistItem) {
                                        {
                                            menuState.show {
                                                when (item) {
                                                    is AlbumItem -> {
                                                        YouTubeAlbumMenu(
                                                            albumItem = item,
                                                            navController = navController,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                    }

                                                    is PlaylistItem -> {
                                                        YouTubePlaylistMenu(
                                                            playlist = item,
                                                            coroutineScope = coroutineScope,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                    }

                                                    is ArtistItem -> {
                                                        YouTubeArtistMenu(
                                                            artist = item,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                    }

                                                    else -> Unit
                                                }
                                            }
                                        }
                                    } else {
                                        null
                                    },
                            ),
                )
            }

            if (items.isEmpty()) {
                items(8) {
                    ShimmerHost {
                        GridItemPlaceHolder(fillMaxWidth = true)
                    }
                }
            }
        }
    }

    TopAppBar(
        title = { Text(title ?: "") },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        },
    )
}
