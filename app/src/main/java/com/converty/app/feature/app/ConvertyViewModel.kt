package com.converty.app.feature.app

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.converty.app.core.model.ContentFit
import com.converty.app.core.model.ConversionDirection
import com.converty.app.core.model.ConversionDocument
import com.converty.app.core.model.ConversionItem
import com.converty.app.core.model.ConversionJob
import com.converty.app.core.model.ConversionOptions
import com.converty.app.core.model.ConversionQuality
import com.converty.app.core.model.ConversionStatus
import com.converty.app.core.model.FileFormat
import com.converty.app.core.model.OutputOptions
import com.converty.app.core.model.isTerminal
import com.converty.app.core.model.selection.SelectionMode
import com.converty.app.core.model.selection.SelectionRequest
import com.converty.app.core.repository.ConversionHistoryRepository
import com.converty.app.core.repository.SettingsRepository
import com.converty.app.core.settings.AppLanguage
import com.converty.app.core.settings.AppSettings
import com.converty.app.core.settings.LanguagePreference
import com.converty.app.core.settings.ThemeMode
import com.converty.app.core.settings.ThemePalette
import com.converty.app.data.files.SafDocumentGateway
import com.converty.app.work.ConversionScheduler
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppDestination { HOME, QUEUE, HISTORY, SETTINGS }

enum class UiNotice { FILE_UNAVAILABLE, OUTPUT_UNAVAILABLE, GENERIC_ERROR }

data class ConversionSetupState(
    val direction: ConversionDirection,
    val documents: List<ConversionDocument> = emptyList(),
    val selectionMode: SelectionMode = SelectionMode.ALL,
    val count: String = "1",
    val expression: String = "",
    val quality: ConversionQuality = ConversionQuality.MAXIMUM,
    val fit: ContentFit = ContentFit.CONTAIN,
    val dpi: Int = 300,
    val lossless: Boolean = true,
    val outputTreeUri: String? = null,
    val isSubmitting: Boolean = false,
) {
    val canSubmit: Boolean
        get() = documents.isNotEmpty() && (
            !direction.supportsItemSelection || when (selectionMode) {
                SelectionMode.ALL -> true
                SelectionMode.FIRST, SelectionMode.LAST -> count.toIntOrNull()?.let { it > 0 } == true
                SelectionMode.KEEP, SelectionMode.REMOVE -> expression.isNotBlank()
            }
        ) && outputTreeUri != null && !isSubmitting
}

data class ConvertyUiState(
    val destination: AppDestination = AppDestination.HOME,
    val settings: AppSettings = AppSettings(),
    val settingsReady: Boolean = false,
    val homeDirection: ConversionDirection = ConversionDirection.PDF_TO_PPTX,
    val jobs: List<ConversionJob> = emptyList(),
    val setup: ConversionSetupState? = null,
    val notice: UiNotice? = null,
) {
    val queue: List<ConversionJob> get() = jobs.filterNot { it.status.isTerminal }
    val history: List<ConversionJob> get() = jobs.filter { it.status.isTerminal }
}

sealed interface ConvertyAction {
    data class Navigate(val destination: AppDestination) : ConvertyAction
    data class HomeSourceChanged(val format: FileFormat) : ConvertyAction
    data class HomeTargetChanged(val format: FileFormat) : ConvertyAction
    data object SwapHomeFormats : ConvertyAction
    data object StartSelectedSetup : ConvertyAction
    data class StartSetup(val direction: ConversionDirection) : ConvertyAction
    data object DismissSetup : ConvertyAction
    data class FilesPicked(val uris: List<Uri>) : ConvertyAction
    data class RemoveDocument(val uri: String) : ConvertyAction
    data class SelectionModeChanged(val mode: SelectionMode) : ConvertyAction
    data class SelectionCountChanged(val value: String) : ConvertyAction
    data class SelectionExpressionChanged(val value: String) : ConvertyAction
    data class QualityChanged(val quality: ConversionQuality) : ConvertyAction
    data class FitChanged(val fit: ContentFit) : ConvertyAction
    data class DpiChanged(val dpi: Int) : ConvertyAction
    data class LosslessChanged(val enabled: Boolean) : ConvertyAction
    data class OutputTreePicked(val uri: Uri) : ConvertyAction
    data object SubmitConversion : ConvertyAction
    data class CancelJob(val jobId: String) : ConvertyAction
    data class RetryJob(val jobId: String) : ConvertyAction
    data class DeleteJob(val jobId: String) : ConvertyAction
    data object ClearHistory : ConvertyAction
    data class ThemeChanged(val mode: ThemeMode) : ConvertyAction
    data class ThemePaletteChanged(val palette: ThemePalette) : ConvertyAction
    data class DynamicColorChanged(val enabled: Boolean) : ConvertyAction
    data class LanguageChanged(val tag: String?) : ConvertyAction
    data class DefaultQualityChanged(val quality: ConversionQuality) : ConvertyAction
    data class DefaultOutputTreePicked(val uri: Uri) : ConvertyAction
    data object NoticeShown : ConvertyAction
}

