/*
 * Mussiech (2026)
 * GPL-3.0 License | Contributors: see git history
 */

package tech.tekkiech.mussiech.models

import dev.zt64.subsonic.api.model.Album as SubsonicAlbum
import dev.zt64.subsonic.api.model.Playlist as SubsonicPlaylist
import dev.zt64.subsonic.api.model.Song as SubsonicSong
import tech.tekkiech.mussiech.playback.stream.StreamSource
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

fun SubsonicSong.toMediaMetadata(coverArtUrl: String?): MediaMetadata =
    MediaMetadata(
        id = id,
        title = title,
        artists =
            artists
                .map { MediaMetadata.Artist(id = it.id, name = it.name) }
                .ifEmpty {
                    listOfNotNull(
                        artistName?.let { MediaMetadata.Artist(id = artistId, name = it) },
                    )
                },
        duration = duration?.inWholeSeconds?.toInt() ?: -1,
        thumbnailUrl = coverArtUrl,
        album = albumId?.let { MediaMetadata.Album(id = it, title = albumTitle.orEmpty()) },
        setVideoId = null,
        spotifyTrackId = null,
        explicit = explicitStatus == SubsonicSong.ExplicitStatus.EXPLICIT,
        liked = starredAt != null,
        likedDate = starredAt?.toLocalDateTimeUtc(),
        inLibrary = null,
        isMusicVideo = false,
        isPodcast = false,
        source = StreamSource.NAVIDROME,
    )

fun SubsonicAlbum.toNavidromeAlbum(coverArtUrl: String?): NavidromeAlbum =
    NavidromeAlbum(
        id = id,
        title = name,
        artistName = artistName,
        songCount = songCount,
        thumbnailUrl = coverArtUrl,
    )

fun SubsonicPlaylist.toNavidromePlaylist(coverArtUrl: String?): NavidromePlaylist =
    NavidromePlaylist(
        id = id,
        name = name,
        owner = owner,
        songCount = songCount,
        thumbnailUrl = coverArtUrl,
    )

private fun kotlin.time.Instant.toLocalDateTimeUtc(): LocalDateTime =
    LocalDateTime.ofInstant(Instant.ofEpochMilli(toEpochMilliseconds()), ZoneOffset.UTC)
