package com.shiyue.reader.core.data.repository

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.shiyue.reader.core.database.ShiyueDatabase
import com.shiyue.reader.core.model.Category
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, manifest = Config.NONE)
class OfflineCategoryRepositoryTest {
    private lateinit var database: ShiyueDatabase
    private lateinit var repository: OfflineCategoryRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ShiyueDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = OfflineCategoryRepository(database)
    }

    @After fun tearDown() = database.close()

    @Test
    fun `create conflict rename reorder and delete are enforced`() = runBlocking {
        val literature = Category.create("文学", 0, timestamp = 10)
        val history = Category.create("历史", 1, timestamp = 11)
        repository.createCategory(literature)
        repository.createCategory(history)

        assertThrows(IllegalStateException::class.java) {
            runBlocking { repository.createCategory(Category.create(" 文学 ", 2)) }
        }
        repository.moveCategory(history.id, -1, timestamp = 20)
        assertEquals(listOf(history.id, literature.id), repository.observeCategories().first().map { it.category.id })

        repository.renameCategory(literature.id, "小说", timestamp = 30)
        repository.deleteCategory(history.id)
        val remaining = repository.observeCategories().first().single().category
        assertEquals("小说", remaining.name)
        assertEquals(0, remaining.sortOrder)
        assertFalse(repository.nameExists(Category.normalize("历史")))
    }
}
