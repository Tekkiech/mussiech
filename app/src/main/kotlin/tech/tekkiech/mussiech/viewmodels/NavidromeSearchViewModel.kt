/*
 * Mussiech (2026)
 * GPL-3.0 License | Contributors: see git history
 */

package tech.tekkiech.mussiech.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tech.tekkiech.mussiech.models.MediaMetadata
import tech.tekkiech.mussiech.models.NavidromeAlbum
import tech.tekkiech.mussiech.models.NavidromeArtist
import tech.tekkiech.mussiech.models.toMediaMetadata
import tech.tekkiech.mussiech.models.toNavidromeAlbum
import tech.tekkiech.mussiech.models.toNavidromeArtist
import tech.tekkiech.mussiech.repository.NavidromeRepository
import javax.inject.Inject

sealed interface NavidromeSearchUiState {
    data object Idle : NavidromeSearchUiState

    data object Loading : NavidromeSearchUiState

    data class Content(
        val songs: List<MediaMetadata>,
        val albums: List<NavidromeAlbum>,
        val artists: List<NavidromeArtist>,
    ) : NavidromeSearchUiState {
        val isEmpty: Boolean get() = songs.isEmpty() && albums.isEmpty() && artists.isEmpty()
    }

    data object Error : NavidromeSearchUiState
}

@HiltViewModel
class NavidromeSearchViewModel
    @Inject
    constructor(
        private val navidromeRepository: NavidromeRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<NavidromeSearchUiState>(NavidromeSearchUiState.Idle)
        val uiState: StateFlow<NavidromeSearchUiState> = _uiState.asStateFlow()

        private var searchJob: Job? = null

        fun search(query: String) {
            searchJob?.cancel()
            if (query.isBlank()) {
                _uiState.value = NavidromeSearchUiState.Idle
                return
            }
            searchJob =
                viewModelScope.launch {
                    _uiState.value = NavidromeSearchUiState.Loading
                    val result =
                        navidromeRepository.search(query).getOrElse {
                            _uiState.value = NavidromeSearchUiState.Error
                            return@launch
                        }
                    val songs =
                        result.songs.map { song ->
                            val coverArtUrl =
                                song.coverArtId?.let { navidromeRepository.getCoverArtUrl(it).getOrNull() }
                            song.toMediaMetadata(coverArtUrl)
                        }
                    val albums =
                        result.albums.map { album ->
                            val coverArtUrl =
                                album.coverArtId?.let { navidromeRepository.getCoverArtUrl(it).getOrNull() }
                            album.toNavidromeAlbum(coverArtUrl)
                        }
                    val artists =
                        result.artists.map { artist ->
                            val coverArtUrl =
                                artist.coverArtId?.let { navidromeRepository.getCoverArtUrl(it).getOrNull() }
                            artist.toNavidromeArtist(coverArtUrl)
                        }
                    _uiState.value = NavidromeSearchUiState.Content(songs, albums, artists)
                }
        }
    }
