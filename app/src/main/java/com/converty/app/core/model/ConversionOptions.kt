package com.converty.app.core.model

import com.converty.app.core.model.selection.SelectionRequest

enum class NameCollisionPolicy {
    CREATE_UNIQUE,
    REPLACE,
    FAIL,
}

data class OutputOptions(
    val destinationTreeUri: String? = null,
    val fileNamePattern: String = "{name}",
    val collisionPolicy: NameCollisionPolicy = NameCollisionPolicy.CREATE_UNIQUE,
) {
    init {
        require(fileNamePattern.isNotBlank()) { "File name pattern cannot be blank" }
    }
}

data class ConversionOptions(
    val selection: SelectionRequest = SelectionRequest.all(),
    val quality: ConversionQuality = ConversionQuality.BALANCED,
    val fit: ContentFit = ContentFit.CONTAIN,
    val dpi: Int = 300,
    val lossless: Boolean = true,
    val output: OutputOptions = OutputOptions(),
) {
    init {
        require(dpi in 72..600) { "DPI must be between 72 and 600" }
    }
}
