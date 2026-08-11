package com.tripnext.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class PlannedCostCalculatorTest {
    @Test fun `converts range and multiplies per-person cost`() {
        assertEquals(
            PlannedCostRange(80_000, 100_000, 130_000),
            PlannedCostCalculator.range(8_000, 10_000, 13_000, 5.0, true, 2)
        )
    }

    @Test fun `adds configured contingency to expected total`() {
        assertEquals(115_000, PlannedCostCalculator.withContingency(100_000, 15))
    }
}
