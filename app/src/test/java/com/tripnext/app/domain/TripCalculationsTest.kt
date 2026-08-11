package com.tripnext.app.domain

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class TripCalculationsTest {
    @Test fun `remaining budget subtracts all expenses`() { assertEquals(4_750L, TripCalculations.remainingBudget(10_000, listOf(2_000, 3_250))) }
    @Test fun `remaining budget can signal overspending`() { assertEquals(-500L, TripCalculations.remainingBudget(1_000, listOf(1_500))) }

    @Test fun `equal split preserves every cent`() {
        val shares = TripCalculations.equalShares(100, listOf("ana", "bia", "caio"))
        assertEquals(listOf(34L, 33L, 33L), shares.map { it.shareMinor }); assertEquals(100L, shares.sumOf { it.shareMinor })
    }

    @Test fun `participant balances produce minimal settlement`() {
        val balances = TripCalculations.balances(mapOf("ana" to 9_000L), mapOf("ana" to 3_000L, "bia" to 3_000L, "caio" to 3_000L))
        assertEquals(setOf(Settlement("bia", "ana", 3_000), Settlement("caio", "ana", 3_000)), TripCalculations.settlements(balances).toSet())
    }

    @Test fun `installments distribute remainder and keep total`() {
        val installments = TripCalculations.installmentAmounts(10_000, 3)
        assertEquals(listOf(3_334L, 3_333L, 3_333L), installments); assertEquals(10_000L, installments.sum())
    }

    @Test fun `installment due dates advance by calendar month`() {
        assertEquals(listOf(LocalDate.of(2026, 1, 31), LocalDate.of(2026, 2, 28), LocalDate.of(2026, 3, 31)), TripCalculations.installmentDueDates(LocalDate.of(2026, 1, 31), 3))
    }

    @Test(expected = IllegalArgumentException::class) fun `installments reject zero count`() { TripCalculations.installmentAmounts(100, 0) }
}
