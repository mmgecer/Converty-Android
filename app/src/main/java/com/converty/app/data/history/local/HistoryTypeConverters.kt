package com.converty.app.data.history.local

import androidx.room.TypeConverter
import com.converty.app.core.model.ContentFit
import com.converty.app.core.model.ConversionDirection
import com.converty.app.core.model.ConversionErrorCode
import com.converty.app.core.model.ConversionQuality
import com.converty.app.core.model.ConversionStatus
import com.converty.app.core.model.NameCollisionPolicy
import com.converty.app.core.model.selection.SelectionMode

class HistoryTypeConverters {
    @TypeConverter fun directionToString(value: ConversionDirection): String = value.name
    @TypeConverter fun stringToDirection(value: String): ConversionDirection = ConversionDirection.valueOf(value)

    @TypeConverter fun statusToString(value: ConversionStatus): String = value.name
    @TypeConverter fun stringToStatus(value: String): ConversionStatus = ConversionStatus.valueOf(value)

    @TypeConverter fun errorCodeToString(value: ConversionErrorCode?): String? = value?.name
    @TypeConverter fun stringToErrorCode(value: String?): ConversionErrorCode? =
        value?.let(ConversionErrorCode::valueOf)

    @TypeConverter fun qualityToString(value: ConversionQuality): String = value.name
    @TypeConverter fun stringToQuality(value: String): ConversionQuality = ConversionQuality.valueOf(value)

    @TypeConverter fun fitToString(value: ContentFit): String = value.name
    @TypeConverter fun stringToFit(value: String): ContentFit = ContentFit.valueOf(value)

    @TypeConverter fun selectionModeToString(value: SelectionMode): String = value.name
    @TypeConverter fun stringToSelectionMode(value: String): SelectionMode = SelectionMode.valueOf(value)

    @TypeConverter fun collisionPolicyToString(value: NameCollisionPolicy): String = value.name
    @TypeConverter fun stringToCollisionPolicy(value: String): NameCollisionPolicy =
        NameCollisionPolicy.valueOf(value)
}
