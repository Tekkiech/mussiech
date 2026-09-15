/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

@file:OptIn(ExperimentalCoroutinesApi::class)

package tech.tekkiech.mussiech.viewmodels

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.exoplayer.offline.Download
import com.google.common.collect.ImmutableList
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tech.tekkiech.mussiech.R
import tech.tekkiech.mussiech.constants.AlbumFilter
import tech.tekkiech.mussiech.constants.AlbumFilterKey
import tech.tekkiech.mussiech.constants.AlbumSortDescendingKey
import tech.tekkiech.mussiech.constants.AlbumSortType
import tech.tekkiech.mussiech.constants.AlbumSortTypeKey
import tech.tekkiech.mussiech.constants.ArtistFilter
import tech.tekkiech.mussiech.constants.ArtistFilterKey
import tech.tekkiech.mussiech.constants.ArtistSongSortDescendingKey
import tech.tekkiech.mussiech.constants.ArtistSongSortType
import tech.tekkiech.mussiech.constants.ArtistSongSortTypeKey
import tech.tekkiech.mussiech.constants.ArtistSortDescendingKey
import tech.tekkiech.mussiech.constants.ArtistSortType
import tech.tekkiech.mussiech.constants.ArtistSortTypeKey
import tech.tekkiech.mussiech.constants.HideExplicitKey
import tech.tekkiech.mussiech.constants.HideVideoKey
import tech.tekkiech.mussiech.constants.LibraryFilter
import tech.tekkiech.mussiech.constants.PlaylistSortDescendingKey
import tech.tekkiech.mussiech.constants.PlaylistSortType
import tech.tekkiech.mussiech.constants.PlaylistSortTypeKey
import tech.tekkiech.mussiech.constants.SongFilter
import tech.tekkiech.mussiech.constants.SongFilterKey
import tech.tekkiech.mussiech.constants.SongSortDescendingKey
import tech.tekkiech.mussiech.constants.SongSortType
import tech.tekkiech.mussiech.constants.SongSortTypeKey
import tech.tekkiech.mussiech.constants.TopSize
import tech.tekkiech.mussiech.db.MusicDatabase
import tech.tekkiech.mussiech.db.entities.Playlist
import tech.tekkiech.mussiech.db.entities.Song
import tech.tekkiech.mussiech.extensions.filterExplicit
import tech.tekkiech.mussiech.extensions.filterExplicitAlbums
import tech.tekkiech.mussiech.extensions.filterVideo
import tech.tekkiech.mussiech.extensions.reversed
import tech.tekkiech.mussiech.extensions.toEnum
import moe.rukamori.archivetune.innertube.YouTube
import tech.tekkiech.mussiech.models.MediaMetadata
import tech.tekkiech.mussiech.models.toMediaMetadata
import tech.tekkiech.mussiech.playback.DownloadUtil
import tech.tekkiech.mussiech.utils.SyncUtils
import tech.tekkiech.mussiech.utils.dataStore
import tech.tekkiech.mussiech.utils.get
import tech.tekkiech.mussiech.utils.reportException
import java.text.Collator
import java.time.Duration
import java.time.LocalDateTime
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

private const val MOST_PLAYED_ALBUM_WINDOW_MILLIS = 14L * 24L * 60L * 60L * 1000L

