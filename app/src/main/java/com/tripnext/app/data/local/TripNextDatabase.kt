package com.tripnext.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [TripEntity::class, ExpenseEntity::class, CategoryBudgetEntity::class, SavingsGoalEntity::class, ItineraryEventEntity::class, InstallmentReservationEntity::class, ChecklistItemEntity::class, TripVehicleEntity::class, TripParticipantEntity::class, PendingOperationEntity::class], version = 1, exportSchema = true)
@TypeConverters(Converters::class)
abstract class TripNextDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
    abstract fun pendingOperationDao(): PendingOperationDao
}
