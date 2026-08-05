package com.shiyue.reader.core.database

import androidx.room.TypeConverter
import com.shiyue.reader.core.model.BookStatus

class BookStatusConverter {
    @TypeConverter
    fun fromBookStatus(status: BookStatus): String = status.name

    @TypeConverter
    fun toBookStatus(value: String): BookStatus = BookStatus.valueOf(value)
}
