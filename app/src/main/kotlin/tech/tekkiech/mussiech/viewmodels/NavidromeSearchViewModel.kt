/*
 * Mussiech (2026)
 * GPL-3.0 License | Contributors: see git history
 */

package tech.tekkiech.mussiech.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tech.tekkiech.mussiech.aurral.AurralSearchItem
import tech.tekkiech.mussiech.auth.AurralAuthRepository
import tech.tekkiech.mussiech.constants.AurralQualityProfileIdKey
import tech.tekkiech.mussiech.constants.AurralRootFolderPathKey
import tech.tekkiech.mussiech.models.MediaMetadata
import tech.tekkiech.mussiech.models.NavidromeAlbum
import tech.tekkiech.mussiech.models.NavidromeArtist
import tech.tekkiech.mussiech.models.toMediaMetadata
import tech.tekkiech.mussiech.models.toNavidromeAlbum
import tech.tekkiech.mussiech.models.toNavidromeArtist
import tech.tekkiech.mussiech.repository.AurralRepository
import tech.tekkiech.mussiech.repository.NavidromeRepository
import tech.tekkiech.mussiech.utils.dataStore
import tech.tekkiech.mussiech.utils.getAsync
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

sealed interface AurralSearchUiState {
    /** aurral isn't configured in Settings - don't show the section at all. */
    data object Unconfigured : AurralSearchUiState

    data object Idle : AurralSearchUiState

    data object Loading : AurralSearchUiState

    data class Content(
        val artists: List<AurralSearchItem>,
    ) : AurralSearchUiState

    data object Error : AurralSearchUiState
}

enum class AurralRequestState { IDLE, REQUESTING, REQUESTED, FAILED }

@HiltViewModel
class NavidromeSearchViewModel
    @Inject
    constructor(
        private val navidromeRepository: NavidromeRepository,
        private val aurralRepository: AurralRepository,
        private val aurralAuthRepository: AurralAuthRepository,
        @ApplicationContext private val context: Context,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<NavidromeSearchUiState>(NavidromeSearchUiState.Idle)
        val uiState: StateFlow<NavidromeSearchUiState> = _uiState.asStateFlow()

        private val _aurralUiState = MutableStateFlow<AurralSearchUiState>(AurralSearchUiState.Unconfigured)
        val aurralUiState: StateFlow<AurralSearchUiState> = _aurralUiState.asStateFlow()

        private val _requestStates = MutableStateFlow<Map<String, AurralRequestState>>(emptyMap())
        val requestStates: StateFlow<Map<String, AurralRequestState>> = _requestStates.asStateFlow()

        private var searchJob: Job? = null
        private var aurralSearchJob: Job? = null

        fun search(query: String) {
            searchJob?.cancel()
            aurralSearchJob?.cancel()
            if (query.isBlank()) {
                _uiState.value = NavidromeSearchUiState.Idle
                _aurralUiState.value = AurralSearchUiState.Unconfigured
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
            aurralSearchJob =
                viewModelScope.launch {
                    if (aurralAuthRepository.currentCredentials() == null) {
                        _aurralUiState.value = AurralSearchUiState.Unconfigured
                        return@launch
                    }
                    _aurralUiState.value = AurralSearchUiState.Loading
                    val response =
                        aurralRepository.search(query, scope = "artist").getOrElse {
                            _aurralUiState.value = AurralSearchUiState.Error
                            return@launch
                        }
                    _aurralUiState.value = AurralSearchUiState.Content(response.items)
                }
        }

        fun requestArtist(item: AurralSearchItem) {
            viewModelScope.launch {
                _requestStates.update { it + (item.id to AurralRequestState.REQUESTING) }
                val rootFolderPath =
                    context.dataStore.getAsync(AurralRootFolderPathKey)
                        ?: AurralServerUiState.DefaultRootFolderPath
                val qualityProfileId =
                    context.dataStore.getAsync(AurralQualityProfileIdKey)
                        ?: AurralServerUiState.DefaultQualityProfileId
                val result =
                    aurralRepository.requestArtist(
                        foreignArtistId = item.id,
                        artistName = item.name,
                        rootFolderPath = rootFolderPath,
                        qualityProfileId = qualityProfileId,
                    )
                val newState = if (result.isSuccess) AurralRequestState.REQUESTED else AurralRequestState.FAILED
                _requestStates.update { it + (item.id to newState) }
            }
        }
    }
