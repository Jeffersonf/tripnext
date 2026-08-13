package com.tripnext.app.data

import com.tripnext.app.data.local.*
import kotlinx.coroutines.flow.Flow

class TripRepository(
    private val dao: TripDao,
    private val pendingDao: PendingOperationDao,
    private val remote: TripRemoteRepository
) {
    private val sessions = (remote as? HttpTripRemoteRepository)
    fun trips() = dao.observeTrips()
    fun archivedTrips() = dao.observeArchivedTrips()
    fun trip(id: String) = dao.observeTrip(id)
    fun expenses(id: String) = dao.observeExpenses(id)
    fun itinerary(id: String) = dao.observeItinerary(id)
    fun ideas(id: String) = dao.observeIdeas(id)
    fun options(id: String) = dao.observeOptions(id)
    fun optionPrices(id: String) = dao.observeOptionPrices(id)
    fun checklist(id: String) = dao.observeChecklist(id)
    fun budgets(id: String) = dao.observeBudgets(id)
    fun spentByCategory(id: String) = dao.observeSpentByCategory(id)

    suspend fun saveExpense(expense: ExpenseEntity) {
        dao.upsertExpense(expense)
        enqueueSnapshot(expense.tripId)
    }
    suspend fun saveTrip(trip: TripEntity) { dao.upsertTrip(trip); enqueueSnapshot(trip.id) }
    suspend fun saveChecklist(item: ChecklistItemEntity) { dao.upsertChecklist(item); enqueueSnapshot(item.tripId) }
    suspend fun saveEvent(event: ItineraryEventEntity) { dao.upsertEvent(event); enqueueSnapshot(event.tripId) }
    suspend fun saveIdea(idea: TripIdeaEntity) { dao.upsertIdea(idea); enqueueSnapshot(idea.tripId) }
    suspend fun deleteIdea(id: String) { val tripId = dao.ideasNow(dao.activeTrip()?.id.orEmpty()).firstOrNull { it.id == id }?.tripId; dao.deleteIdea(id); tripId?.let { enqueueSnapshot(it) } }
    suspend fun saveOption(option: TripOptionEntity) { dao.upsertOption(option); if (dao.optionPriceCount(option.id) == 0) dao.upsertOptionPrice(OptionPriceObservationEntity(tripId = option.tripId, optionId = option.id, priceMinor = option.estimatedCostMinor, currency = option.currency, exchangeRate = option.exchangeRate, observedAt = option.observedAt)); enqueueSnapshot(option.tripId) }
    suspend fun updateOptionPrice(option: TripOptionEntity, priceMinor: Long) { val updated = option.copy(estimatedCostMinor = priceMinor, estimatedMinMinor = minOf(option.estimatedMinMinor, priceMinor), estimatedMaxMinor = maxOf(option.estimatedMaxMinor, priceMinor), observedAt = System.currentTimeMillis()); dao.upsertOption(updated); dao.upsertOptionPrice(OptionPriceObservationEntity(tripId = option.tripId, optionId = option.id, priceMinor = priceMinor, currency = option.currency, exchangeRate = option.exchangeRate, observedAt = updated.observedAt)); enqueueSnapshot(option.tripId) }
    suspend fun chooseOption(option: TripOptionEntity) { dao.chooseOptionInGroup(option.tripId, option.decisionGroup, option.id); enqueueSnapshot(option.tripId) }
    suspend fun deleteOption(id: String) { val active = dao.activeTrip(); dao.deleteOptionPrices(id); dao.deleteOption(id); active?.let { enqueueSnapshot(it.id) } }
    suspend fun saveBudget(budget: CategoryBudgetEntity) { dao.upsertBudget(budget); enqueueSnapshot(budget.tripId) }
    suspend fun toggleChecklist(id: String) { val active = dao.activeTrip(); dao.toggleChecklist(id); active?.let { enqueueSnapshot(it.id) } }
    suspend fun activeTrip() = dao.activeTrip()
    suspend fun activateTrip(id: String) = dao.activateTrip(id)
    suspend fun archiveTrip(id: String) { dao.archiveTrip(id); enqueueSnapshot(id) }
    suspend fun restoreTrip(id: String) { dao.restoreTrip(id); enqueueSnapshot(id) }
    suspend fun deleteTrip(id: String) { if (dao.tripNow(id) == null) return; val version = remote.version(id); dao.deleteTripFully(id); pendingDao.replaceSnapshot(PendingOperationEntity(kind = "DELETE_TRIP", entityId = id, payload = "{}", deduplicationKey = "trip_document:$id", tripId = id, baseVersion = version, deleted = true)) }
    suspend fun deleteTripsNamed(name: String) = dao.deleteTripsNamed(name)
    suspend fun budgetCount(tripId: String) = dao.budgetCount(tripId)

    fun currentSession() = remote.session()
    suspend fun register(apiUrl: String, name: String, email: String, password: String) = remote.register(apiUrl, name, email, password)
    suspend fun login(apiUrl: String, email: String, password: String) = remote.login(apiUrl, email, password)
    suspend fun requestAiPlan(tripId: String) = document(tripId)?.let { remote.plan(tripId, TripDocumentCodec.encode(it)) } ?: error("Viagem não encontrada.")
    suspend fun confirmAiPlan(proposalId: String, selectedItemIds: Set<String>) = remote.applyProposal(proposalId, selectedItemIds)
    fun logout() = remote.logout()
    private suspend fun document(tripId: String): TripDocument? = dao.tripNow(tripId)?.let { TripDocument(it, dao.itineraryNow(tripId), dao.ideasNow(tripId), dao.optionsNow(tripId), dao.checklistNow(tripId), dao.participantsNow(tripId)) }
    private suspend fun enqueueSnapshot(tripId: String) {
        val document = document(tripId) ?: return
        val previous = pendingDao.byDeduplicationKey("trip_document:$tripId")
        pendingDao.replaceSnapshot(PendingOperationEntity(id = previous?.id ?: java.util.UUID.randomUUID().toString(), kind = "UPSERT_TRIP_DOCUMENT", entityId = tripId, payload = TripDocumentCodec.encode(document).toString(), deduplicationKey = "trip_document:$tripId", tripId = tripId, baseVersion = remote.version(tripId)))
    }
    suspend fun sync(): SyncSummary {
        if (remote.session() == null) error("Sessão não configurada.")
        val operations = pendingDao.all()
        operations.filterNot { it.deleted }.forEach { remote.ensureTrip(it) }
        val results = remote.push(operations)
        val conflicts = results.filter { it.status == "conflict" }
        results.filter { it.status == "applied" || it.duplicate }.forEach { result -> operations.firstOrNull { it.id == result.mutationId }?.let { operation -> pendingDao.delete(operation.id); remote.saveVersion(operation.tripId, result.version) } }
        val pulled = remote.pull(remote.cursor())
        pulled.changes.filter { it.entityType == "trip_document" }.forEach { change ->
            remote.saveVersion(change.tripId, change.version)
            if (change.deleted) dao.deleteTripFully(change.tripId)
            else if (pendingDao.byDeduplicationKey("trip_document:${change.tripId}") == null) change.payload?.let { dao.replaceFromRemote(TripDocumentCodec.decode(it)) }
        }
        remote.saveCursor(pulled.cursor)
        return SyncSummary(results.count { it.status == "applied" || it.duplicate }, pulled.changes.size, conflicts)
    }

    suspend fun resolveConflicts(conflicts: List<PushResult>, keepLocal: Boolean) {
        val operations = pendingDao.all().associateBy { it.id }
        conflicts.forEach { conflict ->
            val operation = operations[conflict.mutationId] ?: return@forEach
            if (keepLocal) {
                pendingDao.replaceSnapshot(operation.copy(id = java.util.UUID.randomUUID().toString(), baseVersion = conflict.currentVersion, createdAt = System.currentTimeMillis()))
            } else {
                conflict.currentPayload?.let { dao.replaceFromRemote(TripDocumentCodec.decode(it)) }
                pendingDao.delete(operation.id)
            }
            remote.saveVersion(operation.tripId, conflict.currentVersion)
        }
    }
}

data class SyncSummary(val pushed: Int, val pulled: Int, val conflicts: List<PushResult>)
