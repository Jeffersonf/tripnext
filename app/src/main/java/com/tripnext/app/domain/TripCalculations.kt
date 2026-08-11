package com.tripnext.app.domain

import java.time.LocalDate

data class ParticipantShare(val participantId: String, val shareMinor: Long)
data class ParticipantBalance(val participantId: String, val balanceMinor: Long)
data class Settlement(val fromParticipantId: String, val toParticipantId: String, val amountMinor: Long)

object TripCalculations {
    fun remainingBudget(totalMinor: Long, expensesMinor: Iterable<Long>) = totalMinor - expensesMinor.sum()

    fun equalShares(totalMinor: Long, participantIds: List<String>): List<ParticipantShare> {
        require(participantIds.isNotEmpty())
        val base = totalMinor / participantIds.size
        val remainder = totalMinor % participantIds.size
        return participantIds.mapIndexed { index, id -> ParticipantShare(id, base + if (index < remainder) 1 else 0) }
    }

    fun balances(paidBy: Map<String, Long>, owedBy: Map<String, Long>): List<ParticipantBalance> =
        (paidBy.keys + owedBy.keys).distinct().map { ParticipantBalance(it, paidBy.getOrDefault(it, 0) - owedBy.getOrDefault(it, 0)) }

    fun settlements(balances: List<ParticipantBalance>): List<Settlement> {
        val debtors = balances.filter { it.balanceMinor < 0 }.map { it.participantId to -it.balanceMinor }.toMutableList()
        val creditors = balances.filter { it.balanceMinor > 0 }.map { it.participantId to it.balanceMinor }.toMutableList()
        val result = mutableListOf<Settlement>(); var d = 0; var c = 0
        while (d < debtors.size && c < creditors.size) {
            val amount = minOf(debtors[d].second, creditors[c].second)
            result += Settlement(debtors[d].first, creditors[c].first, amount)
            debtors[d] = debtors[d].copy(second = debtors[d].second - amount)
            creditors[c] = creditors[c].copy(second = creditors[c].second - amount)
            if (debtors[d].second == 0L) d++
            if (creditors[c].second == 0L) c++
        }
        return result
    }

    fun installmentAmounts(totalMinor: Long, count: Int): List<Long> {
        require(totalMinor >= 0 && count > 0)
        val base = totalMinor / count; val remainder = (totalMinor % count).toInt()
        return List(count) { base + if (it < remainder) 1 else 0 }
    }

    fun installmentDueDates(first: LocalDate, count: Int, intervalMonths: Long = 1): List<LocalDate> {
        require(count > 0 && intervalMonths > 0)
        return List(count) { first.plusMonths(it * intervalMonths) }
    }
}
