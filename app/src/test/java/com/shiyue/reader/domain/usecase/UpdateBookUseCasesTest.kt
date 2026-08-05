package com.shiyue.reader.domain.usecase

import com.shiyue.reader.core.model.Book
import com.shiyue.reader.core.model.BookStatus
import com.shiyue.reader.testutil.FakeBookRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class UpdateBookUseCasesTest {
    @Test
    fun metadataUpdateNormalizesFieldsAndPreservesNonFormData() = runBlocking {
        val original = book(currentPage = 40, status = BookStatus.READING)
        val repository = FakeBookRepository(listOf(original))

        val updated = UpdateBookMetadataUseCase(repository)(
            original.id, "  新书名  ", "   ", 240, BookStatus.FINISHED, timestamp = 500,
        )

        assertEquals(original.id, updated.id)
        assertEquals(original.createdAt, updated.createdAt)
        assertEquals(original.coverPath, updated.coverPath)
        assertEquals(40, updated.currentPage)
        assertEquals("新书名", updated.title)
        assertNull(updated.author)
        assertEquals(240, updated.totalPages)
        assertEquals(BookStatus.FINISHED, updated.status)
        assertEquals(500, updated.updatedAt)
    }

    @Test
    fun metadataUpdateRejectsBlankTitleInvalidPagesAndPagesBelowCurrent() {
        val original = book(currentPage = 40)
        val useCase = UpdateBookMetadataUseCase(FakeBookRepository(listOf(original)))
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { useCase(original.id, " ", null, 100, original.status, 2) }
        }
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { useCase(original.id, "书", null, 0, original.status, 2) }
        }
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { useCase(original.id, "书", null, 39, original.status, 2) }
        }
    }

    @Test
    fun changingTotalPagesDoesNotChangeStatus() = runBlocking {
        val original = book(status = BookStatus.PAUSED)
        val updated = UpdateBookMetadataUseCase(FakeBookRepository(listOf(original)))(
            original.id, original.title, original.author, 300, original.status, 2,
        )
        assertEquals(BookStatus.PAUSED, updated.status)
    }

    @Test
    fun progressAcceptsZeroMiddleAndLastPageAndUpdatesTimestamp() = runBlocking {
        for (page in listOf(0, 50, 100)) {
            val original = book(currentPage = 20)
            val updated = UpdateBookProgressUseCase(FakeBookRepository(listOf(original)))(
                original.id, page, markFinished = false, timestamp = 900,
            )
            assertEquals(page, updated.currentPage)
            assertEquals(original.status, updated.status)
            assertEquals(900, updated.updatedAt)
        }
    }

    @Test
    fun progressRejectsNegativeAndPastLastPage() {
        val original = book()
        val useCase = UpdateBookProgressUseCase(FakeBookRepository(listOf(original)))
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { useCase(original.id, -1, false, 2) }
        }
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { useCase(original.id, 101, false, 2) }
        }
    }

    @Test
    fun markingFinishedChangesStatusWhilePageOnlyPreservesIt() = runBlocking {
        val first = book(status = BookStatus.READING)
        val marked = UpdateBookProgressUseCase(FakeBookRepository(listOf(first)))(first.id, 100, true, 2)
        assertEquals(BookStatus.FINISHED, marked.status)

        val second = book(status = BookStatus.PAUSED)
        val pageOnly = UpdateBookProgressUseCase(FakeBookRepository(listOf(second)))(second.id, 100, false, 2)
        assertEquals(BookStatus.PAUSED, pageOnly.status)
    }

    private fun book(
        currentPage: Int = 0,
        status: BookStatus = BookStatus.READING,
    ) = Book.create("原书名", "原作者", 100, currentPage, status, timestamp = 1)
}
