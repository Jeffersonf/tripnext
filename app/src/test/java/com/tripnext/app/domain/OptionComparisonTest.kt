package com.tripnext.app.domain

import com.tripnext.app.data.local.ItineraryType
import com.tripnext.app.data.local.TripOptionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OptionComparisonTest {
    private fun option(id: String, group: String, price: Long, chosen: Boolean = false) = TripOptionEntity(id, "trip", group, id, ItineraryType.CHECK_IN, estimatedCostMinor = price, chosen = chosen)

    @Test fun `escolher alternativa preserva as demais e troca escolha do grupo`() {
        val result = OptionComparison.choose(listOf(option("a", "hotel", 1200, true), option("b", "hotel", 900), option("c", "voo", 500)), "b")
        assertFalse(result.first { it.id == "a" }.chosen)
        assertTrue(result.first { it.id == "b" }.chosen)
        assertFalse(result.first { it.id == "c" }.chosen)
        assertEquals(3, result.size)
    }

    @Test fun `identifica menor custo previsto`() {
        assertEquals("b", OptionComparison.cheapestId(listOf(option("a", "hotel", 1200), option("b", "hotel", 900))))
    }
}