@HiltViewModel
class LibrarySongsViewModel
    @Inject
    constructor(
        @ApplicationContext context: Context,
        database: MusicDatabase,
        downloadUtil: DownloadUtil,
        private val syncUtils: SyncUtils,
    ) : ViewModel() {
        private val _isRefreshing = MutableStateFlow(false)
        val isRefreshing = _isRefreshing.asStateFlow()

        val allSongs =
            context.dataStore.data
                .map {
                    Triple(
                        Triple(
                            it[SongFilterKey].toEnum(SongFilter.LIKED),
                            it[SongSortTypeKey].toEnum(SongSortType.CREATE_DATE),
                            (it[SongSortDescendingKey] ?: true),
                        ),
                        it[HideExplicitKey] ?: false,
                        it[HideVideoKey] ?: false,
                    )
                }.distinctUntilChanged()
                .flatMapLatest { (filterSort, hideExplicit, hideVideo) ->
                    val (filter, sortType, descending) = filterSort
                    when (filter) {
                        SongFilter.LIBRARY -> {
                            database.songs(sortType, descending, hideVideo).map { it.filterExplicit(hideExplicit) }
                        }

                        SongFilter.LIKED -> {
                            database.likedSongs(sortType, descending, hideVideo).map { it.filterExplicit(hideExplicit) }
                        }

                        SongFilter.DOWNLOADED -> {
                            downloadUtil.downloads.flatMapLatest { downloads ->
                                database
                                    .allSongs()
                                    .flowOn(Dispatchers.IO)
                                    .map { songs ->
                                        songs.filter { song: Song ->
                                            downloads[song.id]?.state == Download.STATE_COMPLETED
                                        }
                                    }.map { songs ->
                                        when (sortType) {
                                            SongSortType.CREATE_DATE -> {
                                                songs.sortedBy { song: Song ->
                                                    downloads[song.id]?.updateTimeMs ?: 0L
                                                }
                                            }

                                            SongSortType.NAME -> {
                                                songs.sortedBy { song: Song -> song.song.title }
                                            }

                                            SongSortType.ARTIST -> {
                                                val collator =
                                                    Collator.getInstance(Locale.getDefault())
                                                collator.strength = Collator.PRIMARY
                                                songs.sortedWith(
                                                    compareBy<Song, String>(collator) { song ->
                                                        song.artists.joinToString("") { artist -> artist.name }
                                                    },
                                                )
                                            }

                                            SongSortType.PLAY_TIME -> {
                                                songs.sortedBy { song: Song -> song.song.totalPlayTime }
                                            }
                                        }.reversed(descending)
                                            .filterExplicit(hideExplicit)
                                            .filterVideo(hideVideo)
                                    }
                            }
                        }
                    }
                }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

        fun refresh(filter: SongFilter) {
            if (_isRefreshing.value) return
            viewModelScope.launch(Dispatchers.IO) {
                _isRefreshing.value = true
                try {
                    when (filter) {
                        SongFilter.LIKED -> syncUtils.syncLikedSongs()
                        SongFilter.LIBRARY -> syncUtils.syncLibrarySongs()
                        SongFilter.DOWNLOADED -> Unit
                    }
                } catch (e: Exception) {
                    reportException(e)
                } finally {
                    _isRefreshing.value = false
                }
            }
        }

        fun syncLikedSongs() {
            refresh(SongFilter.LIKED)
        }

        fun syncLibrarySongs() {
            refresh(SongFilter.LIBRARY)
        }
    }

@HiltViewModel
class LibraryArtistsViewModel
    @Inject
    constructor(
        @ApplicationContext context: Context,
        database: MusicDatabase,
        private val syncUtils: SyncUtils,
    ) : ViewModel() {
        private val _isRefreshing = MutableStateFlow(false)
        val isRefreshing = _isRefreshing.asStateFlow()

        val allArtists =
            context.dataStore.data
                .map {
                    Triple(
                        it[ArtistFilterKey].toEnum(ArtistFilter.LIKED),
                        it[ArtistSortTypeKey].toEnum(ArtistSortType.CREATE_DATE),
                        it[ArtistSortDescendingKey] ?: true,
                    )
                }.distinctUntilChanged()
                .flatMapLatest { (filter, sortType, descending) ->
                    when (filter) {
                        ArtistFilter.LIBRARY -> database.artists(sortType, descending)
                        ArtistFilter.LIKED -> database.artistsBookmarked(sortType, descending)
                    }
                }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

        fun refresh(filter: ArtistFilter) {
            if (filter != ArtistFilter.LIKED) return
            if (_isRefreshing.value) return
            viewModelScope.launch(Dispatchers.IO) {
                _isRefreshing.value = true
                try {
                    syncUtils.syncArtistsSubscriptions()
                } catch (e: Exception) {
                    reportException(e)
                } finally {
                    _isRefreshing.value = false
                }
            }
        }

        fun sync() {
            refresh(ArtistFilter.LIKED)
        }

        init {
            viewModelScope.launch(Dispatchers.IO) {
                allArtists.collect { artists ->
                    artists
                        .map { it.artist }
                        .filter {
                            it.thumbnailUrl == null || Duration.between(
                                it.lastUpdateTime,
                                LocalDateTime.now(),
                            ) > Duration.ofDays(10)
                        }.forEach { artist ->
                            YouTube.artist(artist.id).onSuccess { artistPage ->
                                database.query {
                                    update(artist, artistPage)
                                }
                            }
                        }
                }
            }
        }
    }

