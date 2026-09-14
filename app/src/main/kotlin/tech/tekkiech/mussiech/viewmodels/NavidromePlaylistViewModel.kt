/*
 * Mussiech (2026)
 * GPL-3.0 License | Contributors: see git history
 */

package tech.tekkiech.mussiech.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tech.tekkiech.mussiech.models.toMediaMetadata
import tech.tekkiech.mussiech.repository.NavidromeRepository
import javax.inject.Inject

@HiltViewModel
class NavidromePlaylistViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val navidromeRepository: NavidromeRepository,
    ) : ViewModel() {
        private val playlistId = savedStateHandle.get<String>("playlistId")!!

        private val _uiState = MutableStateFlow<NavidromeDetailUiState>(NavidromeDetailUiState.Loading)
        val uiState: StateFlow<NavidromeDetailUiState> = _uiState.asStateFlow()

        init {
            viewModelScope.launch {
                val playlist =
                    navidromeRepository.getPlaylist(playlistId).getOrElse {
                        _uiState.value = NavidromeDetailUiState.Error
                        return@launch
                    }
                val tracks =
                    playlist.songs.map { song ->
                        val coverArtUrl =
                            song.coverArtId?.let { coverArtId ->
                                navidromeRepository.getCoverArtUrl(coverArtId).getOrNull()
                            }
                        song.toMediaMetadata(coverArtUrl)
                    }
                _uiState.value = NavidromeDetailUiState.Content(title = playlist.name, tracks = tracks)
            }
        }
    }
