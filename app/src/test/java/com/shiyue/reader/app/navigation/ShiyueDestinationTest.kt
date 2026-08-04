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
}