@HiltViewModel
class LibraryAlbumsViewModel
    @Inject
    constructor(
        @ApplicationContext context: Context,
        database: MusicDatabase,
        downloadUtil: DownloadUtil,
        private val syncUtils: SyncUtils,
    ) : ViewModel() {
        private val _isRefreshing = MutableStateFlow(false)
        val isRefreshing = _isRefreshing.asStateFlow()

        val allAlbums =
            context.dataStore.data
                .map {
                    Pair(
                        Triple(
                            it[AlbumFilterKey].toEnum(AlbumFilter.LIKED),
                            it[AlbumSortTypeKey].toEnum(AlbumSortType.CREATE_DATE),
                            it[AlbumSortDescendingKey] ?: true,
                        ),
                        it[HideExplicitKey] ?: false,
                    )
                }.distinctUntilChanged()
                .flatMapLatest { (filterSort, hideExplicit) ->
                    val (filter, sortType, descending) = filterSort
                    when (filter) {
                        AlbumFilter.DOWNLOADED -> {
                            downloadUtil.downloads.flatMapLatest { downloads ->
                                database
                                    .allSongs()
                                    .flowOn(Dispatchers.IO)
                                    .map { songs ->
                                        songs
                                            .filter { song -> downloads[song.id]?.state == Download.STATE_COMPLETED }
                                            .mapNotNull { it.song.albumId }
                                            .toSet()
                                    }.flatMapLatest { downloadedAlbumIds ->
                                        database
                                            .albumsByIds(downloadedAlbumIds, sortType, descending)
                                            .map { albums -> albums.filterExplicitAlbums(hideExplicit) }
                                    }
                            }
                        }

                        AlbumFilter.DOWNLOADED_FULL -> {
                            downloadUtil.downloads.flatMapLatest { downloads ->
                                database
                                    .allSongs()
                                    .flowOn(Dispatchers.IO)
                                    .map { songs ->
                                        songs
                                            .filter { song -> downloads[song.id]?.state == Download.STATE_COMPLETED }
                                            .mapNotNull { song -> song.song.albumId?.let { albumId -> albumId to song } }
                                            .groupBy({ it.first }, { it.second })
                                            .mapValues { (_, songList) -> songList.size }
                                    }.flatMapLatest { downloadedCountByAlbum ->
                                        database
                                            .albumsByIds(downloadedCountByAlbum.keys, sortType, descending)
                                            .map { albums ->
                                                albums
                                                    .filter { album ->
                                                        val totalSongsInAlbum = album.album.songCount
                                                        val downloadedSongsCount = downloadedCountByAlbum[album.album.id] ?: 0
                                                        totalSongsInAlbum > 0 && downloadedSongsCount >= totalSongsInAlbum
                                                    }.filterExplicitAlbums(hideExplicit)
                                            }
                                    }
                            }
                        }

                        AlbumFilter.LIBRARY -> {
                            database.albums(sortType, descending).map { it.filterExplicitAlbums(hideExplicit) }
                        }

                        AlbumFilter.LIKED -> {
                            database.albumsLiked(sortType, descending).map { it.filterExplicitAlbums(hideExplicit) }
                        }
                    }
                }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

        fun refresh(filter: AlbumFilter) {
            if (filter != AlbumFilter.LIKED) return
            if (_isRefreshing.value) return
            viewModelScope.launch(Dispatchers.IO) {
                _isRefreshing.value = true
                try {
                    syncUtils.syncLikedAlbums()
                } catch (e: Exception) {
                    reportException(e)
                } finally {
                    _isRefreshing.value = false
                }
            }
        }

        fun sync() {
            refresh(AlbumFilter.LIKED)
        }

        init {
            viewModelScope.launch(Dispatchers.IO) {
                allAlbums.collect { albums ->
                    albums
                        .filter {
                            it.album.songCount == 0
                        }.forEach { album ->
                            YouTube
                                .album(album.id)
                                .onSuccess { albumPage ->
                                    database.query {
                                        update(album.album, albumPage, album.artists)
                                    }
                                }.onFailure {
                                    reportException(it)
                                    if (it.message?.contains("NOT_FOUND") == true) {
                                        database.query {
                                            delete(album.album)
                                        }
                                    }
                                }
                        }
                }
            }
        }
    }

