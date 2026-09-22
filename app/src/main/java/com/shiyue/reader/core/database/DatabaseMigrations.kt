package com.shiyue.reader.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS `categories` (
                `id` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `normalized_name` TEXT NOT NULL,
                `sort_order` INTEGER NOT NULL,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )""".trimIndent(),
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_categories_normalized_name` " +
                "ON `categories` (`normalized_name`)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_categories_sort_order` ON `categories` (`sort_order`)",
        )
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS `book_category_cross_ref` (
                `book_id` TEXT NOT NULL,
                `category_id` TEXT NOT NULL,
                PRIMARY KEY(`book_id`, `category_id`),
                FOREIGN KEY(`book_id`) REFERENCES `books`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`category_id`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )""".trimIndent(),
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_book_category_cross_ref_book_id` " +
                "ON `book_category_cross_ref` (`book_id`)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_book_category_cross_ref_category_id` " +
                "ON `book_category_cross_ref` (`category_id`)",
        )
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS `reading_sessions` (
                `id` TEXT NOT NULL,
                `book_id` TEXT NOT NULL,
                `state` TEXT NOT NULL,
                `started_at_epoch_ms` INTEGER NOT NULL,
                `ended_at_epoch_ms` INTEGER,
                `start_page` INTEGER NOT NULL,
                `end_page` INTEGER,
                `active_duration_ms` INTEGER NOT NULL,
                `active_segment_started_at_epoch_ms` INTEGER,
                `active_segment_started_at_elapsed_realtime_ms` INTEGER,
                `update_book_progress` INTEGER NOT NULL,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`book_id`) REFERENCES `books`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )""".trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_reading_sessions_book_id` ON `reading_sessions` (`book_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_reading_sessions_state` ON `reading_sessions` (`state`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_reading_sessions_started_at_epoch_ms` ON `reading_sessions` (`started_at_epoch_ms`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_reading_sessions_book_id_started_at_epoch_ms` ON `reading_sessions` (`book_id`, `started_at_epoch_ms`)")
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS `active_reading_session` (
                `slot` INTEGER NOT NULL,
                `session_id` TEXT NOT NULL,
                PRIMARY KEY(`slot`),
                FOREIGN KEY(`session_id`) REFERENCES `reading_sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )""".trimIndent(),
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_active_reading_session_session_id` ON `active_reading_session` (`session_id`)")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS `notes` (
                `id` TEXT NOT NULL,
                `book_id` TEXT NOT NULL,
                `session_id` TEXT,
                `page_number` INTEGER,
                `content` TEXT NOT NULL,
                `image_path` TEXT,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`book_id`) REFERENCES `books`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`session_id`) REFERENCES `reading_sessions`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
            )""".trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_book_id` ON `notes` (`book_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_session_id` ON `notes` (`session_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_book_id_created_at` ON `notes` (`book_id`, `created_at`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_created_at` ON `notes` (`created_at`)")
    }
}
