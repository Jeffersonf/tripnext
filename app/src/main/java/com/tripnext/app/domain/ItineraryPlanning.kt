package com.tripnext.app.domain

import com.tripnext.app.data.local.ItineraryEventEntity
import java.time.Instant
import java.time.ZoneId

object ItineraryPlanning {
    fun conflictIds(events: List<ItineraryEventEntity>): Set<String> {
        val result = mutableSetOf<String>()
        events.forEachIndexed { index, first ->
            val firstEnd = first.endsAt ?: return@forEachIndexed
            events.drop(index + 1).forEach { second ->
                val secondEnd = second.endsAt ?: return@forEach
                if (sameLocalDay(first.startsAt, second.startsAt) && first.startsAt < secondEnd && second.startsAt < firstEnd) {
                    result += first.id
                    result += second.id
                }
            }
        }
        return result
    }

    fun reorderForDay(events: List<ItineraryEventEntity>, orderedIds: List<String>): List<ItineraryEventEntity> {
        val order = orderedIds.withIndex().associate { it.value to it.index }
        return events.map { event -> order[event.id]?.let { event.copy(sortOrder = it) } ?: event }
    }

    fun dayPart(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String =
        when (Instant.ofEpochMilli(epochMillis).atZone(zoneId).hour) {
            in 0..11 -> "MANHA"
            in 12..17 -> "TARDE"
            else -> "NOITE"
        }

    private fun sameLocalDay(first: Long, second: Long, zoneId: ZoneId = ZoneId.systemDefault()) =
        Instant.ofEpochMilli(first).atZone(zoneId).toLocalDate() == Instant.ofEpochMilli(second).atZone(zoneId).toLocalDate()
}
