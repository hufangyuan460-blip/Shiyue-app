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
