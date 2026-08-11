package com.tripnext.app.domain

data class PriceChange(val differenceMinor: Long, val percent: Double)

object PriceTrend {
    fun between(previousMinor: Long, currentMinor: Long): PriceChange {
        val difference = currentMinor - previousMinor
        val percent = if (previousMinor == 0L) 0.0 else difference * 100.0 / previousMinor
        return PriceChange(difference, percent)
    }
}
