package com.tripnext.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class PriceTrendTest {
    @Test fun `reports a relevant price drop`() {
        assertEquals(PriceChange(-5_000, -5.0), PriceTrend.between(100_000, 95_000))
    }

    @Test fun `avoids division by zero for the first free observation`() {
        assertEquals(PriceChange(10_000, 0.0), PriceTrend.between(0, 10_000))
    }
}
