package com.converty.app.core.model

data class ConversionItem(
    val id: String,
    val jobId: String,
    val position: Int,
    val input: ConversionDocument,
    val outputs: List<ConversionOutput> = emptyList(),
    val totalUnits: Int? = null,
    val selectedUnits: Int? = null,
    val progressPercent: Int = 0,
    val status: ConversionStatus = ConversionStatus.QUEUED,
    val error: ConversionError? = null,
    val warningCodes: List<String> = emptyList(),
) {
    val output: ConversionOutput? get() = outputs.firstOrNull()

    init {
        require(id.isNotBlank()) { "Item id cannot be blank" }
        require(jobId.isNotBlank()) { "Job id cannot be blank" }
        require(position >= 0) { "Item position cannot be negative" }
        require(totalUnits == null || totalUnits >= 0) { "Total units cannot be negative" }
        require(selectedUnits == null || selectedUnits >= 0) { "Selected units cannot be negative" }
        require(progressPercent in 0..100) { "Progress must be between 0 and 100" }
    }
}
