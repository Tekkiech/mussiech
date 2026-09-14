/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package tech.tekkiech.mussiech.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlinx.coroutines.CoroutineScope
import tech.tekkiech.mussiech.LocalPlayerAwareWindowInsets
import tech.tekkiech.mussiech.LocalPlayerConnection
import tech.tekkiech.mussiech.R
import tech.tekkiech.mussiech.constants.QuickPicks
import tech.tekkiech.mussiech.home.HomeAction
import tech.tekkiech.mussiech.home.HomeEvent
import tech.tekkiech.mussiech.home.HomeScreenState
import tech.tekkiech.mussiech.home.HomeUiState
import tech.tekkiech.mussiech.models.MediaMetadata
import tech.tekkiech.mussiech.extensions.toMediaItem
import tech.tekkiech.mussiech.playback.PlayerConnection
import tech.tekkiech.mussiech.playback.queues.ListQueue
import tech.tekkiech.mussiech.ui.component.ExpressivePullToRefreshBox
import tech.tekkiech.mussiech.ui.component.LocalMenuState
import tech.tekkiech.mussiech.ui.component.MenuState
import tech.tekkiech.mussiech.ui.utils.SnapLayoutInfoProvider
import tech.tekkiech.mussiech.viewmodels.HomeViewModel

private val HomeFeedMaxWidth = 1_200.dp
private val HomeSectionSpacing = 18.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel,
    headerScrollConnection: NestedScrollConnection? = null,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current

    val screenState by viewModel.screenState.collectAsStateWithLifecycle()
    val isPlaying by playerConnection.isPlaying.collectAsStateWithLifecycle()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel, playerConnection, navController) {
        viewModel.events.collect { event ->
            when (event) {
                is HomeEvent.OpenPodcast -> navController.navigate("podcast/${Uri.encode(event.browseId)}")
                is HomeEvent.PlayPodcastEpisode -> {
                    playerConnection.playQueue(
                        ListQueue(
                            title = event.request.title,
                            items = event.request.items.map { metadata -> metadata.toMediaItem() },
                            startIndex = event.request.startIndex,
                        ),
                    )
                }
            }
        }
    }

    val lazyListState = rememberLazyListState()
    val forgottenFavoritesGridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop =
        backStackEntry
            ?.savedStateHandle
            ?.getStateFlow("scrollToTop", false)
            ?.collectAsStateWithLifecycle()

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            lazyListState.animateScrollToItem(0)
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    val successState = screenState as? HomeScreenState.Success
    val uiState = successState?.uiState

    LaunchedEffect(uiState?.forgottenFavorites) {
        if (uiState != null) {
            forgottenFavoritesGridState.scrollToItem(0)
        }
    }

    // Attach the shell's floating-header connection inside this screen (Step 2b) so
    // Home's scroll/fling writes Home's own header state and can't leak into another
    // route's header. Bubbling reaches this ancestor Box before any shell connection.
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .then(
                    if (headerScrollConnection != null) {
                        Modifier.nestedScroll(headerScrollConnection)
                    } else {
                        Modifier
                    },
                ),
    ) {
        when (val state = screenState) {
            HomeScreenState.Loading -> {
                HomeStatePane(
                    iconResId = null,
                    messageResId = null,
                    showLoadingIndicator = true,
                )
            }

            HomeScreenState.Empty -> {
                HomeStatePane(
                    iconResId = R.drawable.music_note,
                    messageResId = R.string.no_results_found,
                    actionResId = R.string.retry,
                    onAction = { viewModel.onAction(HomeAction.Refresh) },
                )
            }

            is HomeScreenState.Error -> {
                HomeStatePane(
                    iconResId = R.drawable.info,
                    messageResId = state.messageResId,
                    actionResId = R.string.retry,
                    onAction = { viewModel.onAction(HomeAction.Refresh) },
                )
            }

            is HomeScreenState.Success -> {
                HomeContent(
                    uiState = state.uiState,
                    mediaMetadata = mediaMetadata,
                    isPlaying = isPlaying,
                    navController = navController,
                    playerConnection = playerConnection,
                    menuState = menuState,
                    haptic = haptic,
                    scope = scope,
                    lazyListState = lazyListState,
                    forgottenFavoritesGridState = forgottenFavoritesGridState,
                    onAction = viewModel::onAction,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HomeStatePane(
    @DrawableRes iconResId: Int?,
    @StringRes messageResId: Int?,
    modifier: Modifier = Modifier,
    @StringRes actionResId: Int? = null,
    showLoadingIndicator: Boolean = false,
    onAction: (() -> Unit)? = null,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .fillMaxSize()
                .padding(LocalPlayerAwareWindowInsets.current.asPaddingValues()),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp),
        ) {
            if (showLoadingIndicator) {
                LoadingIndicator()
            } else {
                iconResId?.let {
                    Icon(
                        painter = painterResource(it),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp),
                    )
                }
                messageResId?.let {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(it),
                        style = MaterialTheme.typography.titleLargeEmphasized,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                if (actionResId != null && onAction != null) {
                    Spacer(Modifier.height(20.dp))
                    FilledTonalButton(onClick = onAction) {
                        Text(stringResource(actionResId))
                    }
                }
            }
        }
    }
}

@OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
private fun HomeContent(
    uiState: HomeUiState,
    mediaMetadata: MediaMetadata?,
    isPlaying: Boolean,
    navController: NavController,
    playerConnection: PlayerConnection,
    menuState: MenuState,
    haptic: HapticFeedback,
    scope: CoroutineScope,
    lazyListState: androidx.compose.foundation.lazy.LazyListState,
    forgottenFavoritesGridState: LazyGridState,
    onAction: (HomeAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val remoteQuickPicks =
        uiState
            .takeIf { it.quickPicksMode == QuickPicks.QUICK_PICKS }
            ?.remoteQuickPicks
    val tonalStart = MaterialTheme.colorScheme.primaryContainer
    val tonalMiddle = MaterialTheme.colorScheme.secondaryContainer
    Box(modifier = modifier.fillMaxSize()) {
        if (uiState.showTonalBackdrop) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(430.dp)
                        .align(Alignment.TopCenter)
                        .drawWithCache {
                            val brush =
                                Brush.verticalGradient(
                                    0f to tonalStart.copy(alpha = 0.30f),
                                    0.42f to tonalMiddle.copy(alpha = 0.14f),
                                    1f to Color.Transparent,
                                )
                            onDrawBehind { drawRect(brush) }
                        },
            )
        }

        ExpressivePullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { onAction(HomeAction.Refresh) },
            modifier = Modifier.fillMaxSize(),
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val forgottenItemWidthFactor = if (maxWidth * 0.475f >= 320.dp) 0.475f else 0.9f
                val forgottenItemWidth = maxWidth.coerceAtMost(HomeFeedMaxWidth) * forgottenItemWidthFactor
                val forgottenSnapLayoutInfoProvider =
                    remember(forgottenFavoritesGridState, forgottenItemWidthFactor) {
                        SnapLayoutInfoProvider(
                            lazyGridState = forgottenFavoritesGridState,
                            positionInLayout = { layoutSize, itemSize ->
                                layoutSize * forgottenItemWidthFactor / 2f - itemSize / 2f
                            },
                        )
                    }

                LazyColumn(
                    state = lazyListState,
                    contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
                    modifier =
                        Modifier
                            .widthIn(max = HomeFeedMaxWidth)
                            .fillMaxWidth()
                            .align(Alignment.TopCenter),
                ) {
                    if (remoteQuickPicks?.items?.isNotEmpty() == true) {
                        item(
                            key = "home_remote_quick_picks_header",
                            contentType = "section_header",
                        ) {
                            HomeSectionHeader(
                                title = stringResource(R.string.quick_picks),
                                modifier = Modifier.animateItem(),
                            )
                        }
                        item(
                            key = "home_remote_quick_picks",
                            contentType = "quick_picks",
                        ) {
                            RemoteQuickPicksSection(
                                section = remoteQuickPicks,
                                mediaMetadata = mediaMetadata,
                                isPlaying = isPlaying,
                                displayMode = uiState.quickPicksDisplayMode,
                                navController = navController,
                                playerConnection = playerConnection,
                                menuState = menuState,
                                haptic = haptic,
                                modifier = Modifier.animateItem(),
                            )
                        }
                    } else if (
                        uiState.quickPicksMode == QuickPicks.LAST_LISTEN &&
                            uiState.quickPicks.isNotEmpty()
                    ) {
                        item(
                            key = "home_quick_picks_header",
                            contentType = "section_header",
                        ) {
                            HomeSectionHeader(
                                title = stringResource(R.string.quick_picks),
                                modifier = Modifier.animateItem(),
                            )
                        }
                        item(
                            key = "home_quick_picks",
                            contentType = "quick_picks",
                        ) {
                            QuickPicksSection(
                                quickPicks = uiState.quickPicks,
                                mediaMetadata = mediaMetadata,
                                isPlaying = isPlaying,
                                displayMode = uiState.quickPicksDisplayMode,
                                navController = navController,
                                playerConnection = playerConnection,
                                menuState = menuState,
                                haptic = haptic,
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }

                    if (uiState.speedDialItems.isNotEmpty()) {
                        sectionSpacer("speed_dial")
                        item(
                            key = "home_speed_dial_header",
                            contentType = "section_header",
                        ) {
                            HomeSectionHeader(
                                title = stringResource(R.string.speed_dial),
                                modifier = Modifier.animateItem(),
                            )
                        }
                        item(
                            key = "home_speed_dial",
                            contentType = "speed_dial",
                        ) {
                            SpeedDialSection(
                                speedDialItems = uiState.speedDialItems,
                                mediaMetadata = mediaMetadata,
                                isPlaying = isPlaying,
                                navController = navController,
                                playerConnection = playerConnection,
                                menuState = menuState,
                                haptic = haptic,
                                scope = scope,
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }

                    if (uiState.keepListening.isNotEmpty()) {
                        sectionSpacer("keep_listening")
                        item(
                            key = "home_keep_listening_header",
                            contentType = "section_header",
                        ) {
                            HomeSectionHeader(
                                title = stringResource(R.string.keep_listening),
                                modifier = Modifier.animateItem(),
                            )
                        }
                        item(
                            key = "home_keep_listening",
                            contentType = "media_shelf",
                        ) {
                            KeepListeningSection(
                                keepListening = uiState.keepListening,
                                mediaMetadata = mediaMetadata,
                                isPlaying = isPlaying,
                                navController = navController,
                                playerConnection = playerConnection,
                                menuState = menuState,
                                haptic = haptic,
                                scope = scope,
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }

                    if (uiState.navidromePlaylists.isNotEmpty()) {
                        sectionSpacer("navidrome_playlists")
                        item(
                            key = "home_navidrome_playlists_header",
                            contentType = "section_header",
                        ) {
                            HomeSectionHeader(
                                title = stringResource(R.string.playlists),
                                modifier = Modifier.animateItem(),
                            )
                        }
                        item(
                            key = "home_navidrome_playlists",
                            contentType = "media_shelf",
                        ) {
                            NavidromePlaylistsSection(
                                playlists = uiState.navidromePlaylists,
                                navController = navController,
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }

                    if (uiState.navidromeHomeAlbums.isNotEmpty()) {
                        sectionSpacer("navidrome_albums")
                        item(
                            key = "home_navidrome_albums_header",
                            contentType = "section_header",
                        ) {
                            HomeSectionHeader(
                                title = stringResource(R.string.albums),
                                modifier = Modifier.animateItem(),
                            )
                        }
                        item(
                            key = "home_navidrome_albums",
                            contentType = "media_shelf",
                        ) {
                            NavidromeAlbumsSection(
                                albums = uiState.navidromeHomeAlbums,
                                navController = navController,
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }

                    if (uiState.forgottenFavorites.isNotEmpty()) {
                        sectionSpacer("forgotten_favorites")
                        item(
                            key = "home_forgotten_favorites_header",
                            contentType = "section_header",
                        ) {
                            HomeSectionHeader(
                                title = stringResource(R.string.forgotten_favorites),
                                modifier = Modifier.animateItem(),
                            )
                        }
                        item(
                            key = "home_forgotten_favorites",
                            contentType = "song_shelf",
                        ) {
                            ForgottenFavoritesSection(
                                forgottenFavorites = uiState.forgottenFavorites,
                                mediaMetadata = mediaMetadata,
                                isPlaying = isPlaying,
                                horizontalLazyGridItemWidth = forgottenItemWidth,
                                lazyGridState = forgottenFavoritesGridState,
                                snapLayoutInfoProvider = forgottenSnapLayoutInfoProvider,
                                navController = navController,
                                playerConnection = playerConnection,
                                menuState = menuState,
                                haptic = haptic,
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }

                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.sectionSpacer(key: String) {
    item(
        key = "home_section_spacer_$key",
        contentType = "section_spacer",
    ) {
        Spacer(Modifier.height(HomeSectionSpacing))
    }
}
