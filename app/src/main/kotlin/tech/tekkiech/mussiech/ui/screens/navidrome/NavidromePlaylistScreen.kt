/*
 * Mussiech (2026)
 * GPL-3.0 License | Contributors: see git history
 */

package tech.tekkiech.mussiech.ui.screens.navidrome

import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import tech.tekkiech.mussiech.viewmodels.NavidromePlaylistViewModel

@Composable
fun NavidromePlaylistScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: NavidromePlaylistViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    NavidromeDetailScreenContent(uiState)
}
