package com.shiyue.reader.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CategoryTest {
    @Test
    fun `create trims name and uses locale independent compatibility normalization`() {
        val category = Category.create("  ＣＯＤＥ  ", sortOrder = 0, timestamp = 10)

        assertEquals("ＣＯＤＥ", category.name)
        assertEquals("code", category.normalizedName)
        assertEquals(10, category.createdAt)
        assertEquals(10, category.updatedAt)
    }

    @Test
    fun `blank and names over twenty Unicode code points are rejected`() {
        assertThrows(IllegalArgumentException::class.java) { Category.create("  ", 0) }
        assertThrows(IllegalArgumentException::class.java) { Category.create("书".repeat(21), 0) }
        assertEquals(20, Category.create("书".repeat(20), 0).name.codePointCount(0, 20))
    }

    @Test
    fun `rename preserves identity and creation time`() {
        val original = Category.create("文学", 2, timestamp = 10)
        val renamed = original.renamed(" 历史 ", timestamp = 20)

        assertEquals(original.id, renamed.id)
        assertEquals(original.createdAt, renamed.createdAt)
        assertEquals("历史", renamed.name)
        assertEquals(20, renamed.updatedAt)
        assertNotEquals(original.normalizedName, renamed.normalizedName)
    }
}
