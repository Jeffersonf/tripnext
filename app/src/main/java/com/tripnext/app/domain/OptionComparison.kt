package com.tripnext.app.domain

import com.tripnext.app.data.local.TripOptionEntity

object OptionComparison {
    fun choose(options: List<TripOptionEntity>, optionId: String): List<TripOptionEntity> {
        val selected = options.firstOrNull { it.id == optionId } ?: return options
        return options.map { if (it.tripId == selected.tripId && it.decisionGroup == selected.decisionGroup) it.copy(chosen = it.id == optionId) else it }
    }

    fun cheapestId(options: List<TripOptionEntity>): String? = options.minByOrNull { it.estimatedCostMinor }?.id
}
