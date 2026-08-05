package com.shiyue.reader.domain.usecase

import com.shiyue.reader.core.model.Book
import com.shiyue.reader.core.model.BookStatus
import com.shiyue.reader.testutil.FakeBookRepository
import com.shiyue.reader.testutil.FakeCoverStorage
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CoverUseCasesTest {
    @Test
    fun `replacing cover persists new path then removes old file`() = runTest {
        val original = Book.create("Book", null, 100, coverPath = "covers/old.jpg")
        val repository = FakeBookRepository(listOf(original))
        val storage = FakeCoverStorage()

        val updated = UpdateBookWithCoverUseCase(repository, storage)(
            bookId = original.id,
            title = original.title,
            author = null,
            totalPages = 100,
            status = BookStatus.READING,
            categoryIds = emptySet(),
            coverChange = CoverChange.Replace(PendingCover("content://source", com.shiyue.reader.domain.repository.CoverTransform())),
        )

        assertEquals("covers/fake-1.jpg", updated.coverPath)
        assertEquals(listOf("covers/old.jpg"), storage.deleted)
    }

    @Test
    fun `deleting book is not rolled back when cover cleanup cannot remove file`() = runTest {
        val original = Book.create("Book", null, 100, coverPath = "covers/old.jpg")
        val repository = FakeBookRepository(listOf(original))
        val storage = object : FakeCoverStorage() {
            override suspend fun deleteCover(relativePath: String): Boolean = false
        }

        DeleteBookUseCase(repository, storage)(original.id)

        assertNull(repository.getBook(original.id))
    }
}
