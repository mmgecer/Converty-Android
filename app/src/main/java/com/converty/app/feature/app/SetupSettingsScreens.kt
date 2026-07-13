@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.converty.app.feature.app

import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.FitScreen
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.converty.app.R
import com.converty.app.core.model.ContentFit
import com.converty.app.core.model.ConversionDirection
import com.converty.app.core.model.ConversionQuality
import com.converty.app.core.model.FileFormat
import com.converty.app.core.model.selection.SelectionMode
import com.converty.app.core.settings.AppSettings
import com.converty.app.core.settings.LanguagePreference
import com.converty.app.core.settings.ThemeMode
import com.converty.app.core.settings.ThemePalette
import com.converty.app.ui.theme.ExpressiveCircularProgress
import com.converty.app.ui.theme.SupportedAppLocales
import com.converty.app.ui.theme.previewColors

@Composable
fun ConversionSetupSheet(
    state: ConversionSetupState,
    onAction: (ConvertyAction) -> Unit,
    onSelectFiles: () -> Unit,
    onChooseOutputTree: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = { onAction(ConvertyAction.DismissSetup) },
        sheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f),
        ) {
            SetupHeader(
                direction = state.direction,
                onClose = { onAction(ConvertyAction.DismissSetup) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    SetupSectionCard(
                        titleRes = R.string.selected_files,
                        icon = Icons.Rounded.Add,
                    ) {
                        Button(
                            onClick = onSelectFiles,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 58.dp),
                            shape = MaterialTheme.shapes.largeIncreased,
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null)
                            Text(
                                text = stringResource(R.string.select_files),
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                        if (state.documents.isNotEmpty()) {
                            Text(
                                text = pluralStringResource(
                                    R.plurals.selected_file_count,
                                    state.documents.size,
                                    state.documents.size,
                                ),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                        state.documents.forEach { document ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                ),
                                shape = MaterialTheme.shapes.large,
                            ) {
                                Row(
                                    modifier = Modifier.padding(
                                        start = 14.dp,
                                        top = 8.dp,
                                        bottom = 8.dp,
                                    ),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Rounded.InsertDriveFile,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                    Text(
                                        text = document.displayName,
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 10.dp),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    IconButton(
                                        onClick = {
                                            onAction(ConvertyAction.RemoveDocument(document.uri))
                                        },
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = stringResource(R.string.remove_file),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (state.direction.supportsItemSelection) item {
                    SetupSectionCard(
                        titleRes = R.string.selection_title,
                        icon = Icons.Rounded.FilterAlt,
                    ) {
                        ChoiceField(
                            label = "",
                            selected = state.selectionMode,
                            options = SelectionMode.entries,
                            optionLabel = { mode ->
                                stringResource(mode.titleRes(state.direction))
                            },
                            onSelected = {
                                onAction(ConvertyAction.SelectionModeChanged(it))
                            },
                        )
                        when (state.selectionMode) {
                            SelectionMode.FIRST, SelectionMode.LAST -> OutlinedTextField(
                                value = state.count,
                                onValueChange = {
                                    onAction(ConvertyAction.SelectionCountChanged(it))
                                },
                                label = { Text(stringResource(R.string.single_item)) },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                ),
                                singleLine = true,
                                shape = MaterialTheme.shapes.large,
                            )
                            SelectionMode.KEEP, SelectionMode.REMOVE -> OutlinedTextField(
                                value = state.expression,
                                onValueChange = {
                                    onAction(ConvertyAction.SelectionExpressionChanged(it))
                                },
                                label = { Text(stringResource(R.string.custom_selection)) },
                                placeholder = { Text(stringResource(R.string.selection_example)) },
                                supportingText = { Text(stringResource(R.string.selection_hint)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = MaterialTheme.shapes.large,
                            )
                            SelectionMode.ALL -> Unit
                        }
                    }
                }

                if (
                    state.direction.targetFormat == FileFormat.JPG ||
                    state.direction.targetFormat == FileFormat.WEBP ||
                    state.direction.targetFormat == FileFormat.THUMBNAIL ||
                    state.direction == ConversionDirection.PDF_TO_PPTX
                ) item {
                    SetupSectionCard(
                        titleRes = R.string.quality,
                        icon = Icons.Rounded.HighQuality,
                    ) {
                        ChoiceField(
                            label = "",
                            selected = state.quality,
                            options = ConversionQuality.entries,
                            optionLabel = { stringResource(it.titleRes()) },
                            onSelected = { onAction(ConvertyAction.QualityChanged(it)) },
                        )
                    }
                }

                if (state.direction.supportsDpi) item {
                    SetupSectionCard(
                        titleRes = R.string.dpi,
                        icon = Icons.Rounded.HighQuality,
                    ) {
                        ChoiceField(
                            label = "",
                            selected = state.dpi,
                            options = listOf(72, 96, 150, 300, 600),
                            optionLabel = { stringResource(R.string.dpi_value, it) },
                            onSelected = { onAction(ConvertyAction.DpiChanged(it)) },
                        )
                    }
                }

                if (state.direction.supportsLosslessChoice) item {
                    SetupSectionCard(
                        titleRes = R.string.lossless,
                        icon = Icons.Rounded.HighQuality,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.lossless),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    text = stringResource(R.string.lossless_summary),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                            Switch(
                                checked = state.lossless,
                                onCheckedChange = {
                                    onAction(ConvertyAction.LosslessChanged(it))
                                },
                            )
                        }
                    }
                }

                if (state.direction == ConversionDirection.PDF_TO_PPTX) item {
                    SetupSectionCard(
                        titleRes = R.string.content_fit,
                        icon = Icons.Rounded.FitScreen,
                    ) {
                        ChoiceField(
                            label = "",
                            selected = state.fit,
                            options = ContentFit.entries,
                            optionLabel = { stringResource(it.titleRes()) },
                            onSelected = { onAction(ConvertyAction.FitChanged(it)) },
                        )
                    }
                }

                item {
                    SetupSectionCard(
                        titleRes = R.string.output_location,
                        icon = Icons.Rounded.Folder,
                    ) {
                        OutlinedCard(
                            onClick = onChooseOutputTree,
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Rounded.Folder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.choose_output_folder),
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    state.outputTreeUri?.let { uri ->
                                        Text(
                                            text = Uri.parse(uri).lastPathSegment ?: uri,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.bodyMedium,
                                        )
                                    } ?: Text(
                                        text = stringResource(R.string.error_output_unavailable),
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(4.dp)) }
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
            ) {
                Column(
                    modifier = Modifier.padding(
                        start = 18.dp,
                        top = 12.dp,
                        end = 18.dp,
                        bottom = 20.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(
                            if (state.canSubmit || state.isSubmitting) {
                                R.string.ready_to_convert
                            } else {
                                R.string.complete_setup
                            },
                        ),
                        color = if (state.canSubmit || state.isSubmitting) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Button(
                        onClick = { onAction(ConvertyAction.SubmitConversion) },
                        enabled = state.canSubmit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                        shape = MaterialTheme.shapes.large,
                    ) {
                        if (state.isSubmitting) {
                            ExpressiveCircularProgress(
                                progress = null,
                                modifier = Modifier.size(24.dp),
                            )
                        } else {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                            Text(
                                text = stringResource(R.string.convert),
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SetupHeader(
    direction: ConversionDirection,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, top = 6.dp, end = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.setup_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = stringResource(
                    R.string.conversion_pair,
                    stringResource(direction.sourceFormat.labelRes()),
                    stringResource(direction.targetFormat.labelRes()),
                ),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium,
            )
        }
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = stringResource(R.string.close),
            )
        }
    }
}

@Composable
private fun SetupSectionCard(
    @StringRes titleRes: Int,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape,
                ) {
                    Box(
                        modifier = Modifier.size(38.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
                Text(
                    text = stringResource(titleRes),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            content()
        }
    }
}

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onAction: (ConvertyAction) -> Unit,
    onChooseOutputTree: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { SettingsHero() }

        item {
            SettingsSectionCard(
                titleRes = R.string.appearance,
                icon = Icons.Rounded.Palette,
            ) {
                ChoiceField(
                    label = stringResource(R.string.theme),
                    selected = settings.themeMode,
                    options = ThemeMode.entries,
                    optionLabel = { stringResource(it.titleRes()) },
                    onSelected = { onAction(ConvertyAction.ThemeChanged(it)) },
                )
                ChoiceField(
                    label = stringResource(R.string.theme_color),
                    selected = settings.themePalette,
                    options = ThemePalette.entries,
                    optionLabel = { stringResource(it.titleRes()) },
                    onSelected = { onAction(ConvertyAction.ThemePaletteChanged(it)) },
                    optionVisual = { PaletteSwatch(it.previewColors()) },
                )
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = MaterialTheme.shapes.large,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.dynamic_color),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = stringResource(R.string.dynamic_color_summary),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        Switch(
                            checked = settings.useDynamicColor,
                            onCheckedChange = {
                                onAction(ConvertyAction.DynamicColorChanged(it))
                            },
                        )
                    }
                }
            }
        }

        item {
            SettingsSectionCard(
                titleRes = R.string.language,
                icon = Icons.Rounded.Translate,
            ) {
                val selectedTag = (settings.language as? LanguagePreference.Specific)
                    ?.language?.languageTag
                val knownTags = SupportedAppLocales.All.map { it.languageTag }
                val options = buildList<String?> {
                    add(null)
                    addAll(knownTags)
                    if (selectedTag != null && selectedTag !in knownTags) add(selectedTag)
                }
                ChoiceField(
                    label = "",
                    selected = selectedTag,
                    options = options,
                    optionLabel = { tag ->
                        if (tag == null) {
                            stringResource(R.string.system_default)
                        } else {
                            SupportedAppLocales.All.firstOrNull { it.languageTag == tag }
                                ?.let { stringResource(it.displayNameRes) }
                                ?: tag
                        }
                    },
                    onSelected = { onAction(ConvertyAction.LanguageChanged(it)) },
                )
            }
        }

        item {
            SettingsSectionCard(
                titleRes = R.string.conversion_defaults,
                icon = Icons.Rounded.Tune,
            ) {
                ChoiceField(
                    label = stringResource(R.string.default_quality),
                    selected = settings.defaultQuality,
                    options = ConversionQuality.entries,
                    optionLabel = { stringResource(it.titleRes()) },
                    onSelected = {
                        onAction(ConvertyAction.DefaultQualityChanged(it))
                    },
                )
                OutlinedCard(
                    onClick = onChooseOutputTree,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Rounded.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.default_output_location),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = settings.defaultOutputTreeUri?.let { uri ->
                                    Uri.parse(uri).lastPathSegment ?: uri
                                } ?: stringResource(R.string.choose_output_folder),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondary,
                        shape = CircleShape,
                    ) {
                        Box(
                            modifier = Modifier.size(42.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondary,
                            )
                        }
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.privacy),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = stringResource(R.string.privacy_summary),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun SettingsHero() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape,
            ) {
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
            Column {
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = stringResource(R.string.settings_subtitle),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    @StringRes titleRes: Int,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = CircleShape,
                ) {
                    Box(
                        modifier = Modifier.size(40.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(21.dp),
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }
                }
                Text(
                    text = stringResource(titleRes),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            content()
        }
    }
}

@Composable
private fun <T> ChoiceField(
    label: String,
    selected: T,
    options: List<T>,
    optionLabel: @Composable (T) -> String,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    optionVisual: (@Composable (T) -> Unit)? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedCard(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                optionVisual?.invoke(selected)
                Column(modifier = Modifier.weight(1f)) {
                    if (label.isNotBlank()) {
                        Text(
                            text = label,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                    Text(
                        text = optionLabel(selected),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Icon(
                    Icons.Rounded.ExpandMore,
                    contentDescription = stringResource(R.string.more_options),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(min = 280.dp, max = 420.dp),
            shape = MaterialTheme.shapes.large,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            optionVisual?.invoke(option)
                            Text(optionLabel(option))
                        }
                    },
                    onClick = {
                        expanded = false
                        onSelected(option)
                    },
                    leadingIcon = {
                        RadioButton(
                            selected = selected == option,
                            onClick = null,
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun PaletteSwatch(colors: List<Color>) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        colors.forEach { color ->
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(color = color, shape = CircleShape),
            )
        }
    }
}

@StringRes
private fun SelectionMode.titleRes(direction: ConversionDirection): Int = when (this) {
    SelectionMode.ALL -> if (direction.sourceFormat == FileFormat.PDF) {
        R.string.all_pages
    } else {
        R.string.all_slides
    }
    SelectionMode.FIRST -> R.string.from_start
    SelectionMode.LAST -> R.string.from_end
    SelectionMode.KEEP -> R.string.custom_selection
    SelectionMode.REMOVE -> R.string.exclude_selection
}

@StringRes
private fun ConversionQuality.titleRes(): Int = when (this) {
    ConversionQuality.COMPACT -> R.string.quality_fast
    ConversionQuality.BALANCED -> R.string.quality_balanced
    ConversionQuality.HIGH -> R.string.quality_high
    ConversionQuality.MAXIMUM -> R.string.quality_maximum
}

@StringRes
private fun ContentFit.titleRes(): Int = when (this) {
    ContentFit.CONTAIN -> R.string.fit_contain
    ContentFit.COVER -> R.string.fit_cover
    ContentFit.STRETCH -> R.string.fit_stretch
}

@StringRes
private fun ThemeMode.titleRes(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.theme_system
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.DARK -> R.string.theme_dark
}

@StringRes
private fun ThemePalette.titleRes(): Int = when (this) {
    ThemePalette.OCEAN -> R.string.palette_ocean
    ThemePalette.VIOLET -> R.string.palette_violet
    ThemePalette.FOREST -> R.string.palette_forest
    ThemePalette.SUNSET -> R.string.palette_sunset
}
