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
 * so most of [ResolvedAudioStream]'s YTM-oriented fields (formatId/codecs/bitrate/loudness/
 * playbackTrackingUrl) are unknown ahead of time and left at safe defaults - Media3 resolves the
 * real format from the response itself once playback starts.
 *
 * Not yet wired into [ResolveAudioStreamUseCase] as the live resolver: that use case is still
 * hard-wired to [YoutubeiStreamRepository] directly (not this interface) and unconditionally runs
 * YTM-specific PoToken refresh logic before resolving. Nothing in the app produces Navidrome-sourced
 * media IDs yet (Home/Search/etc. aren't rewired until later phases), so cutting over the use case
 * is deferred until there's a real caller to wire it against.
 */
class SubsonicStreamRepository
    @Inject
    constructor(
        private val navidromeRepository: NavidromeRepository,
    ) : AudioStreamRepository {
        override suspend fun resolve(request: AudioStreamRequest): ResolvedAudioStream {
            val streamUrl = navidromeRepository.getStreamUrl(request.mediaId).getOrThrow()
            return ResolvedAudioStream(
                url = streamUrl,
                requestHeaders = emptyMap(),
                formatId = 0,
                mimeType = "audio/*",
                codecs = "",
                bitrate = 0,
                sampleRate = null,
                contentLength = -1,
                expiresAtMs = Long.MAX_VALUE,
                authFingerprint = "navidrome",
                source = StreamSource.NAVIDROME,
            )
        }
    }
