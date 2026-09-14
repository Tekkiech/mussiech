/*
 * Mussiech (2026)
 * GPL-3.0 License | Contributors: see git history
 */

package tech.tekkiech.mussiech.ui.screens.navidrome

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import tech.tekkiech.mussiech.LocalPlayerAwareWindowInsets
import tech.tekkiech.mussiech.LocalPlayerConnection
import tech.tekkiech.mussiech.R
import tech.tekkiech.mussiech.extensions.toMediaItem
import tech.tekkiech.mussiech.playback.queues.ListQueue
import tech.tekkiech.mussiech.ui.component.MediaMetadataListItem
import tech.tekkiech.mussiech.viewmodels.NavidromeAlbumViewModel
import tech.tekkiech.mussiech.viewmodels.NavidromeDetailUiState

@Composable
fun NavidromeAlbumScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: NavidromeAlbumViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    NavidromeDetailScreenContent(uiState)
}

@Composable
internal fun NavidromeDetailScreenContent(uiState: NavidromeDetailUiState) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsStateWithLifecycle()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()

    when (uiState) {
        is NavidromeDetailUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        is NavidromeDetailUiState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = stringResource(R.string.error_unknown))
            }
        }

        is NavidromeDetailUiState.Content -> {
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(LocalPlayerAwareWindowInsets.current),
            ) {
                item {
                    Text(
                        text = uiState.title,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(16.dp),
                    )
                }
                items(items = uiState.tracks, key = { it.id }) { track ->
                    val index = uiState.tracks.indexOf(track)
                    MediaMetadataListItem(
                        mediaMetadata = track,
                        isActive = track.id == mediaMetadata?.id,
                        isPlaying = isPlaying,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clickable {
                                    playerConnection.playQueue(
                                        ListQueue(
                                            title = uiState.title,
                                            items = uiState.tracks.map { it.toMediaItem() },
                                            startIndex = index,
                                        ),
                                    )
                                },
                    )
                }
            }
        }
    }
}
