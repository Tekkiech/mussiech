/*
 * Mussiech (2026)
 * GPL-3.0 License | Contributors: see git history
 */

package tech.tekkiech.mussiech.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import tech.tekkiech.mussiech.LocalPlayerAwareWindowInsets
import tech.tekkiech.mussiech.R
import tech.tekkiech.mussiech.ui.component.IconButton
import tech.tekkiech.mussiech.ui.utils.backToMain
import tech.tekkiech.mussiech.viewmodels.AurralConnectionState
import tech.tekkiech.mussiech.viewmodels.AurralServerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AurralServerSettings(
    navController: NavController,
    viewModel: AurralServerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.aurral_server)) },
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
        Column(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            val connected = uiState.connected
            if (connected != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.aurral_connected_as, connected.serverUrl),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
                OutlinedButton(
                    onClick = { navController.navigate("settings/aurral/requests") },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                ) {
                    Text(stringResource(R.string.aurral_view_requests))
                }

                OutlinedButton(
                    onClick = viewModel::disconnect,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                ) {
                    Text(stringResource(R.string.aurral_disconnect))
                }
            } else {
                Text(
                    text = stringResource(R.string.aurral_not_connected),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp),
                )

                OutlinedTextField(
                    value = uiState.serverUrl,
                    onValueChange = viewModel::onServerUrlChange,
                    label = { Text(stringResource(R.string.aurral_server_url)) },
                    placeholder = { Text(stringResource(R.string.aurral_server_url_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    colors = OutlinedTextFieldDefaults.colors(),
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = uiState.apiKey,
                    onValueChange = viewModel::onApiKeyChange,
                    label = { Text(stringResource(R.string.aurral_api_key)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(),
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )

                val connectionState = uiState.connectionState
                if (connectionState is AurralConnectionState.Error) {
                    Text(
                        text = stringResource(R.string.aurral_connection_failed, connectionState.message),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }

                Button(
                    onClick = viewModel::connect,
                    enabled = connectionState !is AurralConnectionState.Connecting,
                    modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                ) {
                    if (connectionState is AurralConnectionState.Connecting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp).padding(end = 8.dp),
                            strokeWidth = 2.dp,
                        )
                        Text(stringResource(R.string.aurral_connecting))
                    } else {
                        Text(stringResource(R.string.aurral_connect))
                    }
                }
            }

            Text(
                text = stringResource(R.string.aurral_request_defaults_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
            )

            OutlinedTextField(
                value = uiState.rootFolderPath,
                onValueChange = viewModel::onRootFolderPathChange,
                label = { Text(stringResource(R.string.aurral_root_folder_path)) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = uiState.qualityProfileId,
                onValueChange = viewModel::onQualityProfileIdChange,
                label = { Text(stringResource(R.string.aurral_quality_profile_id)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )
        }
    }
}