@HiltViewModel
class LibraryPlaylistsViewModel
    @Inject
    constructor(
        @ApplicationContext context: Context,
        private val database: MusicDatabase,
        private val syncUtils: SyncUtils,
    ) : ViewModel() {
        val allPlaylists =
            context.dataStore.data
                .map {
                    it[PlaylistSortTypeKey].toEnum(PlaylistSortType.CUSTOM) to (
                        it[PlaylistSortDescendingKey]
                            ?: true
                    )
                }.distinctUntilChanged()
                .flatMapLatest { (sortType, descending) ->
                    database.playlists(sortType, descending)
                }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

        private val _isRefreshing = MutableStateFlow(false)
        val isRefreshing = _isRefreshing.asStateFlow()

        fun sync() {
            viewModelScope.launch(Dispatchers.IO) {
                _isRefreshing.value = true
                syncUtils.syncSavedPlaylists()
                syncUtils.syncAutoSyncPlaylists()
                _isRefreshing.value = false
            }
        }

        fun updateCustomPlaylistOrder(playlists: List<Playlist>) {
            if (playlists.isEmpty()) return
            viewModelScope.launch(Dispatchers.IO) {
                database.withTransaction {
                    playlists.forEachIndexed { index, playlist ->
                        setPlaylistCustomOrder(playlist.id, index)
                    }
                }
            }
        }

        val topValue =
            context.dataStore.data
                .map { it[TopSize] ?: "50" }
                .distinctUntilChanged()
    }

@HiltViewModel
class ArtistSongsViewModel
    @Inject
    constructor(
        @ApplicationContext context: Context,
        database: MusicDatabase,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val artistId = savedStateHandle.get<String>("artistId")!!
        val artist =
            database
                .artist(artistId)
                .stateIn(viewModelScope, SharingStarted.Lazily, null)

        val songs =
            context.dataStore.data
                .map {
                    Triple(
                        it[ArtistSongSortTypeKey].toEnum(ArtistSongSortType.CREATE_DATE) to (
                            it[ArtistSongSortDescendingKey]
                                ?: true
                        ),
                        it[HideExplicitKey] ?: false,
                        it[HideVideoKey] ?: false,
                    )
                }.distinctUntilChanged()
                .flatMapLatest { (sortDesc, hideExplicit, hideVideo) ->
                    val (sortType, descending) = sortDesc
                    database.artistSongs(artistId, sortType, descending).map {
                        it.filterExplicit(hideExplicit).filterVideo(hideVideo)
                    }
                }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    }

