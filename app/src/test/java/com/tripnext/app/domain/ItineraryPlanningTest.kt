package com.tripnext.app.domain

import com.tripnext.app.data.local.ItineraryEventEntity
import com.tripnext.app.data.local.ItineraryType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

class ItineraryPlanningTest {
    private fun at(hour: Int, minute: Int = 0) = LocalDateTime.of(2027, 1, 10, hour, minute).toInstant(ZoneOffset.UTC).toEpochMilli()
    private fun event(id: String, start: Long, end: Long) = ItineraryEventEntity(id, "trip", id, ItineraryType.ACTIVITY, start, end)

    @Test fun `detecta sobreposicao mas ignora eventos encostados`() {
        val events = listOf(event("a", at(9), at(10, 30)), event("b", at(10), at(11)), event("c", at(11), at(12)))
        assertEquals(setOf("a", "b"), ItineraryPlanning.conflictIds(events))
    }

    @Test fun `reordena somente ids informados`() {
        val events = listOf(event("a", at(9), at(10)), event("b", at(10), at(11)), event("c", at(11), at(12)))
        val result = ItineraryPlanning.reorderForDay(events, listOf("b", "a"))
        assertEquals(1, result.first { it.id == "a" }.sortOrder)
        assertEquals(0, result.first { it.id == "b" }.sortOrder)
        assertEquals(0, result.first { it.id == "c" }.sortOrder)
    }

    @Test fun `classifica blocos do dia`() {
        assertEquals("MANHA", ItineraryPlanning.dayPart(at(8), ZoneOffset.UTC))
        assertEquals("TARDE", ItineraryPlanning.dayPart(at(14), ZoneOffset.UTC))
        assertEquals("NOITE", ItineraryPlanning.dayPart(at(20), ZoneOffset.UTC))
    }
}
