package com.tripnext.app.domain

import kotlin.math.roundToLong

data class PlannedCostRange(val minimumMinor: Long, val expectedMinor: Long, val maximumMinor: Long)

object PlannedCostCalculator {
    fun range(
        minimumMinor: Long,
        expectedMinor: Long,
        maximumMinor: Long,
        exchangeRate: Double,
        perPerson: Boolean,
        travelers: Int
    ): PlannedCostRange {
        val multiplier = exchangeRate.coerceAtLeast(0.0) * if (perPerson) travelers.coerceAtLeast(1) else 1
        return PlannedCostRange(
            (minimumMinor * multiplier).roundToLong(),
            (expectedMinor * multiplier).roundToLong(),
            (maximumMinor * multiplier).roundToLong()
        )
    }

    fun withContingency(expectedMinor: Long, contingencyPercent: Int): Long =
        (expectedMinor * (1 + contingencyPercent.coerceAtLeast(0) / 100.0)).roundToLong()
}
