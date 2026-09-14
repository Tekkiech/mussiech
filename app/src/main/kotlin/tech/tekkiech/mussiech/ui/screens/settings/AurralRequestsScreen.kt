/*
 * Mussiech (2026)
 * GPL-3.0 License | Contributors: see git history
 */

package tech.tekkiech.mussiech.ui.screens.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import tech.tekkiech.mussiech.R
import tech.tekkiech.mussiech.ui.component.EmptyPlaceholder
import tech.tekkiech.mussiech.ui.component.IconButton
import tech.tekkiech.mussiech.ui.utils.backToMain
import tech.tekkiech.mussiech.viewmodels.AurralRequestsUiState
import tech.tekkiech.mussiech.viewmodels.AurralRequestsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AurralRequestsScreen(
    navController: NavController,
    viewModel: AurralRequestsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.aurral_my_requests)) },
                navigationIcon = {
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain,
                    ) {
                        Icon(
                            painterResource(R.drawable.arrow_back),
                            contentDescription = null,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        when (val state = uiState) {
            is AurralRequestsUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            is AurralRequestsUiState.Error -> {
                EmptyPlaceholder(
                    icon = R.drawable.info,
                    text = stringResource(R.string.error_unknown),
                    modifier = Modifier.padding(innerPadding),
                )
            }

            is AurralRequestsUiState.Content -> {
                if (state.rows.isEmpty()) {
                    EmptyPlaceholder(
                        icon = R.drawable.info,
                        text = stringResource(R.string.no_results_found),
                        modifier = Modifier.padding(innerPadding),
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        items(items = state.rows, key = { it.id }) { row ->
                            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                                Text(
                                    text = row.displayTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                if (!row.subtitle.isNullOrBlank()) {
                                    Text(
                                        text = row.subtitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                if (!row.statusLabel.isNullOrBlank()) {
                                    Text(
                                        text = row.statusLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 4.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
