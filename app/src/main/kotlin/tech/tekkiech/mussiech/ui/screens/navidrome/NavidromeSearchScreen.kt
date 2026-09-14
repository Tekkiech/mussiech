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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import tech.tekkiech.mussiech.LocalPlayerAwareWindowInsets
import tech.tekkiech.mussiech.LocalPlayerConnection
import tech.tekkiech.mussiech.R
import tech.tekkiech.mussiech.aurral.AurralSearchItem
import tech.tekkiech.mussiech.constants.ListThumbnailSize
import tech.tekkiech.mussiech.extensions.toMediaItem
import tech.tekkiech.mussiech.playback.queues.ListQueue
import tech.tekkiech.mussiech.ui.component.EmptyPlaceholder
import tech.tekkiech.mussiech.ui.component.ItemThumbnail
import tech.tekkiech.mussiech.ui.component.ListItem
import tech.tekkiech.mussiech.ui.component.MediaMetadataListItem
import tech.tekkiech.mussiech.ui.component.NavidromeAlbumGridItem
import tech.tekkiech.mussiech.ui.component.NavidromeArtistListItem
import tech.tekkiech.mussiech.viewmodels.AurralRequestState
import tech.tekkiech.mussiech.viewmodels.AurralSearchUiState
import tech.tekkiech.mussiech.viewmodels.NavidromeSearchUiState
import tech.tekkiech.mussiech.viewmodels.NavidromeSearchViewModel

@Composable
fun NavidromeSearchScreen(
    navController: NavController,
    viewModel: NavidromeSearchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val aurralUiState by viewModel.aurralUiState.collectAsStateWithLifecycle()
    val requestStates by viewModel.requestStates.collectAsStateWithLifecycle()
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsStateWithLifecycle()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    val songsLabel = stringResource(R.string.songs)

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(LocalPlayerAwareWindowInsets.current),
        ) {
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

            when (val aurralState = aurralUiState) {
                is AurralSearchUiState.Unconfigured, is AurralSearchUiState.Idle -> Unit

                is AurralSearchUiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }

                is AurralSearchUiState.Error -> Unit

                is AurralSearchUiState.Content -> {
                    if (aurralState.artists.isNotEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.aurral_requests),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        }
                        items(items = aurralState.artists, key = { "aurral_${it.id}" }) { item ->
                            AurralArtistRow(
                                item = item,
                                requestState = requestStates[item.id] ?: AurralRequestState.IDLE,
                                onRequest = { viewModel.requestArtist(item) },
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

@Composable
private fun AurralArtistRow(
    item: AurralSearchItem,
    requestState: AurralRequestState,
    onRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        title = item.name,
        subtitle = item.disambiguation,
        thumbnailContent = {
            ItemThumbnail(
                thumbnailUrl = item.imageUrl,
                isActive = false,
                isPlaying = false,
                shape = CircleShape,
                modifier = Modifier.size(ListThumbnailSize),
            )
        },
        trailingContent = {
            when (requestState) {
                AurralRequestState.IDLE ->
                    TextButton(onClick = onRequest) {
                        Text(stringResource(R.string.aurral_request_artist))
                    }

                AurralRequestState.REQUESTING ->
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)

                AurralRequestState.REQUESTED ->
                    Text(
                        text = stringResource(R.string.aurral_requested),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )

                AurralRequestState.FAILED ->
                    TextButton(onClick = onRequest) {
                        Text(
                            text = stringResource(R.string.aurral_request_failed),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
            }
        },
        modifier = modifier,
    )
}
