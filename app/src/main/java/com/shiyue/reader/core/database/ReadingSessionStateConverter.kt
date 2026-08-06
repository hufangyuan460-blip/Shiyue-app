package com.shiyue.reader.core.database

import androidx.room.TypeConverter
import com.shiyue.reader.core.model.ReadingSessionState

class ReadingSessionStateConverter {
    @TypeConverter fun fromState(value: ReadingSessionState): String = value.name
    @TypeConverter fun toState(value: String): ReadingSessionState = ReadingSessionState.valueOf(value)
}
