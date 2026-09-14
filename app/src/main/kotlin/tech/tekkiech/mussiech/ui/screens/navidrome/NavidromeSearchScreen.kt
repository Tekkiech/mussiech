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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import tech.tekkiech.mussiech.LocalPlayerConnection
import tech.tekkiech.mussiech.R
import tech.tekkiech.mussiech.extensions.toMediaItem
import tech.tekkiech.mussiech.playback.queues.ListQueue
import tech.tekkiech.mussiech.ui.component.EmptyPlaceholder
import tech.tekkiech.mussiech.ui.component.MediaMetadataListItem
import tech.tekkiech.mussiech.ui.component.NavidromeAlbumGridItem
import tech.tekkiech.mussiech.ui.component.NavidromeArtistListItem
import tech.tekkiech.mussiech.viewmodels.NavidromeSearchUiState
import tech.tekkiech.mussiech.viewmodels.NavidromeSearchViewModel

@Composable
fun NavidromeSearchScreen(
    navController: NavController,
    viewModel: NavidromeSearchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsStateWithLifecycle()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    val songsLabel = stringResource(R.string.songs)

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.search)) },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions =
                        androidx.compose.foundation.text.KeyboardActions(
                            onSearch = { viewModel.search(query) },
                        ),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                )
            }

            when (val state = uiState) {
                is NavidromeSearchUiState.Idle -> Unit

                is NavidromeSearchUiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }

                is NavidromeSearchUiState.Error -> {
                    item {
                        EmptyPlaceholder(
                            icon = R.drawable.info,
                            text = stringResource(R.string.error_unknown),
                        )
                    }
                }

                is NavidromeSearchUiState.Content -> {
                    if (state.isEmpty) {
                        item {
                            EmptyPlaceholder(
                                icon = R.drawable.search,
                                text = stringResource(R.string.no_results_found),
                            )
                        }
                    }

                    if (state.songs.isNotEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.songs),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        }
                        items(items = state.songs, key = { "song_${it.id}" }) { song ->
                            val index = state.songs.indexOf(song)
                            MediaMetadataListItem(
                                mediaMetadata = song,
                                isActive = song.id == mediaMetadata?.id,
                                isPlaying = isPlaying,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .clickable {
                                            playerConnection.playQueue(
                                                ListQueue(
                                                    title = songsLabel,
                                                    items = state.songs.map { it.toMediaItem() },
                                                    startIndex = index,
                                                ),
                                            )
                                        },
                            )
                        }
                    }

                    if (state.albums.isNotEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.albums),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        }
                        item {
                            LazyRow(modifier = Modifier.fillMaxWidth()) {
                                items(items = state.albums, key = { "album_${it.id}" }) { album ->
                                    NavidromeAlbumGridItem(
                                        album = album,
                                        onClick = { navController.navigate("navidrome_album/${album.id}") },
                                    )
                                }
                            }
                        }
                    }

                    if (state.artists.isNotEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.artists),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        }
                        items(items = state.artists, key = { "artist_${it.id}" }) { artist ->
                            NavidromeArtistListItem(
                                artist = artist,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
