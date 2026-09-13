/*
 * Mussiech (2026)
 * GPL-3.0 License | Contributors: see git history
 */

package tech.tekkiech.mussiech.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.zt64.subsonic.client.SubsonicAuth
import dev.zt64.subsonic.client.SubsonicClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tech.tekkiech.mussiech.constants.NavidromeSaltKey
import tech.tekkiech.mussiech.constants.NavidromeServerUrlKey
import tech.tekkiech.mussiech.constants.NavidromeTokenKey
import tech.tekkiech.mussiech.constants.NavidromeUsernameKey
import tech.tekkiech.mussiech.utils.dataStore
import tech.tekkiech.mussiech.utils.getAsync
import javax.inject.Inject
import javax.inject.Singleton

data class NavidromeCredentials(
    val serverUrl: String,
    val username: String,
    val salt: String,
    val token: String,
)

internal fun buildSubsonicClient(credentials: NavidromeCredentials): SubsonicClient =
    SubsonicClient(
        baseUrl = credentials.serverUrl,
        auth = SubsonicAuth.Token(credentials.username, credentials.salt, credentials.token),
    )

private fun normalizeServerUrl(rawUrl: String): String {
    val trimmed = rawUrl.trim().trimEnd('/')
    return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
        trimmed
    } else {
        "https://$trimmed"
    }
}

@Singleton
class NavidromeAuthRepository
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        suspend fun login(
            serverUrl: String,
            username: String,
            password: String,
        ): Result<Unit> =
            withContext(Dispatchers.IO) {
                runCatchingPreservingCancellation {
                    val normalizedUrl = normalizeServerUrl(serverUrl)
                    require(username.isNotBlank()) { "Username is required" }
                    require(password.isNotBlank()) { "Password is required" }

                    val auth = SubsonicAuth.Token(username, password)
                    val client = SubsonicClient(baseUrl = normalizedUrl, auth = auth)
                    try {
                        client.ping()
                    } finally {
                        client.close()
                    }

                    context.dataStore.edit { preferences ->
                        preferences[NavidromeServerUrlKey] = normalizedUrl
                        preferences[NavidromeUsernameKey] = username
                        preferences[NavidromeSaltKey] = auth.salt
                        preferences[NavidromeTokenKey] = auth.token
                    }
                    Unit
                }
            }

        suspend fun logout() {
            withContext(Dispatchers.IO) {
                context.dataStore.edit { preferences ->
                    preferences.remove(NavidromeServerUrlKey)
                    preferences.remove(NavidromeUsernameKey)
                    preferences.remove(NavidromeSaltKey)
                    preferences.remove(NavidromeTokenKey)
                }
            }
        }

        suspend fun currentCredentials(): NavidromeCredentials? =
            withContext(Dispatchers.IO) {
                val serverUrl = context.dataStore.getAsync(NavidromeServerUrlKey)
                val username = context.dataStore.getAsync(NavidromeUsernameKey)
                val salt = context.dataStore.getAsync(NavidromeSaltKey)
                val token = context.dataStore.getAsync(NavidromeTokenKey)

                if (serverUrl.isNullOrBlank() || username.isNullOrBlank() || salt.isNullOrBlank() || token.isNullOrBlank()) {
                    null
                } else {
                    NavidromeCredentials(serverUrl, username, salt, token)
                }
            }
    }

private suspend inline fun <T> runCatchingPreservingCancellation(crossinline block: suspend () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (throwable: Throwable) {
        if (throwable is CancellationException) throw throwable
        Result.failure(throwable)
    }
