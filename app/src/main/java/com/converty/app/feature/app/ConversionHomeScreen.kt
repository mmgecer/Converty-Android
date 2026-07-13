package com.converty.app.feature.app

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Slideshow
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.converty.app.R
import com.converty.app.core.model.ConversionDirection
import com.converty.app.core.model.FileFormat
import com.converty.app.core.model.FormatCategory

@Composable
fun HomeScreen(
    direction: ConversionDirection,
    onSourceChanged: (FileFormat) -> Unit,
    onTargetChanged: (FileFormat) -> Unit,
    onSwap: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.colorScheme.primary,
                    shape = MaterialTheme.shapes.largeIncreased,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.SwapHoriz,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraExtraLarge,
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FormatSelector(
                            labelRes = R.string.source_format,
                            selected = direction.sourceFormat,
                            options = ConversionDirection.sourceFormats,
                            onSelected = onSourceChanged,
                            modifier = Modifier.weight(1f),
                        )
                        FilledIconButton(
                            onClick = onSwap,
                            enabled = direction.reversedOrNull() != null,
                            modifier = Modifier.size(52.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SwapHoriz,
                                contentDescription = stringResource(R.string.swap_formats),
                            )
                        }
                        FormatSelector(
                            labelRes = R.string.target_format,
                            selected = direction.targetFormat,
                            options = ConversionDirection.targetsFor(direction.sourceFormat),
                            onSelected = onTargetChanged,
                            modifier = Modifier.weight(1f),
                        )
                    }

                    if (direction.preservesAppearanceAsImages) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            shape = MaterialTheme.shapes.large,
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Icon(Icons.Rounded.Visibility, contentDescription = null)
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = stringResource(R.string.preserve_appearance),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        text = stringResource(R.string.preserve_appearance_summary),
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = onContinue,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 58.dp),
                        shape = MaterialTheme.shapes.largeIncreased,
                    ) {
                        Text(stringResource(R.string.continue_to_files))
                    }
                }
            }
        }
    }
}

@Composable
private fun FormatSelector(
    @StringRes labelRes: Int,
    selected: FileFormat,
    options: List<FileFormat>,
    onSelected: (FileFormat) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedCard(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    text = stringResource(labelRes),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = selected.icon(),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(selected.labelRes()),
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Icon(Icons.Rounded.ExpandMore, contentDescription = null)
                }
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = MaterialTheme.shapes.large,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            options.forEach { format ->
                DropdownMenuItem(
                    text = { Text(stringResource(format.labelRes())) },
                    leadingIcon = { Icon(format.icon(), contentDescription = null) },
                    trailingIcon = {
                        RadioButton(
                            selected = format == selected,
                            onClick = null,
                        )
                    },
                    onClick = {
                        expanded = false
                        onSelected(format)
                    },
                )
            }
        }
    }
}

@StringRes
internal fun FileFormat.labelRes(): Int = when (this) {
    FileFormat.PDF -> R.string.format_pdf
    FileFormat.PPTX -> R.string.format_pptx
    FileFormat.PNG -> R.string.format_png
    FileFormat.JPG -> R.string.format_jpg
    FileFormat.WEBP -> R.string.format_webp
    FileFormat.TIFF -> R.string.format_tiff
    FileFormat.BMP -> R.string.format_bmp
    FileFormat.HEIC_HEIF -> R.string.format_heic_heif
    FileFormat.SVG -> R.string.format_svg
    FileFormat.AVIF -> R.string.format_avif
    FileFormat.DOCX -> R.string.format_docx
    FileFormat.XLSX -> R.string.format_xlsx
    FileFormat.ODT -> R.string.format_odt
    FileFormat.ODS -> R.string.format_ods
    FileFormat.ODP -> R.string.format_odp
    FileFormat.THUMBNAIL -> R.string.format_thumbnail
}

internal fun FileFormat.icon(): ImageVector = when (category) {
    FormatCategory.IMAGE, FormatCategory.SPECIAL -> Icons.Rounded.Image
    FormatCategory.PRESENTATION -> Icons.Rounded.Slideshow
    FormatCategory.SPREADSHEET -> Icons.Rounded.TableChart
    FormatCategory.DOCUMENT -> if (this == FileFormat.PDF) {
        Icons.Rounded.PictureAsPdf
    } else {
        Icons.Rounded.Description
    }
}
