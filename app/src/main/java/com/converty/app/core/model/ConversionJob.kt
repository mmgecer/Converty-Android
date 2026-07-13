package com.converty.app.core.model

data class ConversionJob(
    val id: String,
    val direction: ConversionDirection,
    val createdAtEpochMillis: Long,
    val startedAtEpochMillis: Long? = null,
    val finishedAtEpochMillis: Long? = null,
    val status: ConversionStatus = ConversionStatus.QUEUED,
    val options: ConversionOptions = ConversionOptions(),
    val items: List<ConversionItem> = emptyList(),
    val error: ConversionError? = null,
) {
    init {
        require(id.isNotBlank()) { "Job id cannot be blank" }
        require(items.all { it.jobId == id }) { "Every item must belong to this job" }
        require(items.map { it.position }.distinct().size == items.size) {
            "Item positions must be unique inside a job"
        }
    }
}
