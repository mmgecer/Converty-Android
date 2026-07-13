package com.converty.app.core.conversion

import android.net.Uri

enum class DocumentFormat(val mimeType: String) {
    PDF("application/pdf"),
    PPTX("application/vnd.openxmlformats-officedocument.presentationml.presentation"),
    PNG("image/png"),
    JPG("image/jpeg"),
    WEBP("image/webp"),
    TIFF("image/tiff"),
    BMP("image/bmp"),
    HEIC_HEIF("image/heic"),
    SVG("image/svg+xml"),
    AVIF("image/avif"),
    DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
    XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    ODT("application/vnd.oasis.opendocument.text"),
    ODS("application/vnd.oasis.opendocument.spreadsheet"),
    ODP("application/vnd.oasis.opendocument.presentation"),
    THUMBNAIL("image/jpeg"),
}

enum class ImageQuality(val dpi: Int, val jpegQuality: Int) {
    COMPACT(108, 78),
    BALANCED(144, 88),
    HIGH(216, 94),
    MAXIMUM(300, 100),
}

enum class ImageFit { CONTAIN, COVER, STRETCH }

enum class SlideSizePreset {
    MATCH_FIRST_SELECTED_PAGE,
    WIDESCREEN_16_9,
    STANDARD_4_3,
}

data class ConversionRequest(
    val source: Uri,
    val destination: Uri,
    /** Normalized, distinct, ascending, zero-based page/slide indexes. Empty means all. */
    val selectedItemIndices: List<Int> = emptyList(),
    val imageQuality: ImageQuality = ImageQuality.BALANCED,
    val imageFit: ImageFit = ImageFit.CONTAIN,
    val dpi: Int = 300,
    val lossless: Boolean = true,
    val slideSizePreset: SlideSizePreset = SlideSizePreset.MATCH_FIRST_SELECTED_PAGE,
) {
    init {
        require(dpi in 72..600) { "DPI must be between 72 and 600" }
    }
}

data class ConversionCapabilities(
    val sourceFormat: DocumentFormat,
    val destinationFormat: DocumentFormat,
    val supportsItemSelection: Boolean,
    val preservesEditableText: Boolean,
    val notes: Set<String> = emptySet(),
)

fun interface ConversionCancellation {
    fun isCancellationRequested(): Boolean

    companion object {
        val NONE = ConversionCancellation { false }
    }
}

data class ConversionProgress(
    val completedItems: Int,
    val totalItems: Int,
    val stage: Stage,
) {
    enum class Stage { READING, RENDERING, PACKAGING, WRITING }

    val fraction: Float
        get() = if (totalItems == 0) 0f else completedItems.toFloat() / totalItems
}

data class ConversionWarning(
    val code: String,
    val message: String,
    val itemIndex: Int? = null,
)

data class DocumentInspection(
    val format: DocumentFormat,
    val itemCount: Int,
    val warnings: List<ConversionWarning> = emptyList(),
)

sealed interface DocumentInspectionResult {
    data class Success(val inspection: DocumentInspection) : DocumentInspectionResult
    data class Failure(
        val code: ErrorCode,
        val message: String,
        val cause: Throwable? = null,
    ) : DocumentInspectionResult

    data object Cancelled : DocumentInspectionResult
}

sealed interface ConversionResult {
    data class Success(
        val output: Uri,
        val convertedItems: Int,
        val bytesWritten: Long,
        val warnings: List<ConversionWarning> = emptyList(),
    ) : ConversionResult

    data class Failure(
        val code: ErrorCode,
        val message: String,
        val cause: Throwable? = null,
    ) : ConversionResult

    data class Unsupported(
        val features: Set<String>,
        val warnings: List<ConversionWarning>,
    ) : ConversionResult

    data object Cancelled : ConversionResult
}

enum class ErrorCode {
    SOURCE_NOT_READABLE,
    DESTINATION_NOT_WRITABLE,
    INVALID_SELECTION,
    MALFORMED_DOCUMENT,
    ENCRYPTED_DOCUMENT,
    SECURITY_LIMIT_EXCEEDED,
    OUT_OF_MEMORY,
    IO_ERROR,
    INTERNAL_ERROR,
}

interface ConversionEngine {
    val capabilities: ConversionCapabilities

    /** Reads only enough metadata to resolve FIRST/LAST/KEEP/REMOVE before conversion. */
    fun inspect(
        source: Uri,
        cancellation: ConversionCancellation = ConversionCancellation.NONE,
    ): DocumentInspectionResult

    fun convert(
        request: ConversionRequest,
        cancellation: ConversionCancellation = ConversionCancellation.NONE,
        onProgress: (ConversionProgress) -> Unit = {},
    ): ConversionResult
}

data class IndexedDestination(
    val itemIndex: Int,
    val uri: Uri,
)

data class MultiOutputConversionRequest(
    val source: Uri,
    val destinations: List<IndexedDestination>,
    val imageQuality: ImageQuality = ImageQuality.BALANCED,
    val dpi: Int = 300,
) {
    init {
        require(destinations.isNotEmpty()) { "At least one destination is required" }
        require(destinations.map { it.itemIndex }.distinct().size == destinations.size) {
            "Destination item indexes must be unique"
        }
        require(dpi in 72..600) { "DPI must be between 72 and 600" }
    }
}

data class ProducedOutput(
    val itemIndex: Int,
    val uri: Uri,
    val bytesWritten: Long,
)

sealed interface MultiOutputConversionResult {
    data class Success(
        val outputs: List<ProducedOutput>,
        val warnings: List<ConversionWarning> = emptyList(),
    ) : MultiOutputConversionResult

    data class Failure(
        val code: ErrorCode,
        val message: String,
        val cause: Throwable? = null,
    ) : MultiOutputConversionResult

    data class Unsupported(
        val features: Set<String>,
        val warnings: List<ConversionWarning> = emptyList(),
    ) : MultiOutputConversionResult

    data object Cancelled : MultiOutputConversionResult
}

interface MultiOutputConversionEngine : ConversionEngine {
    fun convertMultiple(
        request: MultiOutputConversionRequest,
        cancellation: ConversionCancellation = ConversionCancellation.NONE,
        onProgress: (ConversionProgress) -> Unit = {},
    ): MultiOutputConversionResult

    override fun convert(
        request: ConversionRequest,
        cancellation: ConversionCancellation,
        onProgress: (ConversionProgress) -> Unit,
    ): ConversionResult = ConversionResult.Failure(
        ErrorCode.INTERNAL_ERROR,
        "This engine requires one destination per selected item",
    )
}

internal fun normalizeSelection(selection: List<Int>, total: Int): List<Int>? {
    if (total <= 0) return emptyList()
    if (selection.isEmpty()) return (0 until total).toList()
    if (selection.any { it !in 0 until total }) return null
    return selection.distinct().sorted()
}
