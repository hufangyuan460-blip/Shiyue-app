package com.shiyue.reader.app.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShiyueDestinationTest {
    @Test
    fun navigationContainsFourUniqueDestinations() {
        val destinations = ShiyueDestination.entries

        assertEquals(4, destinations.size)
        assertEquals(destinations.size, destinations.map { it.route }.toSet().size)
        assertTrue(destinations.first() == ShiyueDestination.Bookshelf)
    }

    @Test
    fun secondaryRoutesContainOnlyBookId() {
        val id = "00000000-0000-0000-0000-000000000001"
        assertEquals("book/$id", ShiyueRoutes.bookDetail(id))
        assertEquals("book/$id/edit", ShiyueRoutes.editBook(id))
        assertEquals("book/$id/progress", ShiyueRoutes.updateProgress(id))
    }
}
