/*
 * Mussiech (2026)
 * GPL-3.0 License | Contributors: see git history
 */

package tech.tekkiech.mussiech.playback.stream

import tech.tekkiech.mussiech.repository.NavidromeRepository
import javax.inject.Inject

/**
 * Resolves playback for a Navidrome-hosted song. Unlike [YoutubeiStreamRepository], a Navidrome
 * stream URL already carries its own auth params and doesn't expire on a short signed-URL clock,
 * and unlike YouTube there's no need to wait for the player to resolve real format info after
 * playback starts - the Subsonic API's own song metadata (bitRate/mimeType/sampleRate/fileSize)
 * already carries it, the same source Navic reads file info from. Ignores
 * [AudioStreamRequest.priority] entirely - Navidrome resolution is just URL construction, no real
 * foreground/background network priority concept applies the way it does for YouTube's resolver.
 */
class SubsonicStreamRepository
    @Inject
    constructor(
        private val navidromeRepository: NavidromeRepository,
    ) : AudioStreamRepository {
        override suspend fun resolve(request: AudioStreamRequest): ResolvedAudioStream {
            val streamUrl = navidromeRepository.getStreamUrl(request.mediaId).getOrThrow()
            val song = navidromeRepository.getSong(request.mediaId).getOrNull()
            return ResolvedAudioStream(
                url = streamUrl,
                requestHeaders = emptyMap(),
                formatId = 0,
                mimeType = song?.mimeType ?: "audio/*",
                codecs = "",
                bitrate = (song?.bitRate ?: 0) * 1000,
                sampleRate = song?.sampleRate,
                contentLength = song?.fileSize ?: -1,
                expiresAtMs = Long.MAX_VALUE,
                authFingerprint = "navidrome",
                source = StreamSource.NAVIDROME,
            )
        }
    }
