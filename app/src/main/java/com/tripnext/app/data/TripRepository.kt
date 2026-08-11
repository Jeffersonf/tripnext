package com.tripnext.app.data

import com.tripnext.app.data.local.*
import kotlinx.coroutines.flow.Flow

class TripRepository(
    private val dao: TripDao,
    private val pendingDao: PendingOperationDao,
    private val remote: TripRemoteRepository
) {
    fun trips() = dao.observeTrips()
    fun archivedTrips() = dao.observeArchivedTrips()
    fun trip(id: String) = dao.observeTrip(id)
    fun expenses(id: String) = dao.observeExpenses(id)
    fun itinerary(id: String) = dao.observeItinerary(id)
    fun ideas(id: String) = dao.observeIdeas(id)
    fun options(id: String) = dao.observeOptions(id)
    fun checklist(id: String) = dao.observeChecklist(id)
    fun budgets(id: String) = dao.observeBudgets(id)
    fun spentByCategory(id: String) = dao.observeSpentByCategory(id)

    suspend fun saveExpense(expense: ExpenseEntity) {
        dao.upsertExpense(expense)
        enqueue("UPSERT_EXPENSE", expense.id, "{\"id\":\"${expense.id}\"}")
    }
    suspend fun saveTrip(trip: TripEntity) { dao.upsertTrip(trip); enqueue("UPSERT_TRIP", trip.id, "{\"id\":\"${trip.id}\"}") }
    suspend fun saveChecklist(item: ChecklistItemEntity) { dao.upsertChecklist(item); enqueue("UPSERT_CHECKLIST", item.id, "{\"id\":\"${item.id}\"}") }
    suspend fun saveEvent(event: ItineraryEventEntity) { dao.upsertEvent(event); enqueue("UPSERT_EVENT", event.id, "{\"id\":\"${event.id}\"}") }
    suspend fun saveIdea(idea: TripIdeaEntity) { dao.upsertIdea(idea); enqueue("UPSERT_IDEA", idea.id, "{\"id\":\"${idea.id}\"}") }
    suspend fun deleteIdea(id: String) { dao.deleteIdea(id); enqueue("DELETE_IDEA", id, "{\"id\":\"$id\"}") }
    suspend fun saveOption(option: TripOptionEntity) { dao.upsertOption(option); enqueue("UPSERT_OPTION", option.id, "{\"id\":\"${option.id}\"}") }
    suspend fun chooseOption(option: TripOptionEntity) { dao.chooseOptionInGroup(option.tripId, option.decisionGroup, option.id); enqueue("CHOOSE_OPTION", option.id, "{\"id\":\"${option.id}\"}") }
    suspend fun deleteOption(id: String) { dao.deleteOption(id); enqueue("DELETE_OPTION", id, "{\"id\":\"$id\"}") }
    suspend fun saveBudget(budget: CategoryBudgetEntity) { dao.upsertBudget(budget); enqueue("UPSERT_BUDGET", "${budget.tripId}:${budget.category}", "{}") }
    suspend fun toggleChecklist(id: String) { dao.toggleChecklist(id); enqueue("TOGGLE_CHECKLIST", id, "{\"id\":\"$id\"}") }
    suspend fun activeTrip() = dao.activeTrip()
    suspend fun activateTrip(id: String) = dao.activateTrip(id)
    suspend fun archiveTrip(id: String) = dao.archiveTrip(id)
    suspend fun restoreTrip(id: String) = dao.restoreTrip(id)
    suspend fun deleteTrip(id: String) = dao.deleteTripFully(id)
    suspend fun deleteTripsNamed(name: String) = dao.deleteTripsNamed(name)
    suspend fun budgetCount(tripId: String) = dao.budgetCount(tripId)

    private suspend fun enqueue(kind: String, entityId: String, payload: String) {
        pendingDao.upsert(PendingOperationEntity(kind = kind, entityId = entityId, payload = payload, deduplicationKey = "$kind:$entityId"))
    }
    suspend fun sync(): Int {
        var synced = 0
        for (operation in pendingDao.all()) {
            if (remote.push(operation).isSuccess) { pendingDao.delete(operation.id); synced++ } else break
        }
        return synced
    }
}
