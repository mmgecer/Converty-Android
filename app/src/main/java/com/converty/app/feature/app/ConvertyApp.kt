@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.converty.app.feature.app

import android.Manifest
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.converty.app.R
import com.converty.app.core.model.ConversionOutput
import com.converty.app.ui.theme.ConvertyTheme
import com.converty.app.ui.theme.ExpressiveNavigationBar
import com.converty.app.ui.theme.ExpressiveNavigationBarItem
import com.converty.app.ui.theme.ApplyAppLocale
import kotlinx.coroutines.launch

private enum class TreePickerTarget { SETUP, SETTINGS }

@Composable
fun ConvertyApp(viewModel: ConvertyViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    if (state.settingsReady) {
        ApplyAppLocale(preference = state.settings.language)
    }
    ConvertyTheme(
        themeMode = state.settings.themeMode,
        palette = state.settings.themePalette,
        dynamicColor = state.settings.useDynamicColor,
    ) {
        val context = LocalContext.current
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        var treeTarget by remember { mutableStateOf(TreePickerTarget.SETUP) }

        val filePicker = rememberLauncherForActivityResult(
            ActivityResultContracts.OpenMultipleDocuments(),
        ) { uris ->
            viewModel.onAction(ConvertyAction.FilesPicked(uris))
        }
        val outputTreePicker = rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocumentTree(),
        ) { uri ->
            uri?.let {
                val action = when (treeTarget) {
                    TreePickerTarget.SETUP -> ConvertyAction.OutputTreePicked(it)
                    TreePickerTarget.SETTINGS -> ConvertyAction.DefaultOutputTreePicked(it)
                }
                viewModel.onAction(action)
            }
        }
        val notificationPermissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) {
            // A denied notification permission must not block an explicitly requested conversion.
            viewModel.onAction(ConvertyAction.SubmitConversion)
        }

        val noticeMessage = state.notice?.let { notice ->
            stringResource(
                when (notice) {
                    UiNotice.FILE_UNAVAILABLE -> R.string.error_file_unavailable
                    UiNotice.OUTPUT_UNAVAILABLE -> R.string.error_output_unavailable
                    UiNotice.GENERIC_ERROR -> R.string.error_generic
                },
            )
        }
        LaunchedEffect(noticeMessage) {
            noticeMessage?.let {
                snackbarHostState.showSnackbar(it)
                viewModel.onAction(ConvertyAction.NoticeShown)
            }
        }

        val cannotOpen = stringResource(R.string.error_cannot_open)
        val cannotShare = stringResource(R.string.error_cannot_share)
        val openOutput: (ConversionOutput) -> Unit = { output ->
            if (!openOutput(context, output)) {
                scope.launch { snackbarHostState.showSnackbar(cannotOpen) }
            }
        }
        val shareOutput: (ConversionOutput) -> Unit = { output ->
            if (!shareOutput(context, output)) {
                scope.launch { snackbarHostState.showSnackbar(cannotShare) }
            }
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                ExpressiveNavigationBar {
                    AppDestination.entries.forEach { destination ->
                        ExpressiveNavigationBarItem(
                            selected = state.destination == destination,
                            onClick = { viewModel.onAction(ConvertyAction.Navigate(destination)) },
                            icon = {
                                Icon(
                                    imageVector = when (destination) {
                                        AppDestination.HOME -> Icons.Rounded.Home
                                        AppDestination.QUEUE -> Icons.Rounded.SwapVert
                                        AppDestination.HISTORY -> Icons.Rounded.History
                                        AppDestination.SETTINGS -> Icons.Rounded.Settings
                                    },
                                    contentDescription = null,
                                )
                            },
                            label = {
                                Text(
                                    stringResource(
                                        when (destination) {
                                            AppDestination.HOME -> R.string.nav_home
                                            AppDestination.QUEUE -> R.string.nav_queue
                                            AppDestination.HISTORY -> R.string.nav_history
                                            AppDestination.SETTINGS -> R.string.nav_settings
                                        },
                                    ),
                                )
                            },
                        )
                    }
                }
            },
        ) { padding ->
            when (state.destination) {
                AppDestination.HOME -> HomeScreen(
                    direction = state.homeDirection,
                    onSourceChanged = { viewModel.onAction(ConvertyAction.HomeSourceChanged(it)) },
                    onTargetChanged = { viewModel.onAction(ConvertyAction.HomeTargetChanged(it)) },
                    onSwap = { viewModel.onAction(ConvertyAction.SwapHomeFormats) },
                    onContinue = { viewModel.onAction(ConvertyAction.StartSelectedSetup) },
                    modifier = Modifier.padding(padding),
                )
                AppDestination.QUEUE -> QueueScreen(
                    jobs = state.queue,
                    onCancel = { viewModel.onAction(ConvertyAction.CancelJob(it)) },
                    modifier = Modifier.padding(padding),
                )
                AppDestination.HISTORY -> HistoryScreen(
                    jobs = state.history,
                    onOpen = openOutput,
                    onShare = shareOutput,
                    onRetry = { viewModel.onAction(ConvertyAction.RetryJob(it)) },
                    onDelete = { viewModel.onAction(ConvertyAction.DeleteJob(it)) },
                    onClear = { viewModel.onAction(ConvertyAction.ClearHistory) },
                    modifier = Modifier.padding(padding),
                )
                AppDestination.SETTINGS -> SettingsScreen(
                    settings = state.settings,
                    onAction = viewModel::onAction,
                    onChooseOutputTree = {
                        treeTarget = TreePickerTarget.SETTINGS
                        outputTreePicker.launch(
                            state.settings.defaultOutputTreeUri?.let(Uri::parse),
                        )
                    },
                    modifier = Modifier.padding(padding),
                )
            }
        }

        state.setup?.let { setup ->
            ConversionSetupSheet(
                state = setup,
                onAction = { action ->
                    val needsNotificationPermission = action == ConvertyAction.SubmitConversion &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS,
                        ) != PackageManager.PERMISSION_GRANTED
                    if (needsNotificationPermission) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        viewModel.onAction(action)
                    }
                },
                onSelectFiles = {
                    filePicker.launch(setup.direction.sourceFormat.mimeTypes.toTypedArray())
                },
                onChooseOutputTree = {
                    treeTarget = TreePickerTarget.SETUP
                    outputTreePicker.launch(setup.outputTreeUri?.let(Uri::parse))
                },
            )
        }
    }
}

private fun openOutput(context: Context, output: ConversionOutput): Boolean = runCatching {
    val uri = Uri.parse(output.uri)
    val intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(uri, output.mimeType)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.open)))
}.isSuccess

private fun shareOutput(context: Context, output: ConversionOutput): Boolean = runCatching {
    val uri = Uri.parse(output.uri)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = output.mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newRawUri(output.displayName, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.share)))
}.isSuccess