@HiltViewModel
class LibraryMixViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val database: MusicDatabase,
        private val syncUtils: SyncUtils,
    ) : ViewModel() {
        private val _isRefreshing = MutableStateFlow(false)
        val isRefreshing = _isRefreshing.asStateFlow()

        val mostPlayedAlbumUiState =
            context.dataStore.data
                .map { it[HideExplicitKey] ?: false }
                .distinctUntilChanged()
                .flatMapLatest { hideExplicit ->
                    database
                        .mostPlayedAlbums(
                            fromTimeStamp = System.currentTimeMillis() - MOST_PLAYED_ALBUM_WINDOW_MILLIS,
                            limit = 10,
                        ).flatMapLatest { albums ->
                            val album =
                                albums
                                    .filterExplicitAlbums(hideExplicit)
                                    .firstOrNull()
                            if (album == null) {
                                flowOf(MostPlayedAlbumUiState.Empty)
                            } else {
                                database.albumWithSongs(album.id).map { albumWithSongs ->
                                    val songs = albumWithSongs?.songs.orEmpty()
                                    if (songs.isEmpty()) {
                                        MostPlayedAlbumUiState.Empty
                                    } else {
                                        MostPlayedAlbumUiState.Success(
                                            album =
                                                MostPlayedAlbumUiModel(
                                                    id = album.id,
                                                    title = album.title,
                                                    thumbnailUrl = album.thumbnailUrl,
                                                    trackCount = songs.size,
                                                    tracks = ImmutableList.copyOf(songs.map { it.toMediaMetadata() }),
                                                ),
                                        )
                                    }
                                }
                            }
                        }
                }.flowOn(Dispatchers.IO)
                .catch { throwable ->
                    if (throwable is CancellationException) throw throwable
                    reportException(throwable)
                    emit(MostPlayedAlbumUiState.Error(context.getString(R.string.error_unknown)))
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MostPlayedAlbumUiState.Loading)

        fun syncAllLibrary() {
            if (_isRefreshing.value) return
            _isRefreshing.value = true
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    syncUtils.performFullSync()
                } catch (e: Exception) {
                    timber.log.Timber.e(e, "Error during manual sync")
                    reportException(e)
                } finally {
                    _isRefreshing.value = false
                }
            }
        }

        val topValue =
            context.dataStore.data
                .map { it[TopSize] ?: "50" }
                .distinctUntilChanged()
        var artists =
            database
                .artistsBookmarked(
                    ArtistSortType.CREATE_DATE,
                    true,
                ).stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        var albums =
            context.dataStore.data
                .map { it[HideExplicitKey] ?: false }
                .distinctUntilChanged()
                .flatMapLatest { hideExplicit ->
                    database.albumsLiked(AlbumSortType.CREATE_DATE, true).map { it.filterExplicitAlbums(hideExplicit) }
                }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        var playlists =
            context.dataStore.data
                .map {
                    it[PlaylistSortTypeKey].toEnum(PlaylistSortType.CUSTOM) to (it[PlaylistSortDescendingKey] ?: true)
                }.distinctUntilChanged()
                .flatMapLatest { (sortType, descending) -> database.playlists(sortType, descending) }
                .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

        init {
            viewModelScope.launch(Dispatchers.IO) {
                albums.collect { albums ->
                    albums
                        .filter {
                            it.album.songCount == 0
                        }.forEach { album ->
                            YouTube
                                .album(album.id)
                                .onSuccess { albumPage ->
                                    database.query {
                                        update(album.album, albumPage, album.artists)
                                    }
                                }.onFailure {
                                    reportException(it)
                                    if (it.message?.contains("NOT_FOUND") == true) {
                                        database.query {
                                            delete(album.album)
                                        }
                                    }
                                }
                        }
                }
            }
            viewModelScope.launch(Dispatchers.IO) {
                artists.collect { artists ->
                    artists
                        .map { it.artist }
                        .filter {
                            it.thumbnailUrl == null ||
                                Duration.between(
                                    it.lastUpdateTime,
                                    LocalDateTime.now(),
                                ) > Duration.ofDays(10)
                        }.forEach { artist ->
                            YouTube.artist(artist.id).onSuccess { artistPage ->
                                database.query {
                                    update(artist, artistPage)
                                }
                            }
                        }
                }
            }
        }
    }

@Immutable
sealed interface MostPlayedAlbumUiState {
    data object Loading : MostPlayedAlbumUiState

    @Immutable
    data class Success(
        val album: MostPlayedAlbumUiModel,
    ) : MostPlayedAlbumUiState

    data object Empty : MostPlayedAlbumUiState

    @Immutable
    data class Error(
        val message: String,
    ) : MostPlayedAlbumUiState
}

@Immutable
data class MostPlayedAlbumUiModel(
    val id: String,
    val title: String,
    val thumbnailUrl: String?,
    val trackCount: Int,
    val tracks: ImmutableList<MediaMetadata>,
)

@HiltViewModel
class LibraryViewModel
    @Inject
    constructor() : ViewModel() {
        private val curScreen = mutableStateOf(LibraryFilter.LIBRARY)
        val filter: MutableState<LibraryFilter> = curScreen
    }
