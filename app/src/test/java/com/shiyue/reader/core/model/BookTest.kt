package com.shiyue.reader.core.model

import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class BookTest {
    @Test
    fun createNormalizesTitleAndAuthorAndUsesDefaults() {
        val book = Book.create(
            title = "  Kotlin 实战  ",
            author = "  示例作者  ",
            totalPages = 320,
            timestamp = 1_700_000_000_000,
        )

        UUID.fromString(book.id)
        assertEquals("Kotlin 实战", book.title)
        assertEquals("示例作者", book.author)
        assertNull(book.coverPath)
        assertEquals(0, book.currentPage)
        assertEquals(BookStatus.WISH, book.status)
        assertEquals(1_700_000_000_000, book.createdAt)
        assertEquals(book.createdAt, book.updatedAt)
    }

    @Test
    fun blankAuthorBecomesNull() {
        val book = Book.create(
            title = "书名",
            author = "   ",
            totalPages = 100,
        )

        assertNull(book.author)
    }

    @Test
    fun blankTitleIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            Book.create(title = "  ", author = null, totalPages = 100)
        }
    }

    @Test
    fun nonPositiveTotalPagesAreRejected() {
        listOf(0, -1).forEach { totalPages ->
            assertThrows(IllegalArgumentException::class.java) {
                Book.create(title = "书名", author = null, totalPages = totalPages)
            }
        }
    }

    @Test
    fun pageOutsideBookRangeIsRejected() {
        listOf(-1, 101).forEach { currentPage ->
            assertThrows(IllegalArgumentException::class.java) {
                Book.create(
                    title = "书名",
                    author = null,
                    totalPages = 100,
                    currentPage = currentPage,
                )
            }
        }
    }

    @Test
    fun progressIsCalculatedForFirstMiddleAndLastPage() {
        val firstPage = bookAt(currentPage = 0)
        val middlePage = bookAt(currentPage = 50)
        val lastPage = bookAt(currentPage = 100)

        assertEquals(0.0, firstPage.progress, 0.0)
        assertEquals(0.5, middlePage.progress, 0.0)
        assertEquals(1.0, lastPage.progress, 0.0)
    }

    private fun bookAt(currentPage: Int): Book = Book.create(
        title = "书名",
        author = null,
        totalPages = 100,
        currentPage = currentPage,
    )
}
