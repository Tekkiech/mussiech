/*
 * Mussiech (2026)
 * GPL-3.0 License | Contributors: see git history
 */

package tech.tekkiech.mussiech.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tech.tekkiech.mussiech.constants.AurralApiKeyKey
import tech.tekkiech.mussiech.constants.AurralServerUrlKey
import tech.tekkiech.mussiech.utils.dataStore
import tech.tekkiech.mussiech.utils.getAsync
import javax.inject.Inject
import javax.inject.Singleton

data class AurralCredentials(
    val serverUrl: String,
    val apiKey: String,
)

private fun normalizeServerUrl(rawUrl: String): String {
    val trimmed = rawUrl.trim().trimEnd('/')
    return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
        trimmed
    } else {
        "https://$trimmed"
    }
}

/**
 * aurral has no per-user, permission-scoped API keys - `GET /api/auth/api-key` returns one
 * global instance key, and using it via the `X-Api-Key` header resolves to a synthetic admin
 * identity with every permission. There is no login/token-derivation flow to implement here,
 * unlike [NavidromeAuthRepository] - the user pastes in a key they've already obtained from
 * their aurral instance, and it's stored as-is.
 */
@Singleton
class AurralAuthRepository
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val client =
            HttpClient(OkHttp) {
                expectSuccess = false
                install(HttpTimeout) {
                    requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
                    connectTimeoutMillis = CONNECT_TIMEOUT_MILLIS
                    socketTimeoutMillis = REQUEST_TIMEOUT_MILLIS
                }
            }

        suspend fun login(
            serverUrl: String,
            apiKey: String,
        ): Result<Unit> =
            withContext(Dispatchers.IO) {
                try {
                    val normalizedUrl = normalizeServerUrl(serverUrl)
                    require(apiKey.isNotBlank()) { "API key is required" }

                    val response =
                        client.get("$normalizedUrl/api/requests") {
                            header("X-Api-Key", apiKey)
                        }
                    check(response.status.value in 200..299) {
                        "Connection test failed with HTTP ${response.status.value}"
                    }

                    context.dataStore.edit { preferences ->
                        preferences[AurralServerUrlKey] = normalizedUrl
                        preferences[AurralApiKeyKey] = apiKey
                    }
                    Result.success(Unit)
                } catch (throwable: Throwable) {
                    if (throwable is CancellationException) throw throwable
                    Result.failure(throwable)
                }
            }

        suspend fun logout() {
            withContext(Dispatchers.IO) {
                context.dataStore.edit { preferences ->
                    preferences.remove(AurralServerUrlKey)
                    preferences.remove(AurralApiKeyKey)
                }
            }
        }

        suspend fun currentCredentials(): AurralCredentials? =
            withContext(Dispatchers.IO) {
                val serverUrl = context.dataStore.getAsync(AurralServerUrlKey)
                val apiKey = context.dataStore.getAsync(AurralApiKeyKey)
                if (serverUrl.isNullOrBlank() || apiKey.isNullOrBlank()) {
                    null
                } else {
                    AurralCredentials(serverUrl, apiKey)
                }
            }

        private companion object {
            const val REQUEST_TIMEOUT_MILLIS = 10_000L
            const val CONNECT_TIMEOUT_MILLIS = 8_000L
        }
    }