class ConvertyViewModel(
    private val historyRepository: ConversionHistoryRepository,
    private val settingsRepository: SettingsRepository,
    private val documentGateway: SafDocumentGateway,
    private val scheduler: ConversionScheduler,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ConvertyUiState())
    val uiState: StateFlow<ConvertyUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val safeSettings = settingsRepository.settings.catch { error ->
                Log.e(LOG_TAG, "Settings could not be loaded; using safe defaults", error)
                _uiState.update { it.copy(notice = UiNotice.GENERIC_ERROR) }
                emit(AppSettings())
            }
            val safeJobs = historyRepository.observeJobs().catch { error ->
                Log.e(LOG_TAG, "History could not be loaded; opening without history", error)
                _uiState.update { it.copy(notice = UiNotice.GENERIC_ERROR) }
                emit(emptyList())
            }
            combine(safeSettings, safeJobs) { settings, jobs ->
                settings to jobs
            }.collect { (settings, jobs) ->
                _uiState.update {
                    it.copy(settings = settings, settingsReady = true, jobs = jobs)
                }
            }
        }
    }

    private companion object {
        const val LOG_TAG = "ConvertyStartup"
    }

    fun onAction(action: ConvertyAction) {
        when (action) {
            is ConvertyAction.Navigate -> _uiState.update { it.copy(destination = action.destination) }
            is ConvertyAction.HomeSourceChanged -> changeHomeSource(action.format)
            is ConvertyAction.HomeTargetChanged -> changeHomeTarget(action.format)
            ConvertyAction.SwapHomeFormats -> swapHomeFormats()
            ConvertyAction.StartSelectedSetup -> startSetup(_uiState.value.homeDirection)
            is ConvertyAction.StartSetup -> startSetup(action.direction)
            ConvertyAction.DismissSetup -> dismissSetup()
            is ConvertyAction.FilesPicked -> addDocuments(action.uris)
            is ConvertyAction.RemoveDocument -> removeDocument(action.uri)
            is ConvertyAction.SelectionModeChanged -> updateSetup { copy(selectionMode = action.mode) }
            is ConvertyAction.SelectionCountChanged -> updateSetup {
                copy(count = action.value.filter(Char::isDigit).take(6))
            }
            is ConvertyAction.SelectionExpressionChanged -> updateSetup {
                copy(expression = action.value.take(256))
            }
            is ConvertyAction.QualityChanged -> updateSetup { copy(quality = action.quality) }
            is ConvertyAction.FitChanged -> updateSetup { copy(fit = action.fit) }
            is ConvertyAction.DpiChanged -> updateSetup { copy(dpi = action.dpi.coerceIn(72, 600)) }
            is ConvertyAction.LosslessChanged -> updateSetup { copy(lossless = action.enabled) }
            is ConvertyAction.OutputTreePicked -> rememberOutputTree(action.uri, forDefaults = false)
            ConvertyAction.SubmitConversion -> submitConversion()
            is ConvertyAction.CancelJob -> cancel(action.jobId)
            is ConvertyAction.RetryJob -> retry(action.jobId)
            is ConvertyAction.DeleteJob -> deleteJob(action.jobId)
            ConvertyAction.ClearHistory -> clearHistory()
            is ConvertyAction.ThemeChanged -> updateSettings { it.copy(themeMode = action.mode) }
            is ConvertyAction.ThemePaletteChanged -> updateSettings {
                it.copy(themePalette = action.palette, useDynamicColor = false)
            }
            is ConvertyAction.DynamicColorChanged -> updateSettings {
                it.copy(useDynamicColor = action.enabled)
            }
            is ConvertyAction.LanguageChanged -> updateSettings {
                it.copy(
                    language = action.tag?.let { tag -> LanguagePreference.Specific(AppLanguage(tag)) }
                        ?: LanguagePreference.System,
                )
            }
            is ConvertyAction.DefaultQualityChanged -> updateSettings {
                it.copy(defaultQuality = action.quality)
            }
            is ConvertyAction.DefaultOutputTreePicked -> rememberOutputTree(action.uri, forDefaults = true)
            ConvertyAction.NoticeShown -> _uiState.update { it.copy(notice = null) }
        }
    }

    private fun changeHomeSource(source: FileFormat) {
        val current = _uiState.value.homeDirection
        val next = ConversionDirection.from(source, current.targetFormat)
            ?: ConversionDirection.entries.firstOrNull { it.sourceFormat == source }
            ?: return
        _uiState.update { it.copy(homeDirection = next) }
    }

    private fun changeHomeTarget(target: FileFormat) {
        val current = _uiState.value.homeDirection
        val next = ConversionDirection.from(current.sourceFormat, target) ?: return
        _uiState.update { it.copy(homeDirection = next) }
    }

    private fun swapHomeFormats() {
        val reversed = _uiState.value.homeDirection.reversedOrNull() ?: return
        _uiState.update { it.copy(homeDirection = reversed) }
    }

    private fun startSetup(direction: ConversionDirection) {
        val settings = _uiState.value.settings
        _uiState.update {
            it.copy(
                setup = ConversionSetupState(
                    direction = direction,
                    selectionMode = if (direction.supportsItemSelection) {
                        settings.defaultSelection.mode
                    } else {
                        SelectionMode.ALL
                    },
                    count = settings.defaultSelection.count?.toString() ?: "1",
                    expression = settings.defaultSelection.expression.orEmpty(),
                    quality = settings.defaultQuality,
                    fit = settings.defaultFit,
                    dpi = 300,
                    lossless = true,
                    outputTreeUri = settings.defaultOutputTreeUri,
                ),
            )
        }
    }

    private fun addDocuments(uris: List<Uri>) {
        if (uris.isEmpty() || _uiState.value.setup == null) return
        viewModelScope.launch {
            val setup = _uiState.value.setup ?: return@launch
            val accepted = mutableListOf<ConversionDocument>()
            var failed = false
            uris.distinct().forEach { uri ->
                runCatching {
                    documentGateway.metadata(uri).toCoreModel()
                }.onSuccess { document ->
                    if (setup.direction.sourceFormat.accepts(document.displayName, document.mimeType)) {
                        runCatching {
                            if (_uiState.value.settings.persistInputPermissions) {
                                documentGateway.takePersistableReadPermission(uri)
                            }
                        }.onSuccess {
                            accepted += document.copy(
                                hasPersistedReadPermission = _uiState.value.settings.persistInputPermissions,
                            )
                        }.onFailure { failed = true }
                    } else {
                        failed = true
                    }
                }.onFailure { failed = true }
            }
            updateSetup {
                val known = documents.mapTo(mutableSetOf()) { it.uri }
                copy(documents = documents + accepted.filter { known.add(it.uri) })
            }
            if (failed) showNotice(UiNotice.FILE_UNAVAILABLE)
        }
    }

    private fun rememberOutputTree(uri: Uri, forDefaults: Boolean) {
        viewModelScope.launch {
            runCatching { documentGateway.takePersistableTreePermission(uri) }
                .onSuccess {
                    if (forDefaults) {
                        settingsRepository.update { it.copy(defaultOutputTreeUri = uri.toString()) }
                    } else {
                        updateSetup { copy(outputTreeUri = uri.toString()) }
                    }
                }
                .onFailure { showNotice(UiNotice.OUTPUT_UNAVAILABLE) }
        }
    }

    private fun submitConversion() {
        val setup = _uiState.value.setup?.takeIf(ConversionSetupState::canSubmit) ?: return
        updateSetup { copy(isSubmitting = true) }
        viewModelScope.launch {
            runCatching {
                val jobId = UUID.randomUUID().toString()
                val options = ConversionOptions(
                    selection = setup.toSelectionRequest(),
                    quality = setup.quality,
                    fit = setup.fit,
                    dpi = setup.dpi,
                    lossless = setup.lossless,
                    output = OutputOptions(destinationTreeUri = setup.outputTreeUri),
                )
                val job = ConversionJob(
                    id = jobId,
                    direction = setup.direction,
                    createdAtEpochMillis = System.currentTimeMillis(),
                    options = options,
                    items = setup.documents.mapIndexed { index, document ->
                        ConversionItem(
                            id = UUID.randomUUID().toString(),
                            jobId = jobId,
                            position = index,
                            input = document,
                        )
                    },
                )
                historyRepository.saveSnapshot(job)
                scheduler.enqueue(jobId)
            }.onSuccess {
                _uiState.update {
                    it.copy(destination = AppDestination.QUEUE, setup = null)
                }
            }.onFailure {
                updateSetup { copy(isSubmitting = false) }
                showNotice(UiNotice.GENERIC_ERROR)
            }
        }
    }

    private fun retry(jobId: String) {
        viewModelScope.launch {
            runCatching {
                val job = historyRepository.getJob(jobId) ?: error("Missing job")
                historyRepository.updateJobStatus(job.id, ConversionStatus.QUEUED)
                scheduler.retry(job.id)
            }.onSuccess {
                _uiState.update { it.copy(destination = AppDestination.QUEUE) }
            }.onFailure { showNotice(UiNotice.GENERIC_ERROR) }
        }
    }

    private fun removeDocument(uri: String) {
        updateSetup { copy(documents = documents.filterNot { it.uri == uri }) }
        releaseInputPermissionIfUnused(uri, _uiState.value.jobs)
    }

    private fun dismissSetup() {
        val documents = _uiState.value.setup?.documents.orEmpty()
        _uiState.update { it.copy(setup = null) }
        documents.forEach { document ->
            releaseInputPermissionIfUnused(document.uri, _uiState.value.jobs)
        }
    }

    private fun deleteJob(jobId: String) {
        viewModelScope.launch {
            runCatching {
                val state = _uiState.value
                val deleted = state.jobs.firstOrNull { it.id == jobId }
                    ?: historyRepository.getJob(jobId)
                historyRepository.deleteHistoryRecord(jobId)
                val remaining = state.jobs.filterNot { it.id == jobId }
                deleted?.items?.map { it.input.uri }?.distinct()?.forEach { uri ->
                    releaseInputPermissionIfUnused(uri, remaining)
                }
            }.onFailure { showNotice(UiNotice.GENERIC_ERROR) }
        }
    }

    private fun clearHistory() {
        viewModelScope.launch {
            runCatching {
                val jobs = _uiState.value.jobs
                historyRepository.clearHistory()
                jobs.flatMap { job -> job.items.map { it.input.uri } }
                    .distinct()
                    .forEach { uri -> releaseInputPermissionIfUnused(uri, emptyList()) }
            }.onFailure { showNotice(UiNotice.GENERIC_ERROR) }
        }
    }

    private fun releaseInputPermissionIfUnused(uri: String, remainingJobs: List<ConversionJob>) {
        val usedBySetup = _uiState.value.setup?.documents?.any { it.uri == uri } == true
        val usedByHistory = remainingJobs.any { job -> job.items.any { it.input.uri == uri } }
        if (usedBySetup || usedByHistory) return
        viewModelScope.launch {
            runCatching { documentGateway.releasePersistablePermission(Uri.parse(uri)) }
        }
    }

    private fun cancel(jobId: String) {
        scheduler.cancel(jobId)
        viewModelScope.launch {
            runCatching {
                val job = historyRepository.getJob(jobId) ?: return@runCatching
                job.items.filterNot { it.status.isTerminal }.forEach { item ->
                    historyRepository.updateItemProgress(
                        itemId = item.id,
                        status = ConversionStatus.CANCELLED,
                        progressPercent = item.progressPercent,
                        totalUnits = item.totalUnits,
                        selectedUnits = item.selectedUnits,
                    )
                }
                historyRepository.updateJobStatus(
                    jobId = jobId,
                    status = ConversionStatus.CANCELLED,
                    startedAtEpochMillis = job.startedAtEpochMillis,
                    finishedAtEpochMillis = System.currentTimeMillis(),
                )
            }.onFailure { showNotice(UiNotice.GENERIC_ERROR) }
        }
    }

    private fun updateSettings(transform: (AppSettings) -> AppSettings) {
        launchRepositoryAction { settingsRepository.update(transform) }
    }

    private fun launchRepositoryAction(block: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { block() }.onFailure { showNotice(UiNotice.GENERIC_ERROR) }
        }
    }

    private fun updateSetup(transform: ConversionSetupState.() -> ConversionSetupState) {
        _uiState.update { state -> state.copy(setup = state.setup?.transform()) }
    }

    private fun showNotice(notice: UiNotice) {
        _uiState.update { it.copy(notice = notice) }
    }

    private fun ConversionSetupState.toSelectionRequest(): SelectionRequest = when (selectionMode) {
        SelectionMode.ALL -> SelectionRequest.all()
        SelectionMode.FIRST -> SelectionRequest.first(count.toInt())
        SelectionMode.LAST -> SelectionRequest.last(count.toInt())
        SelectionMode.KEEP -> SelectionRequest.keep(expression)
        SelectionMode.REMOVE -> SelectionRequest.remove(expression)
    }

    class Factory(
        private val historyRepository: ConversionHistoryRepository,
        private val settingsRepository: SettingsRepository,
        private val documentGateway: SafDocumentGateway,
        private val scheduler: ConversionScheduler,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ConvertyViewModel::class.java))
            return ConvertyViewModel(
                historyRepository = historyRepository,
                settingsRepository = settingsRepository,
                documentGateway = documentGateway,
                scheduler = scheduler,
            ) as T
        }
    }
}
