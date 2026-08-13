package com.tripnext.app

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tripnext.app.data.*
import com.tripnext.app.data.local.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class AndroidSyncContractTest {
    @Test
    fun androidDocumentRoundTripsThroughTheRealHttpContract() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.getSharedPreferences("tripnext_session", Context.MODE_PRIVATE).edit().clear().commit()
        val database = Room.inMemoryDatabaseBuilder(context, TripNextDatabase::class.java).allowMainThreadQueries().build()
        try {
            val remote = HttpTripRemoteRepository(SessionStore(context))
            remote.register("http://127.0.0.1:8787", "Android Test", "android-${UUID.randomUUID()}@example.com", "senha-segura-123")
            val repository = TripRepository(database.tripDao(), database.pendingOperationDao(), remote)
            val trip = TripEntity(id = "android-${UUID.randomUUID()}", name = "Plano Android", destination = "Recife", startDate = 1_830_297_600_000, endDate = 1_830_556_800_000, totalBudgetMinor = 400_000, origin = "São Paulo", children = 1, childAges = "8", interests = "praias, cultura", pace = "LIGHT", dietaryRestrictions = "sem lactose", maxWalkingMinutes = 20)
            repository.saveTrip(trip)
            repository.saveEvent(ItineraryEventEntity(tripId = trip.id, title = "Marco Zero", type = ItineraryType.ACTIVITY, startsAt = trip.startDate + 36_000_000, location = "Recife Antigo"))
            repository.saveChecklist(ChecklistItemEntity(tripId = trip.id, name = "Separar protetor solar"))
            repository.saveBudget(CategoryBudgetEntity(trip.id, ExpenseCategory.FOOD, 80_000))
            val summary = repository.sync()
            assertTrue(summary.pushed >= 1)
            val pulled = remote.pull(0).changes.filter { it.entityType == "trip_document" }
            val document = TripDocumentCodec.decode(pulled.last { it.tripId == trip.id }.payload!!)
            assertEquals("Plano Android", document.trip.name)
            assertEquals("Marco Zero", document.itinerary.single().title)
            assertEquals("Separar protetor solar", document.checklist.single().name)
            assertEquals(80_000, document.budgets.single().limitMinor)
            assertEquals("São Paulo", document.trip.origin)
            assertEquals("praias, cultura", document.trip.interests)
            assertEquals(20, document.trip.maxWalkingMinutes)
        } finally { database.close() }
    }
}
