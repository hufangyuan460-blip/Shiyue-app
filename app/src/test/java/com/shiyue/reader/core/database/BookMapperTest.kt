package com.shiyue.reader.core.database

import com.shiyue.reader.core.model.Book
import com.shiyue.reader.core.model.BookStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class BookMapperTest {
    @Test
    fun bookMapsToEntityAndBackWithoutLosingData() {
        val book = Book.restore(
            id = "00000000-0000-0000-0000-000000000001",
            title = "书名",
            author = "作者",
            coverPath = null,
            totalPages = 240,
            currentPage = 60,
            status = BookStatus.READING,
            createdAt = 100,
            updatedAt = 200,
        )

        val entity = book.asEntity()
        val restored = entity.asExternalModel()

        assertEquals(book, restored)
        assertEquals("书名", entity.title)
        assertEquals(240, entity.totalPages)
        assertEquals(60, entity.currentPage)
        assertEquals(BookStatus.READING, entity.status)
    }
}
