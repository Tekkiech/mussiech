/*
 * Mussiech (2026)
 * GPL-3.0 License | Contributors: see git history
 */

package tech.tekkiech.mussiech.repository

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import tech.tekkiech.mussiech.aurral.AurralRequestArtistBody
import tech.tekkiech.mussiech.aurral.AurralRequestRow
import tech.tekkiech.mussiech.aurral.AurralSearchResponse
import tech.tekkiech.mussiech.auth.AurralAuthRepository
import javax.inject.Inject
import javax.inject.Singleton

class NotLoggedInToAurralException : IllegalStateException("Not connected to an aurral instance")

class AurralApiException(
    statusCode: Int,
) : Exception("aurral request failed with HTTP $statusCode")

@Singleton
class AurralRepository
    @Inject
    constructor(
        private val authRepository: AurralAuthRepository,
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

        private val json = Json { ignoreUnknownKeys = true }

        suspend fun search(
            query: String,
            scope: String = "artist",
            limit: Int = 24,
            offset: Int = 0,
        ): Result<AurralSearchResponse> =
            call { serverUrl, apiKey ->
                val response =
                    client.get("$serverUrl/api/search") {
                        header("X-Api-Key", apiKey)
                        parameter("q", query)
                        parameter("scope", scope)
                        parameter("limit", limit)
                        parameter("offset", offset)
                    }
                if (response.status.value !in 200..299) throw AurralApiException(response.status.value)
                json.decodeFromString<AurralSearchResponse>(response.bodyAsText())
            }

        suspend fun requestArtist(
            foreignArtistId: String,
            artistName: String,
            rootFolderPath: String,
            qualityProfileId: Int,
        ): Result<Unit> =
            call { serverUrl, apiKey ->
                val response =
                    client.post("$serverUrl/api/library/artists") {
                        header("X-Api-Key", apiKey)
                        contentType(ContentType.Application.Json)
                        setBody(
                            json.encodeToString(
                                AurralRequestArtistBody(
                                    foreignArtistId = foreignArtistId,
                                    artistName = artistName,
                                    rootFolderPath = rootFolderPath,
                                    qualityProfileId = qualityProfileId,
                                ),
                            ),
                        )
                    }
                if (response.status.value !in 200..299) throw AurralApiException(response.status.value)
                Unit
            }

        suspend fun listRequests(): Result<List<AurralRequestRow>> =
            call { serverUrl, apiKey ->
                val response =
                    client.get("$serverUrl/api/requests") {
                        header("X-Api-Key", apiKey)
                    }
                if (response.status.value !in 200..299) throw AurralApiException(response.status.value)
                json.decodeFromString<List<AurralRequestRow>>(response.bodyAsText())
            }

        suspend fun cancelRequest(id: String): Result<Unit> =
            call { serverUrl, apiKey ->
                val response =
                    client.delete("$serverUrl/api/requests/$id") {
                        header("X-Api-Key", apiKey)
                    }
                if (response.status.value !in 200..299) throw AurralApiException(response.status.value)
                Unit
            }

        private suspend fun <T> call(block: suspend (serverUrl: String, apiKey: String) -> T): Result<T> =
            withContext(Dispatchers.IO) {
                try {
                    val credentials = authRepository.currentCredentials() ?: throw NotLoggedInToAurralException()
                    Result.success(block(credentials.serverUrl, credentials.apiKey))
                } catch (throwable: Throwable) {
                    if (throwable is CancellationException) throw throwable
                    Result.failure(throwable)
                }
            }

        private companion object {
            const val REQUEST_TIMEOUT_MILLIS = 15_000L
            const val CONNECT_TIMEOUT_MILLIS = 8_000L
        }
    }
