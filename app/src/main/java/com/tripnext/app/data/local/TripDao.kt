package com.tripnext.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

data class CategorySpent(val category: ExpenseCategory, val amountMinor: Long)

@Dao
interface TripDao {
    @Query("SELECT * FROM trips WHERE archived = 0 ORDER BY isActive DESC, startDate") fun observeTrips(): Flow<List<TripEntity>>
    @Query("SELECT * FROM trips WHERE archived = 1 ORDER BY startDate DESC") fun observeArchivedTrips(): Flow<List<TripEntity>>
    @Query("SELECT * FROM trips WHERE id = :id") fun observeTrip(id: String): Flow<TripEntity?>
    @Query("SELECT * FROM expenses WHERE tripId = :tripId ORDER BY date DESC") fun observeExpenses(tripId: String): Flow<List<ExpenseEntity>>
    @Query("SELECT * FROM itinerary_events WHERE tripId = :tripId ORDER BY startsAt, sortOrder") fun observeItinerary(tripId: String): Flow<List<ItineraryEventEntity>>
    @Query("SELECT * FROM trip_ideas WHERE tripId = :tripId ORDER BY createdAt DESC") fun observeIdeas(tripId: String): Flow<List<TripIdeaEntity>>
    @Query("SELECT * FROM trip_options WHERE tripId = :tripId ORDER BY decisionGroup, estimatedCostMinor") fun observeOptions(tripId: String): Flow<List<TripOptionEntity>>
    @Query("SELECT * FROM option_price_observations WHERE optionId = :optionId ORDER BY observedAt") fun observeOptionPrices(optionId: String): Flow<List<OptionPriceObservationEntity>>
    @Query("SELECT * FROM checklist_items WHERE tripId = :tripId ORDER BY checked, category, name") fun observeChecklist(tripId: String): Flow<List<ChecklistItemEntity>>
    @Query("SELECT * FROM category_budgets WHERE tripId = :tripId") fun observeBudgets(tripId: String): Flow<List<CategoryBudgetEntity>>
    @Query("SELECT category, SUM(amountMinor) AS amountMinor FROM expenses WHERE tripId = :tripId GROUP BY category") fun observeSpentByCategory(tripId: String): Flow<List<CategorySpent>>
    @Upsert suspend fun upsertTrip(value: TripEntity)
    @Upsert suspend fun upsertExpense(value: ExpenseEntity)
    @Upsert suspend fun upsertChecklist(value: ChecklistItemEntity)
    @Upsert suspend fun upsertEvent(value: ItineraryEventEntity)
    @Upsert suspend fun upsertIdea(value: TripIdeaEntity)
    @Upsert suspend fun upsertOption(value: TripOptionEntity)
    @Upsert suspend fun upsertOptionPrice(value: OptionPriceObservationEntity)
    @Upsert suspend fun upsertEvents(values: List<ItineraryEventEntity>)
    @Upsert suspend fun upsertIdeas(values: List<TripIdeaEntity>)
    @Upsert suspend fun upsertOptions(values: List<TripOptionEntity>)
    @Upsert suspend fun upsertChecklistItems(values: List<ChecklistItemEntity>)
    @Upsert suspend fun upsertParticipants(values: List<TripParticipantEntity>)
    @Query("SELECT COUNT(*) FROM option_price_observations WHERE optionId = :optionId") suspend fun optionPriceCount(optionId: String): Int
    @Query("UPDATE trip_options SET chosen = CASE WHEN id = :optionId THEN 1 ELSE 0 END WHERE tripId = :tripId AND decisionGroup = :decisionGroup") suspend fun chooseOptionInGroup(tripId: String, decisionGroup: String, optionId: String)
    @Query("DELETE FROM trip_options WHERE id = :id") suspend fun deleteOption(id: String)
    @Query("DELETE FROM option_price_observations WHERE optionId = :optionId") suspend fun deleteOptionPrices(optionId: String)
    @Upsert suspend fun upsertBudget(value: CategoryBudgetEntity)
    @Query("UPDATE checklist_items SET checked = NOT checked WHERE id = :id") suspend fun toggleChecklist(id: String)
    @Query("SELECT * FROM trips WHERE isActive = 1 LIMIT 1") suspend fun activeTrip(): TripEntity?
    @Query("SELECT * FROM trips WHERE id = :id LIMIT 1") suspend fun tripNow(id: String): TripEntity?
    @Query("SELECT * FROM itinerary_events WHERE tripId = :id ORDER BY startsAt, sortOrder") suspend fun itineraryNow(id: String): List<ItineraryEventEntity>
    @Query("SELECT * FROM trip_ideas WHERE tripId = :id ORDER BY createdAt") suspend fun ideasNow(id: String): List<TripIdeaEntity>
    @Query("SELECT * FROM trip_options WHERE tripId = :id ORDER BY decisionGroup, estimatedCostMinor") suspend fun optionsNow(id: String): List<TripOptionEntity>
    @Query("SELECT * FROM checklist_items WHERE tripId = :id ORDER BY category, name") suspend fun checklistNow(id: String): List<ChecklistItemEntity>
    @Query("SELECT * FROM trip_participants WHERE tripId = :id ORDER BY name") suspend fun participantsNow(id: String): List<TripParticipantEntity>
    @Query("SELECT * FROM category_budgets WHERE tripId = :id ORDER BY category") suspend fun budgetsNow(id: String): List<CategoryBudgetEntity>
    @Query("SELECT COUNT(*) FROM category_budgets WHERE tripId = :tripId") suspend fun budgetCount(tripId: String): Int
    @Query("UPDATE trips SET isActive = CASE WHEN id = :id THEN 1 ELSE 0 END") suspend fun activateTrip(id: String)
    @Query("UPDATE trips SET archived = 1, isActive = 0 WHERE id = :id") suspend fun archiveTrip(id: String)
    @Query("UPDATE trips SET archived = 0 WHERE id = :id") suspend fun restoreTrip(id: String)
    @Query("DELETE FROM trip_ideas WHERE id = :id") suspend fun deleteIdea(id: String)
    @Query("DELETE FROM itinerary_events WHERE id = :id") suspend fun deleteEvent(id: String)
    @Query("DELETE FROM checklist_items WHERE id = :id") suspend fun deleteChecklistItem(id: String)
    @Query("DELETE FROM trip_ideas WHERE tripId = :id") suspend fun deleteIdeasForTrip(id: String)
    @Query("DELETE FROM trip_options WHERE tripId = :id") suspend fun deleteOptionsForTrip(id: String)
    @Query("DELETE FROM option_price_observations WHERE tripId = :id") suspend fun deleteOptionPricesForTrip(id: String)
    @Query("DELETE FROM expenses WHERE tripId = :id") suspend fun deleteExpensesForTrip(id: String)
    @Query("DELETE FROM itinerary_events WHERE tripId = :id") suspend fun deleteEventsForTrip(id: String)
    @Query("DELETE FROM checklist_items WHERE tripId = :id") suspend fun deleteChecklistForTrip(id: String)
    @Query("DELETE FROM category_budgets WHERE tripId = :id") suspend fun deleteBudgetsForTrip(id: String)
    @Query("DELETE FROM savings_goals WHERE tripId = :id") suspend fun deleteGoalsForTrip(id: String)
    @Query("DELETE FROM installment_reservations WHERE tripId = :id") suspend fun deleteReservationsForTrip(id: String)
    @Query("DELETE FROM trip_vehicles WHERE tripId = :id") suspend fun deleteVehiclesForTrip(id: String)
    @Query("DELETE FROM trip_participants WHERE tripId = :id") suspend fun deleteParticipantsForTrip(id: String)
    @Query("DELETE FROM trips WHERE id = :id") suspend fun deleteTripRow(id: String)
    @Transaction suspend fun replaceFromRemote(document: com.tripnext.app.data.TripDocument) {
        val id = document.trip.id
        upsertTrip(document.trip); deleteEventsForTrip(id); deleteIdeasForTrip(id); deleteOptionPricesForTrip(id); deleteOptionsForTrip(id); deleteChecklistForTrip(id); deleteParticipantsForTrip(id); deleteBudgetsForTrip(id)
        upsertEvents(document.itinerary); upsertIdeas(document.ideas); upsertOptions(document.options); upsertChecklistItems(document.checklist); upsertParticipants(document.participants); document.budgets.forEach { upsertBudget(it) }
    }
    @Transaction suspend fun deleteTripFully(id: String) { deleteExpensesForTrip(id); deleteEventsForTrip(id); deleteIdeasForTrip(id); deleteOptionPricesForTrip(id); deleteOptionsForTrip(id); deleteChecklistForTrip(id); deleteBudgetsForTrip(id); deleteGoalsForTrip(id); deleteReservationsForTrip(id); deleteVehiclesForTrip(id); deleteParticipantsForTrip(id); deleteTripRow(id) }
    @Query("SELECT id FROM trips WHERE name = :name") suspend fun tripIdsNamed(name: String): List<String>
    @Transaction suspend fun deleteTripsNamed(name: String) { tripIdsNamed(name).forEach { deleteTripFully(it) } }
}

@Dao
interface PendingOperationDao {
    @Query("SELECT * FROM pending_operations ORDER BY createdAt") suspend fun all(): List<PendingOperationEntity>
    @Upsert suspend fun upsert(value: PendingOperationEntity)
    @Query("SELECT * FROM pending_operations WHERE deduplicationKey = :key LIMIT 1") suspend fun byDeduplicationKey(key: String): PendingOperationEntity?
    @Query("DELETE FROM pending_operations WHERE deduplicationKey = :key") suspend fun deleteByDeduplicationKey(key: String)
    @Query("DELETE FROM pending_operations WHERE id = :id") suspend fun delete(id: String)
    @Transaction suspend fun replaceSnapshot(value: PendingOperationEntity) { deleteByDeduplicationKey(value.deduplicationKey); upsert(value) }
}
