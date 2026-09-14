/*
 * Mussiech (2026)
 * GPL-3.0 License | Contributors: see git history
 */

package tech.tekkiech.mussiech.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tech.tekkiech.mussiech.aurral.AurralRequestRow
import tech.tekkiech.mussiech.repository.AurralRepository
import javax.inject.Inject

sealed interface AurralRequestsUiState {
    data object Loading : AurralRequestsUiState

    data class Content(
        val rows: List<AurralRequestRow>,
    ) : AurralRequestsUiState

    data object Error : AurralRequestsUiState
}

@HiltViewModel
class AurralRequestsViewModel
    @Inject
    constructor(
        private val aurralRepository: AurralRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<AurralRequestsUiState>(AurralRequestsUiState.Loading)
        val uiState: StateFlow<AurralRequestsUiState> = _uiState.asStateFlow()

        init {
            refresh()
        }

        fun refresh() {
            viewModelScope.launch {
                _uiState.value = AurralRequestsUiState.Loading
                val rows =
                    aurralRepository.listRequests().getOrElse {
                        _uiState.value = AurralRequestsUiState.Error
                        return@launch
                    }
                _uiState.value = AurralRequestsUiState.Content(rows)
            }
        }
    }
