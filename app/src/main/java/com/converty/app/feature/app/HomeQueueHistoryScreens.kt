@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.converty.app.feature.app

import android.text.format.Formatter
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.converty.app.R
import com.converty.app.core.model.ConversionErrorCode
import com.converty.app.core.model.ConversionItem
import com.converty.app.core.model.ConversionJob
import com.converty.app.core.model.ConversionOutput
import com.converty.app.core.model.ConversionStatus
import com.converty.app.ui.theme.ExpressiveLinearProgress
import java.text.DateFormat
import java.util.Date

@Composable
fun QueueScreen(
    jobs: List<ConversionJob>,
    onCancel: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.queue_title),
                style = MaterialTheme.typography.headlineLarge,
            )
        }
        if (jobs.isEmpty()) {
            item { EmptyCard(R.string.queue_empty) }
        } else {
            items(jobs, key = { it.id }) { job ->
                QueueJobCard(job = job, onCancel = onCancel)
            }
        }
    }
}

@Composable
private fun QueueJobCard(job: ConversionJob, onCancel: (String) -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            JobHeader(job)
            if (job.status == ConversionStatus.PREPARING || job.status == ConversionStatus.RUNNING) {
                val progress = if (job.status == ConversionStatus.RUNNING && job.items.isNotEmpty()) {
                    job.items.map { it.progressPercent }.average().div(100.0).toFloat()
                } else {
                    null
                }
                ExpressiveLinearProgress(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            job.items.sortedBy { it.position }.forEach { item ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = item.input.displayName,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = stringResource(R.string.progress_percent, item.progressPercent),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                    if (item.status == ConversionStatus.RUNNING) {
                        ExpressiveLinearProgress(
                            progress = item.progressPercent / 100f,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
            OutlinedButton(
                onClick = { onCancel(job.id) },
                modifier = Modifier.align(Alignment.End),
            ) {
                Icon(Icons.Rounded.Cancel, contentDescription = null)
                Text(
                    text = stringResource(R.string.cancel_job),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

private enum class HistoryFilter { ALL, SUCCESSFUL, FAILED, CANCELLED }

@Composable
fun HistoryScreen(
    jobs: List<ConversionJob>,
    onOpen: (ConversionOutput) -> Unit,
    onShare: (ConversionOutput) -> Unit,
    onRetry: (String) -> Unit,
    onDelete: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(HistoryFilter.ALL) }
    var confirmClear by remember { mutableStateOf(false) }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.clear_history)) },
            text = { Text(stringResource(R.string.clear_history_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmClear = false
                        onClear()
                    },
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
    val filteredJobs = remember(jobs, query, filter) {
        jobs.filter { job ->
            val matchesQuery = job.matchesHistoryQuery(query)
            val matchesFilter = when (filter) {
                HistoryFilter.ALL -> true
                HistoryFilter.SUCCESSFUL -> job.status == ConversionStatus.SUCCEEDED ||
                    job.status == ConversionStatus.SUCCEEDED_WITH_WARNINGS
                HistoryFilter.FAILED -> job.status == ConversionStatus.FAILED
                HistoryFilter.CANCELLED -> job.status == ConversionStatus.CANCELLED
            }
            matchesQuery && matchesFilter
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.history_title),
                    style = MaterialTheme.typography.headlineLarge,
                )
                if (jobs.isNotEmpty()) {
                    TextButton(onClick = { confirmClear = true }) {
                        Text(stringResource(R.string.clear_history))
                    }
                }
            }
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text(stringResource(R.string.search_history)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }
        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                HistoryFilter.entries.forEach { option ->
                    FilterChip(
                        selected = filter == option,
                        onClick = { filter = option },
                        label = {
                            Text(
                                stringResource(
                                    when (option) {
                                        HistoryFilter.ALL -> R.string.filter_all
                                        HistoryFilter.SUCCESSFUL -> R.string.filter_successful
                                        HistoryFilter.FAILED -> R.string.filter_failed
                                        HistoryFilter.CANCELLED -> R.string.filter_cancelled
                                    },
                                ),
                            )
                        },
                    )
                }
            }
        }
        if (filteredJobs.isEmpty()) {
            item { EmptyCard(R.string.history_empty) }
        } else {
            items(filteredJobs, key = { it.id }) { job ->
                HistoryJobCard(
                    job = job,
                    onOpen = onOpen,
                    onShare = onShare,
                    onRetry = onRetry,
                    onDelete = onDelete,
                )
            }
        }
    }
}

@Composable
private fun HistoryJobCard(
    job: ConversionJob,
    onOpen: (ConversionOutput) -> Unit,
    onShare: (ConversionOutput) -> Unit,
    onRetry: ((String) -> Unit)?,
    onDelete: ((String) -> Unit)?,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            JobHeader(job)
            Text(
                text = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                    .format(Date(job.createdAtEpochMillis)),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            job.items.sortedBy { it.position }.forEach { item ->
                item.outputsForHistory().forEachIndexed { index, output ->
                    HistoryOutputRow(
                        item = item,
                        output = output,
                        showItemMessages = index == 0,
                        onOpen = onOpen,
                        onShare = onShare,
                    )
                }
            }
            if (onRetry != null || onDelete != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    onRetry?.takeIf {
                        job.status == ConversionStatus.FAILED ||
                            job.status == ConversionStatus.CANCELLED
                    }?.let {
                        TextButton(onClick = { it(job.id) }) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null)
                            Text(
                                text = stringResource(R.string.retry),
                                modifier = Modifier.padding(start = 6.dp),
                            )
                        }
                    }
                    onDelete?.let {
                        TextButton(onClick = { it(job.id) }) {
                            Icon(Icons.Rounded.Delete, contentDescription = null)
                            Text(
                                text = stringResource(R.string.delete),
                                modifier = Modifier.padding(start = 6.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

internal fun ConversionJob.matchesHistoryQuery(query: String): Boolean =
    query.isBlank() || items.any { item ->
        item.input.displayName.contains(query, ignoreCase = true) ||
            item.outputs.any { output -> output.displayName.contains(query, ignoreCase = true) }
    }

internal fun ConversionItem.outputsForHistory(): List<ConversionOutput?> =
    outputs.ifEmpty { listOf(null) }

@Composable
private fun HistoryOutputRow(
    item: ConversionItem,
    output: ConversionOutput?,
    showItemMessages: Boolean,
    onOpen: (ConversionOutput) -> Unit,
    onShare: (ConversionOutput) -> Unit,
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.FolderOpen, contentDescription = null)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp),
        ) {
            Text(
                text = output?.displayName ?: item.input.displayName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
            )
            (output?.sizeBytes ?: item.input.sizeBytes)?.let { size ->
                Text(
                    text = Formatter.formatShortFileSize(context, size),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (showItemMessages) {
                item.error?.let { error ->
                    Text(
                        text = stringResource(error.code.messageRes()),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (item.warningCodes.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.conversion_warning),
                        color = MaterialTheme.colorScheme.tertiary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        if (output?.isReadable == true) {
            IconButton(onClick = { onOpen(output) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                    contentDescription = stringResource(R.string.open),
                )
            }
            IconButton(onClick = { onShare(output) }) {
                Icon(
                    imageVector = Icons.Rounded.Share,
                    contentDescription = stringResource(R.string.share),
                )
            }
        }
    }
}

@Composable
private fun JobHeader(job: ConversionJob) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = job.direction.targetFormat.icon(),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(
                    R.string.conversion_pair,
                    stringResource(job.direction.sourceFormat.labelRes()),
                    stringResource(job.direction.targetFormat.labelRes()),
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = pluralStringResource(
                    R.plurals.selected_file_count,
                    job.items.size,
                    job.items.size,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Text(
            text = stringResource(job.status.titleRes()),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun EmptyCard(@StringRes messageRes: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Text(
            text = stringResource(messageRes),
            modifier = Modifier.padding(24.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@StringRes
private fun ConversionStatus.titleRes(): Int = when (this) {
    ConversionStatus.QUEUED -> R.string.status_waiting
    ConversionStatus.PREPARING -> R.string.status_preparing
    ConversionStatus.RUNNING -> R.string.status_converting
    ConversionStatus.SUCCEEDED, ConversionStatus.SUCCEEDED_WITH_WARNINGS -> R.string.status_succeeded
    ConversionStatus.FAILED -> R.string.status_failed
    ConversionStatus.CANCELLED -> R.string.status_cancelled
}

@StringRes
private fun ConversionErrorCode.messageRes(): Int = when (this) {
    ConversionErrorCode.INPUT_PERMISSION_LOST,
    ConversionErrorCode.INPUT_NOT_FOUND,
    -> R.string.error_file_unavailable
    ConversionErrorCode.OUTPUT_PERMISSION_LOST,
    ConversionErrorCode.OUTPUT_CREATE_FAILED,
    ConversionErrorCode.OUTPUT_WRITE_FAILED,
    -> R.string.error_output_unavailable
    ConversionErrorCode.INVALID_SELECTION -> R.string.error_invalid_selection
    ConversionErrorCode.UNSUPPORTED_INPUT,
    ConversionErrorCode.INVALID_DOCUMENT,
    ConversionErrorCode.PASSWORD_PROTECTED,
    -> R.string.error_invalid_format
    else -> R.string.error_generic
}
