package com.tripnext.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [TripEntity::class, ExpenseEntity::class, CategoryBudgetEntity::class, SavingsGoalEntity::class, ItineraryEventEntity::class, TripIdeaEntity::class, InstallmentReservationEntity::class, ChecklistItemEntity::class, TripVehicleEntity::class, TripParticipantEntity::class, PendingOperationEntity::class], version = 2, exportSchema = true)
@TypeConverters(Converters::class)
abstract class TripNextDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
    abstract fun pendingOperationDao(): PendingOperationDao
    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE trips ADD COLUMN archived INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE trips ADD COLUMN travelers INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE itinerary_events ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE itinerary_events ADD COLUMN planningStatus TEXT NOT NULL DEFAULT 'RESEARCHING'")
                db.execSQL("ALTER TABLE itinerary_events ADD COLUMN estimatedCostMinor INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE itinerary_events ADD COLUMN bookingCode TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE itinerary_events ADD COLUMN sourceUrl TEXT NOT NULL DEFAULT ''")
                db.execSQL("CREATE TABLE IF NOT EXISTS trip_ideas (id TEXT NOT NULL, tripId TEXT NOT NULL, title TEXT NOT NULL, type TEXT NOT NULL, location TEXT NOT NULL, notes TEXT NOT NULL, estimatedCostMinor INTEGER NOT NULL, sourceUrl TEXT NOT NULL, createdAt INTEGER NOT NULL, PRIMARY KEY(id))")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_trip_ideas_tripId ON trip_ideas(tripId)")
            }
        }
    }
}
