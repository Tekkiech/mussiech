/*
 * Mussiech (2026)
 * GPL-3.0 License | Contributors: see git history
 */

package tech.tekkiech.mussiech.home

import dev.zt64.subsonic.api.model.AlbumListType
import tech.tekkiech.mussiech.models.NavidromeAlbum
import tech.tekkiech.mussiech.models.NavidromePlaylist
import tech.tekkiech.mussiech.models.toNavidromeAlbum
import tech.tekkiech.mussiech.models.toNavidromePlaylist
import tech.tekkiech.mussiech.repository.NavidromeRepository
import javax.inject.Inject

class LoadNavidromeHomePlaylistsUseCase
    @Inject
    constructor(
        private val navidromeRepository: NavidromeRepository,
    ) {
        suspend operator fun invoke(): Result<List<NavidromePlaylist>> =
            navidromeRepository.getPlaylists().map { playlists ->
                playlists.map { playlist ->
                    val coverArtUrl =
                        playlist.coverArtId?.let { coverArtId ->
                            navidromeRepository.getCoverArtUrl(coverArtId).getOrNull()
                        }
                    playlist.toNavidromePlaylist(coverArtUrl)
                }
            }
    }

class LoadNavidromeHomeAlbumsUseCase
    @Inject
    constructor(
        private val navidromeRepository: NavidromeRepository,
    ) {
        suspend operator fun invoke(): Result<List<NavidromeAlbum>> =
            navidromeRepository.getAlbums(AlbumListType.Random, size = 20).map { albums ->
                albums.map { album ->
                    val coverArtUrl =
                        album.coverArtId?.let { coverArtId ->
                            navidromeRepository.getCoverArtUrl(coverArtId).getOrNull()
                        }
                    album.toNavidromeAlbum(coverArtUrl)
                }
            }
    }
