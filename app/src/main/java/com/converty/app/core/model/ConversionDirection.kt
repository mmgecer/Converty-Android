package com.converty.app.core.model

enum class FormatCategory { DOCUMENT, IMAGE, PRESENTATION, SPREADSHEET, SPECIAL }

enum class FileFormat(
    val primaryExtension: String,
    val extensions: Set<String>,
    val mimeTypes: Set<String>,
    val category: FormatCategory,
    val canBeSource: Boolean = true,
) {
    PDF("pdf", setOf("pdf"), setOf("application/pdf"), FormatCategory.DOCUMENT),
    PPTX(
        "pptx",
        setOf("pptx"),
        setOf("application/vnd.openxmlformats-officedocument.presentationml.presentation"),
        FormatCategory.PRESENTATION,
    ),
    PNG("png", setOf("png"), setOf("image/png"), FormatCategory.IMAGE),
    JPG("jpg", setOf("jpg", "jpeg"), setOf("image/jpeg"), FormatCategory.IMAGE),
    WEBP("webp", setOf("webp"), setOf("image/webp"), FormatCategory.IMAGE),
    TIFF("tiff", setOf("tif", "tiff"), setOf("image/tiff"), FormatCategory.IMAGE),
    BMP("bmp", setOf("bmp"), setOf("image/bmp", "image/x-ms-bmp"), FormatCategory.IMAGE),
    HEIC_HEIF(
        "heic",
        setOf("heic", "heif"),
        setOf("image/heic", "image/heif", "image/heic-sequence", "image/heif-sequence"),
        FormatCategory.IMAGE,
    ),
    SVG("svg", setOf("svg"), setOf("image/svg+xml"), FormatCategory.IMAGE),
    AVIF("avif", setOf("avif"), setOf("image/avif"), FormatCategory.IMAGE),
    DOCX(
        "docx",
        setOf("docx"),
        setOf("application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
        FormatCategory.DOCUMENT,
    ),
    XLSX(
        "xlsx",
        setOf("xlsx"),
        setOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
        FormatCategory.SPREADSHEET,
    ),
    ODT("odt", setOf("odt"), setOf("application/vnd.oasis.opendocument.text"), FormatCategory.DOCUMENT),
    ODS(
        "ods",
        setOf("ods"),
        setOf("application/vnd.oasis.opendocument.spreadsheet"),
        FormatCategory.SPREADSHEET,
    ),
    ODP(
        "odp",
        setOf("odp"),
        setOf("application/vnd.oasis.opendocument.presentation"),
        FormatCategory.PRESENTATION,
    ),
    THUMBNAIL(
        "jpg",
        setOf("jpg"),
        setOf("image/jpeg"),
        FormatCategory.SPECIAL,
        canBeSource = false,
    ),
    ;

    val preferredMimeType: String get() = mimeTypes.first()
    val isRasterImage: Boolean
        get() = this in setOf(PNG, JPG, WEBP, TIFF, BMP, HEIC_HEIF, AVIF, THUMBNAIL)

    fun accepts(displayName: String, mimeType: String?): Boolean {
        val extension = displayName.substringAfterLast('.', "").lowercase()
        return extension in extensions || mimeType?.lowercase() in mimeTypes
    }
}

enum class ConversionDirection(
    val sourceFormat: FileFormat,
    val targetFormat: FileFormat,
) {
    PDF_TO_PPTX(FileFormat.PDF, FileFormat.PPTX),
    PPTX_TO_PDF(FileFormat.PPTX, FileFormat.PDF),

    PNG_TO_JPG(FileFormat.PNG, FileFormat.JPG),
    JPG_TO_PNG(FileFormat.JPG, FileFormat.PNG),
    PNG_TO_WEBP(FileFormat.PNG, FileFormat.WEBP),
    WEBP_TO_PNG(FileFormat.WEBP, FileFormat.PNG),
    JPG_TO_WEBP(FileFormat.JPG, FileFormat.WEBP),
    WEBP_TO_JPG(FileFormat.WEBP, FileFormat.JPG),
    TIFF_TO_PNG(FileFormat.TIFF, FileFormat.PNG),
    TIFF_TO_JPG(FileFormat.TIFF, FileFormat.JPG),
    BMP_TO_PNG(FileFormat.BMP, FileFormat.PNG),
    BMP_TO_JPG(FileFormat.BMP, FileFormat.JPG),
    HEIC_HEIF_TO_JPG(FileFormat.HEIC_HEIF, FileFormat.JPG),
    HEIC_HEIF_TO_PNG(FileFormat.HEIC_HEIF, FileFormat.PNG),
    SVG_TO_PNG(FileFormat.SVG, FileFormat.PNG),
    AVIF_TO_PNG(FileFormat.AVIF, FileFormat.PNG),
    AVIF_TO_JPG(FileFormat.AVIF, FileFormat.JPG),
    AVIF_TO_WEBP(FileFormat.AVIF, FileFormat.WEBP),

    DOCX_TO_PDF(FileFormat.DOCX, FileFormat.PDF),
    XLSX_TO_PDF(FileFormat.XLSX, FileFormat.PDF),
    ODT_TO_PDF(FileFormat.ODT, FileFormat.PDF),
    ODS_TO_PDF(FileFormat.ODS, FileFormat.PDF),
    ODP_TO_PDF(FileFormat.ODP, FileFormat.PDF),

    PDF_TO_PNG(FileFormat.PDF, FileFormat.PNG),
    PDF_TO_JPG(FileFormat.PDF, FileFormat.JPG),
    PDF_TO_TIFF(FileFormat.PDF, FileFormat.TIFF),
    PDF_TO_THUMBNAIL(FileFormat.PDF, FileFormat.THUMBNAIL),
    ;

    val sourceExtension: String get() = sourceFormat.primaryExtension
    val targetExtension: String get() = targetFormat.primaryExtension
    val sourceMimeType: String get() = sourceFormat.preferredMimeType
    val targetMimeType: String get() = targetFormat.preferredMimeType
    val supportsItemSelection: Boolean
        get() = (sourceFormat == FileFormat.PDF || sourceFormat == FileFormat.PPTX) &&
            this != PDF_TO_THUMBNAIL
    val supportsDpi: Boolean
        get() = sourceFormat == FileFormat.PDF && this != PDF_TO_THUMBNAIL &&
            (targetFormat.isRasterImage || targetFormat == FileFormat.PPTX)
    val supportsLosslessChoice: Boolean get() = targetFormat == FileFormat.WEBP
    val alwaysLossless: Boolean get() = targetFormat == FileFormat.PNG || targetFormat == FileFormat.TIFF
    val preservesAppearanceAsImages: Boolean get() = this == PDF_TO_PPTX

    fun reversedOrNull(): ConversionDirection? = from(targetFormat, sourceFormat)

    companion object {
        fun from(source: FileFormat, target: FileFormat): ConversionDirection? =
            entries.firstOrNull { it.sourceFormat == source && it.targetFormat == target }

        fun targetsFor(source: FileFormat): List<FileFormat> = entries
            .asSequence()
            .filter { it.sourceFormat == source }
            .map { it.targetFormat }
            .distinct()
            .toList()

        val sourceFormats: List<FileFormat> = entries
            .asSequence()
            .map { it.sourceFormat }
            .distinct()
            .toList()
    }
}
