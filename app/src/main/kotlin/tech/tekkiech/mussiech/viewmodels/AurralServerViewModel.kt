/*
 * Mussiech (2026)
 * GPL-3.0 License | Contributors: see git history
 */

package tech.tekkiech.mussiech.viewmodels

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tech.tekkiech.mussiech.auth.AurralAuthRepository
import tech.tekkiech.mussiech.auth.AurralCredentials
import tech.tekkiech.mussiech.constants.AurralQualityProfileIdKey
import tech.tekkiech.mussiech.constants.AurralRootFolderPathKey
import tech.tekkiech.mussiech.utils.dataStore
import tech.tekkiech.mussiech.utils.getAsync
import javax.inject.Inject

sealed interface AurralConnectionState {
    data object Idle : AurralConnectionState

    data object Connecting : AurralConnectionState

    data class Error(
        val message: String,
    ) : AurralConnectionState
}

data class AurralServerUiState(
    val serverUrl: String = "",
    val apiKey: String = "",
    val rootFolderPath: String = DefaultRootFolderPath,
    val qualityProfileId: String = DefaultQualityProfileId.toString(),
    val connectionState: AurralConnectionState = AurralConnectionState.Idle,
    val connected: AurralCredentials? = null,
) {
    companion object {
        const val DefaultRootFolderPath = "/data/Music"
        const val DefaultQualityProfileId = 1
    }
}

@HiltViewModel
class AurralServerViewModel
    @Inject
    constructor(
        private val authRepository: AurralAuthRepository,
        @ApplicationContext private val context: Context,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(AurralServerUiState())
        val uiState: StateFlow<AurralServerUiState> = _uiState.asStateFlow()

        init {
            viewModelScope.launch {
                val current = authRepository.currentCredentials()
                val rootFolderPath =
                    context.dataStore.getAsync(AurralRootFolderPathKey)
                        ?: AurralServerUiState.DefaultRootFolderPath
                val qualityProfileId =
                    context.dataStore.getAsync(AurralQualityProfileIdKey)
                        ?: AurralServerUiState.DefaultQualityProfileId
                _uiState.update {
                    it.copy(
                        connected = current,
                        serverUrl = current?.serverUrl ?: it.serverUrl,
                        rootFolderPath = rootFolderPath,
                        qualityProfileId = qualityProfileId.toString(),
                    )
                }
            }
        }

        fun onServerUrlChange(value: String) = _uiState.update { it.copy(serverUrl = value) }

        fun onApiKeyChange(value: String) = _uiState.update { it.copy(apiKey = value) }

        fun onRootFolderPathChange(value: String) {
            _uiState.update { it.copy(rootFolderPath = value) }
            viewModelScope.launch {
                context.dataStore.edit { preferences -> preferences[AurralRootFolderPathKey] = value }
            }
        }

        fun onQualityProfileIdChange(value: String) {
            _uiState.update { it.copy(qualityProfileId = value) }
            value.toIntOrNull()?.let { intValue ->
                viewModelScope.launch {
                    context.dataStore.edit { preferences -> preferences[AurralQualityProfileIdKey] = intValue }
                }
            }
        }

        fun connect() {
            val state = _uiState.value
            if (state.connectionState is AurralConnectionState.Connecting) return

            viewModelScope.launch {
                _uiState.update { it.copy(connectionState = AurralConnectionState.Connecting) }
                authRepository
                    .login(state.serverUrl, state.apiKey)
                    .fold(
                        onSuccess = {
                            val credentials = authRepository.currentCredentials()
                            _uiState.update {
                                it.copy(
                                    connectionState = AurralConnectionState.Idle,
                                    connected = credentials,
                                    apiKey = "",
                                )
                            }
                        },
                        onFailure = { throwable ->
                            _uiState.update {
                                it.copy(
                                    connectionState =
                                        AurralConnectionState.Error(throwable.message ?: throwable.toString()),
                                )
                            }
                        },
                    )
            }
        }

        fun disconnect() {
            viewModelScope.launch {
                authRepository.logout()
                _uiState.update { it.copy(connected = null) }
            }
        }
    }
