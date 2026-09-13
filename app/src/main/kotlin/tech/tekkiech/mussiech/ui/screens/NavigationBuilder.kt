/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package tech.tekkiech.mussiech.ui.screens

import android.net.Uri
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import tech.tekkiech.mussiech.BuildConfig
import tech.tekkiech.mussiech.constants.UpdateChannel
import tech.tekkiech.mussiech.defaultUpdateChannel
import tech.tekkiech.mussiech.musicrecognition.MusicRecognitionRoute
import tech.tekkiech.mussiech.musicrecognition.MusicRecognitionDetailsRoute
import tech.tekkiech.mussiech.ui.screens.BrowseScreen
import tech.tekkiech.mussiech.ui.screens.artist.ArtistAlbumsScreen
import tech.tekkiech.mussiech.ui.screens.artist.ArtistItemsScreen
import tech.tekkiech.mussiech.ui.screens.artist.ArtistScreen
import tech.tekkiech.mussiech.ui.screens.artist.ArtistSongsScreen
import tech.tekkiech.mussiech.ui.screens.library.LibraryScreen
import tech.tekkiech.mussiech.ui.screens.library.LocalSongScreen
import tech.tekkiech.mussiech.ui.screens.musicrecognition.MusicRecognitionScreen
import tech.tekkiech.mussiech.ui.screens.musicrecognition.MusicRecognitionDetailsScreen
import tech.tekkiech.mussiech.ui.screens.playlist.AutoPlaylistScreen
import tech.tekkiech.mussiech.ui.screens.playlist.CachePlaylistScreen
import tech.tekkiech.mussiech.ui.screens.playlist.LocalPlaylistScreen
import tech.tekkiech.mussiech.ui.screens.playlist.OnlinePlaylistScreen
import tech.tekkiech.mussiech.ui.screens.playlist.SpotifyPlaylistScreen
import tech.tekkiech.mussiech.ui.screens.playlist.TopPlaylistScreen
import tech.tekkiech.mussiech.ui.screens.podcast.PodcastRoute
import tech.tekkiech.mussiech.ui.screens.podcast.PodcastScreen
import tech.tekkiech.mussiech.ui.screens.search.OnlineSearchResult
import tech.tekkiech.mussiech.ui.screens.search.OnlineSearchResultArgument
import tech.tekkiech.mussiech.ui.screens.search.OnlineSearchResultRoute
import tech.tekkiech.mussiech.ui.screens.search.OnlineSearchResultRoutePrefix
import tech.tekkiech.mussiech.ui.screens.search.SearchScreen
import tech.tekkiech.mussiech.ui.screens.settings.AboutScreen
import tech.tekkiech.mussiech.ui.screens.settings.AccountSettings
import tech.tekkiech.mussiech.ui.screens.settings.AiIntegrationSettings
import tech.tekkiech.mussiech.ui.screens.settings.AodCustomizedScreen
import tech.tekkiech.mussiech.ui.screens.settings.AppearanceSettings
import tech.tekkiech.mussiech.ui.screens.settings.BackupAndRestore
import tech.tekkiech.mussiech.ui.screens.settings.ChangelogScreen
import tech.tekkiech.mussiech.ui.screens.settings.ContentSettings
import tech.tekkiech.mussiech.ui.screens.settings.CustomizeBackground
import tech.tekkiech.mussiech.ui.screens.settings.DebugSettings
import tech.tekkiech.mussiech.ui.screens.settings.DiscordSettings
import tech.tekkiech.mussiech.ui.screens.settings.HiddenPlaylistsScreen
import tech.tekkiech.mussiech.ui.screens.settings.IconScreen
import tech.tekkiech.mussiech.ui.screens.settings.IntegrationScreen
import tech.tekkiech.mussiech.ui.screens.settings.InternetSettings
import tech.tekkiech.mussiech.ui.screens.settings.LastFMSettings
import tech.tekkiech.mussiech.ui.screens.settings.LogcatScreen
import tech.tekkiech.mussiech.ui.screens.settings.LyricsAnimationSettings
import tech.tekkiech.mussiech.ui.screens.settings.LyricsSettings
import tech.tekkiech.mussiech.ui.screens.settings.MusicTogetherScreen
import tech.tekkiech.mussiech.ui.screens.settings.NavidromeServerSettings
import tech.tekkiech.mussiech.ui.screens.settings.PalettePickerScreen
import tech.tekkiech.mussiech.ui.screens.settings.PlayerSettings
import tech.tekkiech.mussiech.ui.screens.settings.PrivacySettings
import tech.tekkiech.mussiech.ui.screens.settings.SettingsScreen
import tech.tekkiech.mussiech.ui.screens.settings.StorageSettings
import tech.tekkiech.mussiech.ui.screens.settings.ThemeCreatorScreen
import tech.tekkiech.mussiech.ui.screens.settings.UpdateScreen
import tech.tekkiech.mussiech.viewmodels.HomeViewModel
import tech.tekkiech.mussiech.viewmodels.OnlineSearchSort

