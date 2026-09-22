package com.shiyue.reader.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class Migration3To4Test {
    @get:Rule val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(), ShiyueDatabase::class.java, emptyList(), FrameworkSQLiteOpenHelperFactory(),
    )

    @Test fun migrationPreservesDataAndAddsEmptyNotesTable() {
        helper.createDatabase(NAME, 3).apply {
            execSQL("INSERT INTO books (id,title,author,cover_path,total_pages,current_page,status,created_at,updated_at) VALUES ('00000000-0000-0000-0000-000000000001','Existing',NULL,NULL,100,10,'READING',1,2)")
            close()
        }
        helper.runMigrationsAndValidate(NAME, 4, true, MIGRATION_3_4).use { db ->
            db.query("SELECT COUNT(*) FROM books").use { it.moveToFirst(); assertEquals(1, it.getInt(0)) }
            db.query("SELECT COUNT(*) FROM notes").use { it.moveToFirst(); assertEquals(0, it.getInt(0)) }
        }
    }

    @Test fun noteSessionForeignKeyUsesSetNull() {
        helper.createDatabase(NAME, 3).apply {
            execSQL("INSERT INTO books (id,title,author,cover_path,total_pages,current_page,status,created_at,updated_at) VALUES ('00000000-0000-0000-0000-000000000001','Existing',NULL,NULL,100,10,'READING',1,2)")
            close()
        }
        helper.runMigrationsAndValidate(NAME, 4, true, MIGRATION_3_4).use { db ->
            db.execSQL("INSERT INTO reading_sessions (id,book_id,state,started_at_epoch_ms,ended_at_epoch_ms,start_page,end_page,active_duration_ms,active_segment_started_at_epoch_ms,active_segment_started_at_elapsed_realtime_ms,update_book_progress,created_at,updated_at) VALUES ('20000000-0000-0000-0000-000000000001','00000000-0000-0000-0000-000000000001','COMPLETED',1,2,0,1,1000,NULL,NULL,0,1,2)")
            db.execSQL("INSERT INTO notes (id,book_id,session_id,page_number,content,image_path,created_at,updated_at) VALUES ('30000000-0000-0000-0000-000000000001','00000000-0000-0000-0000-000000000001','20000000-0000-0000-0000-000000000001',5,'Note',NULL,1,1)")
            db.execSQL("DELETE FROM reading_sessions WHERE id = '20000000-0000-0000-0000-000000000001'")
            db.query("SELECT session_id FROM notes").use {
                it.moveToFirst()
                assertTrue(it.isNull(0))
            }
        }
    }

    private companion object { const val NAME = "migration-3-4" }
}
