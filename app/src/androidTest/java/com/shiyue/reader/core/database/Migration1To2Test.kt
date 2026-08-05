package com.shiyue.reader.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class Migration1To2Test {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ShiyueDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    @Throws(IOException::class)
    fun migrationPreservesBooksAndCreatesEmptyCategoryRelations() {
        helper.createDatabase(TEST_DATABASE, 1).apply {
            execSQL(
                """INSERT INTO books
                    (id, title, author, cover_path, total_pages, current_page, status, created_at, updated_at)
                    VALUES ('00000000-0000-0000-0000-000000000001', 'Existing', NULL, NULL, 100, 10, 'READING', 1, 2)
                """.trimIndent(),
            )
            close()
        }

        helper.runMigrationsAndValidate(TEST_DATABASE, 2, true, MIGRATION_1_2).use { migrated ->
            migrated.query("SELECT COUNT(*) FROM books").use { cursor ->
                cursor.moveToFirst()
                assertEquals(1, cursor.getInt(0))
            }
            migrated.query("SELECT COUNT(*) FROM categories").use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
            }
            migrated.query("SELECT COUNT(*) FROM book_category_cross_ref").use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
            }
        }
    }

    private companion object {
        const val TEST_DATABASE = "migration-1-2"
    }
}
