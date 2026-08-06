package com.shiyue.reader.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class Migration2To3Test {
    @get:Rule val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(), ShiyueDatabase::class.java, emptyList(), FrameworkSQLiteOpenHelperFactory(),
    )

    @Test fun migrationPreservesLibraryAndAddsEmptyReadingTables() {
        helper.createDatabase(NAME, 2).apply {
            execSQL("INSERT INTO books (id,title,author,cover_path,total_pages,current_page,status,created_at,updated_at) VALUES ('00000000-0000-0000-0000-000000000001','Existing',NULL,NULL,100,10,'READING',1,2)")
            execSQL("INSERT INTO categories (id,name,normalized_name,sort_order,created_at,updated_at) VALUES ('10000000-0000-0000-0000-000000000001','Test','test',0,1,1)")
            execSQL("INSERT INTO book_category_cross_ref (book_id,category_id) VALUES ('00000000-0000-0000-0000-000000000001','10000000-0000-0000-0000-000000000001')")
            close()
        }
        helper.runMigrationsAndValidate(NAME, 3, true, MIGRATION_2_3).use { db ->
            db.query("SELECT COUNT(*) FROM books").use { it.moveToFirst(); assertEquals(1, it.getInt(0)) }
            db.query("SELECT COUNT(*) FROM categories").use { it.moveToFirst(); assertEquals(1, it.getInt(0)) }
            db.query("SELECT COUNT(*) FROM book_category_cross_ref").use { it.moveToFirst(); assertEquals(1, it.getInt(0)) }
            db.query("SELECT COUNT(*) FROM reading_sessions").use { it.moveToFirst(); assertEquals(0, it.getInt(0)) }
            db.query("SELECT COUNT(*) FROM active_reading_session").use { it.moveToFirst(); assertEquals(0, it.getInt(0)) }
        }
    }
    private companion object { const val NAME = "migration-2-3" }
}
