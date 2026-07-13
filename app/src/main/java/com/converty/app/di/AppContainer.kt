package com.converty.app.di

import android.app.Application
import com.converty.app.core.conversion.PdfToPptxEngine
import com.converty.app.core.conversion.PptxToPdfEngine
import com.converty.app.core.conversion.ImageTranscodeEngine
import com.converty.app.core.conversion.PdfToRasterPagesEngine
import com.converty.app.core.conversion.PdfThumbnailEngine
import com.converty.app.core.conversion.PdfToTiffEngine
import com.converty.app.core.conversion.OfficeToPdfEngine
import com.converty.app.core.model.ConversionDirection
import com.converty.app.core.model.FileFormat
import com.converty.app.core.repository.ConversionHistoryRepository
import com.converty.app.core.repository.SettingsRepository
import com.converty.app.data.files.SafDocumentGateway
import com.converty.app.data.history.RoomConversionHistoryRepository
import com.converty.app.data.history.local.ConvertyDatabase
import com.converty.app.data.settings.PreferencesSettingsRepository
import com.converty.app.data.settings.convertySettingsDataStore
import com.converty.app.work.ConversionScheduler
import com.converty.app.work.ConversionWorkerFactory

/** Small application-scoped service locator; feature code depends on its interfaces. */
class AppContainer(application: Application) {
    private val appContext = application.applicationContext
    private val database by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        ConvertyDatabase.getInstance(appContext)
    }

    val historyRepository: ConversionHistoryRepository by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        RoomConversionHistoryRepository(database)
    }

    val settingsRepository: SettingsRepository by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        PreferencesSettingsRepository(appContext.convertySettingsDataStore)
    }

    val safDocumentGateway by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        SafDocumentGateway(appContext)
    }

    val pdfToPptxEngine by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        PdfToPptxEngine(appContext)
    }

    val pptxToPdfEngine by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        PptxToPdfEngine(appContext)
    }

    val pdfToPngEngine by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        PdfToRasterPagesEngine(appContext, FileFormat.PNG)
    }

    val pdfToJpgEngine by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        PdfToRasterPagesEngine(appContext, FileFormat.JPG)
    }

    val pdfToTiffEngine by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        PdfToTiffEngine(appContext)
    }

    val pdfThumbnailEngine by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        PdfThumbnailEngine(appContext)
    }

    private val officeToPdfEngines by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        setOf(FileFormat.DOCX, FileFormat.XLSX, FileFormat.ODT, FileFormat.ODS, FileFormat.ODP)
            .associateWith { OfficeToPdfEngine(appContext, it) }
    }

    fun officeToPdfEngine(source: FileFormat) = officeToPdfEngines[source]

    private val imageTranscodeEngines by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        ConversionDirection.entries
            .filter { direction ->
                direction.sourceFormat in setOf(
                    FileFormat.PNG,
                    FileFormat.JPG,
                    FileFormat.WEBP,
                    FileFormat.TIFF,
                    FileFormat.BMP,
                    FileFormat.HEIC_HEIF,
                    FileFormat.SVG,
                    FileFormat.AVIF,
                ) && direction.targetFormat in setOf(
                    FileFormat.PNG,
                    FileFormat.JPG,
                    FileFormat.WEBP,
                )
            }
            .associateWith { ImageTranscodeEngine(appContext, it) }
    }

    fun imageTranscodeEngine(direction: ConversionDirection) = imageTranscodeEngines[direction]

    val conversionScheduler by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        ConversionScheduler(appContext)
    }

    val workerFactory by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        ConversionWorkerFactory(this)
    }
}
