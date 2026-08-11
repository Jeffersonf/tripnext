package com.tripnext.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class ExpenseCategory { ACCOMMODATION, TRANSPORT, FOOD, ACTIVITIES, INSURANCE, GIFTS, DOCUMENTS, UNEXPECTED, SHOPPING, OTHER }
enum class ChecklistCategory { DOCUMENTS, CLOTHES, ELECTRONICS, HYGIENE, MEDICINES, OTHER }
enum class ItineraryType { FLIGHT, CHECK_IN, CHECK_OUT, ACTIVITY, RESTAURANT, TRANSFER, OTHER }
enum class ParticipantRole { ORGANIZER, EDITOR, VIEWER, GUEST }
enum class ReservationType { FLIGHT, ACCOMMODATION, CAR_RENTAL, OTHER }
enum class SplitType { EQUAL, CUSTOM }

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val destination: String,
    val startDate: Long,
    val endDate: Long,
    val totalBudgetMinor: Long,
    val currency: String = "BRL",
    val isActive: Boolean = false,
    val archived: Boolean = false,
    val travelers: Int = 1,
    val updatedAt: Long = System.currentTimeMillis(),
    val contingencyPercent: Int = 0
)

@Entity(tableName = "expenses", indices = [Index("tripId")])
data class ExpenseEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val tripId: String,
    val amountMinor: Long,
    val category: ExpenseCategory,
    val date: Long,
    val description: String,
    val paymentMethod: String = "",
    val installmentCount: Int = 1,
    val paidByParticipantId: String? = null,
    val splitType: SplitType = SplitType.EQUAL,
    val approved: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "category_budgets", primaryKeys = ["tripId", "category"])
data class CategoryBudgetEntity(val tripId: String, val category: ExpenseCategory, val limitMinor: Long)

@Entity(tableName = "savings_goals", indices = [Index("tripId")])
data class SavingsGoalEntity(@PrimaryKey val id: String = UUID.randomUUID().toString(), val tripId: String, val targetMinor: Long, val savedMinor: Long = 0, val targetDate: Long)

@Entity(tableName = "itinerary_events", indices = [Index("tripId")])
data class ItineraryEventEntity(@PrimaryKey val id: String = UUID.randomUUID().toString(), val tripId: String, val title: String, val type: ItineraryType, val startsAt: Long, val endsAt: Long? = null, val location: String = "", val notes: String = "", val sortOrder: Int = 0, val planningStatus: String = "RESEARCHING", val estimatedCostMinor: Long = 0, val bookingCode: String = "", val sourceUrl: String = "", val latitude: Double? = null, val longitude: Double? = null, val placeId: String = "", val estimatedMinMinor: Long = estimatedCostMinor, val estimatedMaxMinor: Long = estimatedCostMinor, val costCurrency: String = "BRL", val exchangeRate: Double = 1.0, val quoteDate: Long? = null, val costScope: String = "GROUP", val costClass: String = "DAILY", val city: String = "")

@Entity(tableName = "trip_ideas", indices = [Index("tripId")])
data class TripIdeaEntity(@PrimaryKey val id: String = UUID.randomUUID().toString(), val tripId: String, val title: String, val type: ItineraryType = ItineraryType.ACTIVITY, val location: String = "", val notes: String = "", val estimatedCostMinor: Long = 0, val sourceUrl: String = "", val createdAt: Long = System.currentTimeMillis(), val latitude: Double? = null, val longitude: Double? = null, val placeId: String = "")

@Entity(tableName = "trip_options", indices = [Index("tripId"), Index(value = ["tripId", "decisionGroup"])])
data class TripOptionEntity(@PrimaryKey val id: String = UUID.randomUUID().toString(), val tripId: String, val decisionGroup: String, val title: String, val type: ItineraryType, val provider: String = "", val location: String = "", val estimatedCostMinor: Long = 0, val currency: String = "BRL", val cancellationPolicy: String = "", val inclusions: String = "", val pros: String = "", val cons: String = "", val sourceUrl: String = "", val chosen: Boolean = false, val observedAt: Long = System.currentTimeMillis(), val origin: String = "", val destination: String = "", val departsAt: Long? = null, val arrivesAt: Long? = null, val stopCount: Int = 0, val roomType: String = "", val nightCount: Int = 0, val durationMinutes: Int = 0, val estimatedMinMinor: Long = estimatedCostMinor, val estimatedMaxMinor: Long = estimatedCostMinor, val exchangeRate: Double = 1.0, val quoteDate: Long? = null, val costScope: String = "GROUP", val costClass: String = "FIXED")

@Entity(tableName = "installment_reservations", indices = [Index("tripId")])
data class InstallmentReservationEntity(@PrimaryKey val id: String = UUID.randomUUID().toString(), val tripId: String, val title: String, val type: ReservationType, val totalMinor: Long, val installmentCount: Int, val firstDueDate: Long, val intervalMonths: Int = 1)

@Entity(tableName = "checklist_items", indices = [Index("tripId")])
data class ChecklistItemEntity(@PrimaryKey val id: String = UUID.randomUUID().toString(), val tripId: String, val name: String, val quantity: Int = 1, val category: ChecklistCategory = ChecklistCategory.OTHER, val checked: Boolean = false)

@Entity(tableName = "trip_vehicles", indices = [Index("tripId")])
data class TripVehicleEntity(@PrimaryKey val id: String = UUID.randomUUID().toString(), val tripId: String, val description: String, val rented: Boolean, val initialOdometerKm: Double = 0.0, val currentOdometerKm: Double = 0.0, val fuelCostMinor: Long = 0, val maintenanceCostMinor: Long = 0)

@Entity(tableName = "trip_participants", indices = [Index("tripId"), Index(value = ["tripId", "email"], unique = true)])
data class TripParticipantEntity(@PrimaryKey val id: String = UUID.randomUUID().toString(), val tripId: String, val name: String, val email: String, val role: ParticipantRole = ParticipantRole.GUEST)

@Entity(tableName = "pending_operations", indices = [Index(value = ["deduplicationKey"], unique = true)])
data class PendingOperationEntity(@PrimaryKey val id: String = UUID.randomUUID().toString(), val kind: String, val entityId: String, val payload: String, val deduplicationKey: String, val createdAt: Long = System.currentTimeMillis(), val attempts: Int = 0)
