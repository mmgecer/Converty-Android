package com.converty.app.data.history.local

import androidx.room.Embedded
import androidx.room.Relation

data class ConversionItemWithOutputs(
    @Embedded val item: ConversionItemEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "item_id",
    )
    val outputs: List<ConversionOutputEntity>,
)

data class ConversionJobWithItems(
    @Embedded val job: ConversionJobEntity,
    @Relation(
        entity = ConversionItemEntity::class,
        parentColumn = "id",
        entityColumn = "job_id",
    )
    val items: List<ConversionItemWithOutputs>,
)
