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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tech.tekkiech.mussiech.auth.NavidromeAuthRepository
import tech.tekkiech.mussiech.auth.NavidromeCredentials
import javax.inject.Inject

sealed interface NavidromeConnectionState {
    data object Idle : NavidromeConnectionState

    data object Connecting : NavidromeConnectionState

    data class Error(
        val message: String,
    ) : NavidromeConnectionState
}

data class NavidromeServerUiState(
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val connectionState: NavidromeConnectionState = NavidromeConnectionState.Idle,
    val loggedInAs: NavidromeCredentials? = null,
)

@HiltViewModel
class NavidromeServerViewModel
    @Inject
    constructor(
        private val authRepository: NavidromeAuthRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(NavidromeServerUiState())
        val uiState: StateFlow<NavidromeServerUiState> = _uiState.asStateFlow()

        init {
            viewModelScope.launch {
                val current = authRepository.currentCredentials()
                if (current != null) {
                    _uiState.update {
                        it.copy(
                            loggedInAs = current,
                            serverUrl = current.serverUrl,
                            username = current.username,
                        )
                    }
                }
            }
        }

        fun onServerUrlChange(value: String) = _uiState.update { it.copy(serverUrl = value) }

        fun onUsernameChange(value: String) = _uiState.update { it.copy(username = value) }

        fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value) }

        fun connect() {
            val state = _uiState.value
            if (state.connectionState is NavidromeConnectionState.Connecting) return

            viewModelScope.launch {
                _uiState.update { it.copy(connectionState = NavidromeConnectionState.Connecting) }
                authRepository
                    .login(state.serverUrl, state.username, state.password)
                    .fold(
                        onSuccess = {
                            val credentials = authRepository.currentCredentials()
                            _uiState.update {
                                it.copy(
                                    connectionState = NavidromeConnectionState.Idle,
                                    loggedInAs = credentials,
                                    password = "",
                                )
                            }
                        },
                        onFailure = { throwable ->
                            _uiState.update {
                                it.copy(
                                    connectionState =
                                        NavidromeConnectionState.Error(
                                            throwable.message ?: throwable.toString(),
                                        ),
                                )
                            }
                        },
                    )
            }
        }

        fun logOut() {
            viewModelScope.launch {
                authRepository.logout()
                _uiState.update { NavidromeServerUiState() }
            }
        }
    }