@OptIn(ExperimentalMaterial3Api::class)
fun NavGraphBuilder.navigationBuilder(
    navController: NavHostController,
    scrollBehavior: TopAppBarScrollBehavior,
    homeViewModel: HomeViewModel,
    latestVersionName: () -> String,
    disableAnimations: Boolean = false,
    onClearUpdateBadge: () -> Unit = {},
    homeScrollConnection: NestedScrollConnection? = null,
    searchScrollConnection: NestedScrollConnection? = null,
    onlineSearchSort: OnlineSearchSort = OnlineSearchSort.DEFAULT,
) {
    composable(Screens.Home.route) {
        HomeScreen(
            navController = navController,
            viewModel = homeViewModel,
            headerScrollConnection = homeScrollConnection,
        )
    }
    composable(
        Screens.Library.route,
    ) {
        LibraryScreen(navController)
    }
    composable(Screens.Search.route) {
        SearchScreen(
            navController = navController,
            onSearchClick = {
                navController.currentBackStackEntry
                    ?.savedStateHandle
                    ?.set("openSearch", true)
            },
            headerScrollConnection = searchScrollConnection,
        )
    }
    composable("local_songs") {
        LocalSongScreen(navController)
    }
    composable("history") {
        HistoryScreen(navController)
    }
    composable("stats") {
        StatsScreen(navController)
    }
    composable("news") {
        NewsScreen(navController)
    }
    composable(
        route = "view_news/{newsId}",
        arguments =
            listOf(
                navArgument("newsId") { type = NavType.StringType },
            ),
    ) {
        ViewNewsScreen(navController)
    }
    composable(
        route = "year_in_music?year={year}",
        arguments =
            listOf(
                navArgument("year") {
                    type = NavType.IntType
                    defaultValue = -1
                },
            ),
    ) { backStackEntry ->
        val selectedYear = backStackEntry.arguments?.getInt("year")?.takeIf { it > 0 }
        YearInMusicScreen(
            navController = navController,
            initialYear = selectedYear,
        )
    }
    composable(MusicRecognitionRoute) {
        MusicRecognitionScreen(navController)
    }
    composable(MusicRecognitionDetailsRoute) { backStackEntry ->
        val encodedTrack = backStackEntry.arguments?.getString("encodedTrack").orEmpty()
        MusicRecognitionDetailsScreen(navController, encodedTrack)
    }
    composable(Screens.MoodAndGenres.route) {
        MoodAndGenresScreen(navController)
    }
    composable("account") {
        AccountScreen(navController, scrollBehavior)
    }
    composable("new_release") {
        NewReleaseScreen(navController, scrollBehavior)
    }
    composable("charts_screen") {
        ChartsScreen(navController)
    }
    composable(
        route = "browse/{browseId}",
        arguments =
            listOf(
                navArgument("browseId") {
                    type = NavType.StringType
                },
            ),
    ) {
        BrowseScreen(
            navController,
            scrollBehavior,
            it.arguments?.getString("browseId"),
        )
    }
    composable(
        route = OnlineSearchResultRoute,
        arguments =
            listOf(
                navArgument(OnlineSearchResultArgument) {
                    type = NavType.StringType
                },
            ),
        enterTransition = {
            if (disableAnimations) {
                fadeIn(tween(0))
            } else {
                fadeIn(tween(250))
            }
        },
        exitTransition = {
            if (disableAnimations) {
                fadeOut(tween(0))
            } else if (targetState.destination.route?.startsWith(OnlineSearchResultRoutePrefix) == true) {
                fadeOut(tween(200))
            } else {
                fadeOut(tween(200)) + slideOutHorizontally { -it / 2 }
            }
        },
        popEnterTransition = {
            if (disableAnimations) {
                fadeIn(tween(0))
            } else if (initialState.destination.route?.startsWith(OnlineSearchResultRoutePrefix) == true) {
                fadeIn(tween(250))
            } else {
                fadeIn(tween(250)) + slideInHorizontally { -it / 2 }
            }
        },
        popExitTransition = {
            if (disableAnimations) {
                fadeOut(tween(0))
            } else {
                fadeOut(tween(200))
            }
        },
    ) {
        OnlineSearchResult(
            navController = navController,
            searchSort = onlineSearchSort,
        )
    }
    composable(
        route = "album/{albumId}",
        arguments =
            listOf(
                navArgument("albumId") {
                    type = NavType.StringType
                },
            ),
    ) {
        AlbumScreen(navController, scrollBehavior)
    }
    composable(
        route = PodcastRoute,
        arguments =
            listOf(
                navArgument("browseId") {
                    type = NavType.StringType
                },
            ),
    ) {
        PodcastScreen(navController)
    }
    composable(
        route = "artist/{artistId}",
        arguments =
            listOf(
                navArgument("artistId") {
                    type = NavType.StringType
                },
            ),
    ) {
        ArtistScreen(navController, scrollBehavior)
    }
    composable(
        route = "artist/{artistId}/songs",
        arguments =
            listOf(
                navArgument("artistId") {
                    type = NavType.StringType
                },
            ),
    ) {
        ArtistSongsScreen(navController, scrollBehavior)
    }
    composable(
        route = "artist/{artistId}/albums",
        arguments =
            listOf(
                navArgument("artistId") {
                    type = NavType.StringType
                },
            ),
    ) {
        ArtistAlbumsScreen(navController, scrollBehavior)
    }
    composable(
        route = "artist/{artistId}/items?browseId={browseId}&params={params}",
        arguments =
            listOf(
                navArgument("artistId") {
                    type = NavType.StringType
                },
                navArgument("browseId") {
                    type = NavType.StringType
                    nullable = true
                },
                navArgument("params") {
                    type = NavType.StringType
                    nullable = true
                },
            ),
    ) {
        ArtistItemsScreen(navController, scrollBehavior)
    }
    composable(
        route = "online_playlist/{playlistId}",
        arguments =
            listOf(
                navArgument("playlistId") {
                    type = NavType.StringType
                },
            ),
    ) {
        OnlinePlaylistScreen(navController, scrollBehavior)
    }
    composable(
        route = "local_playlist/{playlistId}",
        arguments =
            listOf(
                navArgument("playlistId") {
                    type = NavType.StringType
                },
            ),
    ) {
        LocalPlaylistScreen(navController, scrollBehavior)
    }
    composable(
        route = "spotify_playlist/{playlistId}",
        arguments =
            listOf(
                navArgument("playlistId") {
                    type = NavType.StringType
                },
            ),
    ) {
        SpotifyPlaylistScreen(navController, scrollBehavior)
    }
    composable(
        route = "auto_playlist/{playlist}?tab={tab}",
        arguments =
            listOf(
                navArgument("playlist") {
                    type = NavType.StringType
                },
                navArgument("tab") {
                    type = NavType.StringType
                    defaultValue = "downloaded"
                },
            ),
    ) {
        AutoPlaylistScreen(navController, scrollBehavior)
    }
    composable(
        route = "cache_playlist/{playlist}",
        arguments =
            listOf(
                navArgument("playlist") {
                    type = NavType.StringType
                },
            ),
    ) {
        CachePlaylistScreen(navController, scrollBehavior)
    }
    composable(
        route = "top_playlist/{top}",
        arguments =
            listOf(
                navArgument("top") {
                    type = NavType.StringType
                },
            ),
    ) {
        TopPlaylistScreen(navController, scrollBehavior)
    }
    composable(
        route = "youtube_browse/{browseId}?params={params}",
        arguments =
            listOf(
                navArgument("browseId") {
                    type = NavType.StringType
                    nullable = true
                },
                navArgument("params") {
                    type = NavType.StringType
                    nullable = true
                },
            ),
    ) {
        YouTubeBrowseScreen(navController)
    }
    composable("settings") {
        SettingsScreen(navController, latestVersionName())
    }
    composable("settings/account") {
        AccountSettings(
            navController = navController,
            latestVersionName = latestVersionName(),
            viewModel = homeViewModel,
        )
    }
    composable("settings/navidrome") {
        NavidromeServerSettings(navController)
    }
    composable("settings/hidden_playlists") {
        HiddenPlaylistsScreen(navController)
    }
    composable("settings/appearance") {
        AppearanceSettings(navController)
    }
    composable("settings/appearance/icon") {
        IconScreen(navController)
    }
    composable("settings/appearance/aod_customized") {
        AodCustomizedScreen(navController)
    }
    composable("settings/appearance/palette_picker") {
        PalettePickerScreen(navController)
    }
    composable("settings/appearance/lyrics_animations") {
        LyricsAnimationSettings(navController)
    }
    composable("settings/appearance/theme_creator") {
        ThemeCreatorScreen(navController)
    }
    composable("settings/content") {
        ContentSettings(navController)
    }
    composable("settings/lyrics") {
        LyricsSettings(navController)
    }
    composable("settings/internet") {
        InternetSettings(navController)
    }
    composable("settings/player") {
        PlayerSettings(navController)
    }
    composable("settings/storage") {
        StorageSettings(navController)
    }
    composable("settings/privacy") {
        PrivacySettings(navController)
    }
    composable("settings/backup_restore") {
        BackupAndRestore(navController)
    }
    composable("settings/discord") {
        DiscordSettings(navController)
    }
    composable("settings/integration") {
        IntegrationScreen(navController)
    }
    composable("settings/ai_integration") {
        AiIntegrationSettings(navController)
    }
    composable("settings/music_together") {
        MusicTogetherScreen(navController)
    }
    composable("settings/lastfm") {
        LastFMSettings(navController)
    }
    composable("settings/discord/experimental") {
        tech.tekkiech.mussiech.ui.screens.settings
            .DiscordExperimental(navController)
    }
    composable("settings/misc") {
        DebugSettings(navController)
    }
    composable("settings/logcat") {
        LogcatScreen(navController)
    }
    if (BuildConfig.UPDATER_AVAILABLE) {
        composable("settings/update") {
            UpdateScreen(navController, onUpToDate = onClearUpdateBadge)
        }
    }
    composable(
        route = "settings/changelog?channel={channel}",
        arguments =
            listOf(
                navArgument("channel") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
    ) { backStackEntry ->
        val channelName = backStackEntry.arguments?.getString("channel")
        val channel = UpdateChannel.fromStoredName(channelName, defaultUpdateChannel)
        ChangelogScreen(navController, channel = channel)
    }
    composable("settings/about") {
        AboutScreen(navController)
    }
    composable("customize_background") {
        CustomizeBackground(navController)
    }
    composable(
        route = "$LOGIN_ROUTE?$LOGIN_URL_ARGUMENT={$LOGIN_URL_ARGUMENT}",
        arguments =
            listOf(
                navArgument(LOGIN_URL_ARGUMENT) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
    ) { backStackEntry ->
        LoginScreen(
            navController,
            startUrl = backStackEntry.arguments?.getString(LOGIN_URL_ARGUMENT)?.let(Uri::decode),
        )
    }
}
