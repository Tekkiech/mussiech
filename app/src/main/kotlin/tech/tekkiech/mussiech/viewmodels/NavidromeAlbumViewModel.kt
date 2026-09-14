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
import tech.tekkiech.mussiech.models.MediaMetadata
import tech.tekkiech.mussiech.models.toMediaMetadata
import tech.tekkiech.mussiech.repository.NavidromeRepository
import javax.inject.Inject

sealed interface NavidromeDetailUiState {
    data object Loading : NavidromeDetailUiState

    data class Content(
        val title: String,
        val tracks: List<MediaMetadata>,
    ) : NavidromeDetailUiState

    data object Error : NavidromeDetailUiState
}

@HiltViewModel
class NavidromeAlbumViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val navidromeRepository: NavidromeRepository,
    ) : ViewModel() {
        private val albumId = savedStateHandle.get<String>("albumId")!!

        private val _uiState = MutableStateFlow<NavidromeDetailUiState>(NavidromeDetailUiState.Loading)
        val uiState: StateFlow<NavidromeDetailUiState> = _uiState.asStateFlow()

        init {
            viewModelScope.launch {
                val album =
                    navidromeRepository.getAlbum(albumId).getOrElse {
                        _uiState.value = NavidromeDetailUiState.Error
                        return@launch
                    }
                val tracks =
                    album.songs.map { song ->
                        val coverArtUrl =
                            song.coverArtId?.let { coverArtId ->
                                navidromeRepository.getCoverArtUrl(coverArtId).getOrNull()
                            }
                        song.toMediaMetadata(coverArtUrl)
                    }
                _uiState.value = NavidromeDetailUiState.Content(title = album.name, tracks = tracks)
            }
        }
    }
