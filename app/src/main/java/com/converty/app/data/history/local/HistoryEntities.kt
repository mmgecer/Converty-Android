package com.converty.app.data.history.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.converty.app.core.model.ContentFit
import com.converty.app.core.model.ConversionDirection
import com.converty.app.core.model.ConversionErrorCode
import com.converty.app.core.model.ConversionQuality
import com.converty.app.core.model.ConversionStatus
import com.converty.app.core.model.NameCollisionPolicy
import com.converty.app.core.model.selection.SelectionMode

@Entity(tableName = "conversion_jobs")
data class ConversionJobEntity(
    @PrimaryKey val id: String,
    val direction: ConversionDirection,
    @ColumnInfo(name = "created_at") val createdAtEpochMillis: Long,
    @ColumnInfo(name = "started_at") val startedAtEpochMillis: Long?,
    @ColumnInfo(name = "finished_at") val finishedAtEpochMillis: Long?,
    val status: ConversionStatus,
    @ColumnInfo(name = "quality") val quality: ConversionQuality,
    @ColumnInfo(name = "content_fit") val fit: ContentFit,
    @ColumnInfo(name = "dpi") val dpi: Int,
    @ColumnInfo(name = "lossless") val lossless: Boolean,
    @ColumnInfo(name = "selection_mode") val selectionMode: SelectionMode,
    @ColumnInfo(name = "selection_count") val selectionCount: Int?,
    @ColumnInfo(name = "selection_expression") val selectionExpression: String?,
    @ColumnInfo(name = "destination_tree_uri") val destinationTreeUri: String?,
    @ColumnInfo(name = "file_name_pattern") val fileNamePattern: String,
    @ColumnInfo(name = "collision_policy") val collisionPolicy: NameCollisionPolicy,
    @ColumnInfo(name = "error_code") val errorCode: ConversionErrorCode?,
    @ColumnInfo(name = "error_diagnostic") val errorDiagnostic: String?,
    @ColumnInfo(name = "error_retryable") val errorRetryable: Boolean,
)

@Entity(
    tableName = "conversion_items",
    foreignKeys = [
        ForeignKey(
            entity = ConversionJobEntity::class,
            parentColumns = ["id"],
            childColumns = ["job_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["job_id"]), Index(value = ["job_id", "position"], unique = true)],
)
data class ConversionItemEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "job_id") val jobId: String,
    val position: Int,
    @ColumnInfo(name = "source_uri") val sourceUri: String,
    @ColumnInfo(name = "source_name") val sourceDisplayName: String,
    @ColumnInfo(name = "source_mime") val sourceMimeType: String?,
    @ColumnInfo(name = "source_size") val sourceSizeBytes: Long?,
    @ColumnInfo(name = "source_modified_at") val sourceLastModifiedEpochMillis: Long?,
    @ColumnInfo(name = "source_persisted_read") val sourceHasPersistedReadPermission: Boolean,
    @ColumnInfo(name = "total_units") val totalUnits: Int?,
    @ColumnInfo(name = "selected_units") val selectedUnits: Int?,
    @ColumnInfo(name = "progress_percent") val progressPercent: Int,
    val status: ConversionStatus,
    @ColumnInfo(name = "error_code") val errorCode: ConversionErrorCode?,
    @ColumnInfo(name = "error_diagnostic") val errorDiagnostic: String?,
    @ColumnInfo(name = "error_retryable") val errorRetryable: Boolean,
    @ColumnInfo(name = "warning_codes") val warningCodes: String?,
)

@Entity(
    tableName = "conversion_outputs",
    foreignKeys = [
        ForeignKey(
            entity = ConversionItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["item_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    primaryKeys = ["item_id", "position"],
    indices = [Index(value = ["item_id"])],
)
data class ConversionOutputEntity(
    @ColumnInfo(name = "item_id") val itemId: String,
    val position: Int,
    val uri: String,
    @ColumnInfo(name = "display_name") val displayName: String,
    @ColumnInfo(name = "mime_type") val mimeType: String,
    @ColumnInfo(name = "size_bytes") val sizeBytes: Long?,
    @ColumnInfo(name = "created_at") val createdAtEpochMillis: Long,
    @ColumnInfo(name = "is_readable") val isReadable: Boolean,
)
