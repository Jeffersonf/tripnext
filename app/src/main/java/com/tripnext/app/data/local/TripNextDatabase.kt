package com.tripnext.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [TripEntity::class, ExpenseEntity::class, CategoryBudgetEntity::class, SavingsGoalEntity::class, ItineraryEventEntity::class, TripIdeaEntity::class, TripOptionEntity::class, InstallmentReservationEntity::class, ChecklistItemEntity::class, TripVehicleEntity::class, TripParticipantEntity::class, PendingOperationEntity::class], version = 5, exportSchema = true)
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
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE itinerary_events ADD COLUMN latitude REAL")
                db.execSQL("ALTER TABLE itinerary_events ADD COLUMN longitude REAL")
                db.execSQL("ALTER TABLE itinerary_events ADD COLUMN placeId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE trip_ideas ADD COLUMN latitude REAL")
                db.execSQL("ALTER TABLE trip_ideas ADD COLUMN longitude REAL")
                db.execSQL("ALTER TABLE trip_ideas ADD COLUMN placeId TEXT NOT NULL DEFAULT ''")
            }
        }
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS trip_options (id TEXT NOT NULL, tripId TEXT NOT NULL, decisionGroup TEXT NOT NULL, title TEXT NOT NULL, type TEXT NOT NULL, provider TEXT NOT NULL, location TEXT NOT NULL, estimatedCostMinor INTEGER NOT NULL, currency TEXT NOT NULL, cancellationPolicy TEXT NOT NULL, inclusions TEXT NOT NULL, pros TEXT NOT NULL, cons TEXT NOT NULL, sourceUrl TEXT NOT NULL, chosen INTEGER NOT NULL, observedAt INTEGER NOT NULL, PRIMARY KEY(id))")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_trip_options_tripId ON trip_options(tripId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_trip_options_tripId_decisionGroup ON trip_options(tripId, decisionGroup)")
            }
        }
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE trip_options ADD COLUMN origin TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE trip_options ADD COLUMN destination TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE trip_options ADD COLUMN departsAt INTEGER")
                db.execSQL("ALTER TABLE trip_options ADD COLUMN arrivesAt INTEGER")
                db.execSQL("ALTER TABLE trip_options ADD COLUMN stopCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE trip_options ADD COLUMN roomType TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE trip_options ADD COLUMN nightCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE trip_options ADD COLUMN durationMinutes INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
