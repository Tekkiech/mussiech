/*
 * Mussiech (2026)
 * GPL-3.0 License | Contributors: see git history
 */

package tech.tekkiech.mussiech.repository

import dev.zt64.subsonic.api.model.Album
import dev.zt64.subsonic.api.model.AlbumListType
import dev.zt64.subsonic.api.model.Artist
import dev.zt64.subsonic.api.model.ArtistIndex
import dev.zt64.subsonic.api.model.Lyrics
import dev.zt64.subsonic.api.model.Playlist
import dev.zt64.subsonic.api.model.SearchResult
import dev.zt64.subsonic.client.SubsonicClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import tech.tekkiech.mussiech.auth.NavidromeAuthRepository
import tech.tekkiech.mussiech.auth.NavidromeCredentials
import tech.tekkiech.mussiech.auth.buildSubsonicClient
import javax.inject.Inject
import javax.inject.Singleton

class NotLoggedInToNavidromeException : IllegalStateException("Not logged in to a Navidrome server")

@Singleton
class NavidromeRepository
    @Inject
    constructor(
        private val authRepository: NavidromeAuthRepository,
    ) {
        private val clientMutex = Mutex()
        private var cachedCredentials: NavidromeCredentials? = null
        private var cachedClient: SubsonicClient? = null

        private suspend fun client(): SubsonicClient =
            clientMutex.withLock {
                val credentials = authRepository.currentCredentials() ?: throw NotLoggedInToNavidromeException()
                if (credentials != cachedCredentials) {
                    cachedClient?.close()
                    cachedClient = buildSubsonicClient(credentials)
                    cachedCredentials = credentials
                }
                cachedClient!!
            }

        suspend fun ping(): Result<Unit> = call { it.ping() }

        suspend fun getArtists(): Result<List<Artist>> =
            call { it.getArtists() }.map { indexes -> indexes.flatMap(ArtistIndex::artists) }

        suspend fun getArtist(id: String): Result<Artist> = call { it.getArtist(id) }

        suspend fun getAlbum(id: String): Result<Album> = call { it.getAlbum(id) }

        suspend fun getAlbums(
            type: AlbumListType,
            size: Int = 20,
            offset: Int = 0,
        ): Result<List<Album>> = call { it.getAlbumsID3(type, size, offset) }

        suspend fun search(
            query: String,
            artistCount: Int = 20,
            albumCount: Int = 20,
            songCount: Int = 20,
        ): Result<SearchResult> =
            call {
                it.searchID3(
                    query = query,
                    artistCount = artistCount,
                    albumCount = albumCount,
                    songCount = songCount,
                )
            }

        suspend fun getPlaylists(): Result<List<Playlist>> = call { it.getPlaylists() }

        suspend fun getPlaylist(id: String): Result<Playlist> = call { it.getPlaylist(id) }

        suspend fun star(id: String): Result<Unit> = call { it.star(id) }

        suspend fun unstar(id: String): Result<Unit> = call { it.unstar(id) }

        suspend fun setRating(
            id: String,
            rating: Int,
        ): Result<Unit> = call { it.setRating(id, rating) }

        suspend fun scrobble(
            id: String,
            submission: Boolean = true,
        ): Result<Unit> = call { it.scrobble(id = id, submission = submission) }

        suspend fun getLyrics(
            artist: String,
            title: String,
        ): Result<Lyrics> = call { it.getLyrics(artist, title) }

        /**
         * Builds a stream URL for a song. Not suspend - just constructs a URL with
         * auth params baked in, doesn't make a network call - but still needs a
         * live client for the current credentials.
         */
        suspend fun getStreamUrl(id: String): Result<String> = call { it.getStreamUrl(id) }

        suspend fun getCoverArtUrl(
            id: String,
            size: String? = null,
        ): Result<String> = call { it.getCoverArtUrl(id, size) }

        private suspend fun <T> call(block: suspend (SubsonicClient) -> T): Result<T> =
            withContext(Dispatchers.IO) {
                try {
                    Result.success(block(client()))
                } catch (throwable: Throwable) {
                    if (throwable is CancellationException) throw throwable
                    Result.failure(throwable)
                }
            }
    }
