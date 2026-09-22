package com.shiyue.reader.core.database

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, manifest = Config.NONE)
class Migration3To4JvmTest {
    @Test
    fun `Room opens a version three database through migration with an empty notes table`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase(DATABASE_NAME)
        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(DATABASE_NAME)
            .callback(object : SupportSQLiteOpenHelper.Callback(3) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """CREATE TABLE IF NOT EXISTS books (
                            id TEXT NOT NULL, title TEXT NOT NULL, author TEXT, cover_path TEXT,
                            total_pages INTEGER NOT NULL, current_page INTEGER NOT NULL DEFAULT 0,
                            status TEXT NOT NULL, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL,
                            PRIMARY KEY(id))
                        """.trimIndent(),
                    )
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_books_status ON books(status)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_books_updated_at ON books(updated_at)")
                    MIGRATION_1_2.migrate(db)
                    MIGRATION_2_3.migrate(db)
                    db.execSQL(
                        """INSERT INTO books VALUES
                            ('00000000-0000-0000-0000-000000000001', 'Existing', NULL, NULL,
                            100, 10, 'READING', 1, 2)
                        """.trimIndent(),
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
            .build()
        FrameworkSQLiteOpenHelperFactory().create(configuration).use { it.writableDatabase }

        val database = Room.databaseBuilder(context, ShiyueDatabase::class.java, DATABASE_NAME)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .allowMainThreadQueries()
            .build()
        try {
            assertEquals("Existing", database.bookDao().getById("00000000-0000-0000-0000-000000000001")?.title)
            assertEquals(0, database.noteDao().observeAll().first().size)
            assertNull(database.noteDao().getById("10000000-0000-0000-0000-000000000001"))
        } finally {
            database.close()
            context.deleteDatabase(DATABASE_NAME)
        }
    }

    private companion object { const val DATABASE_NAME = "migration-3-4-jvm.db" }
}
