package com.tripnext.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [TripEntity::class, ExpenseEntity::class, CategoryBudgetEntity::class, SavingsGoalEntity::class, ItineraryEventEntity::class, TripIdeaEntity::class, TripOptionEntity::class, OptionPriceObservationEntity::class, InstallmentReservationEntity::class, ChecklistItemEntity::class, TripVehicleEntity::class, TripParticipantEntity::class, PendingOperationEntity::class], version = 8, exportSchema = true)
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
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE trips ADD COLUMN contingencyPercent INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE itinerary_events ADD COLUMN estimatedMinMinor INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE itinerary_events ADD COLUMN estimatedMaxMinor INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE itinerary_events ADD COLUMN costCurrency TEXT NOT NULL DEFAULT 'BRL'")
                db.execSQL("ALTER TABLE itinerary_events ADD COLUMN exchangeRate REAL NOT NULL DEFAULT 1.0")
                db.execSQL("ALTER TABLE itinerary_events ADD COLUMN quoteDate INTEGER")
                db.execSQL("ALTER TABLE itinerary_events ADD COLUMN costScope TEXT NOT NULL DEFAULT 'GROUP'")
                db.execSQL("ALTER TABLE itinerary_events ADD COLUMN costClass TEXT NOT NULL DEFAULT 'DAILY'")
                db.execSQL("ALTER TABLE itinerary_events ADD COLUMN city TEXT NOT NULL DEFAULT ''")
                db.execSQL("UPDATE itinerary_events SET estimatedMinMinor = estimatedCostMinor, estimatedMaxMinor = estimatedCostMinor")
                db.execSQL("ALTER TABLE trip_options ADD COLUMN estimatedMinMinor INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE trip_options ADD COLUMN estimatedMaxMinor INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE trip_options ADD COLUMN exchangeRate REAL NOT NULL DEFAULT 1.0")
                db.execSQL("ALTER TABLE trip_options ADD COLUMN quoteDate INTEGER")
                db.execSQL("ALTER TABLE trip_options ADD COLUMN costScope TEXT NOT NULL DEFAULT 'GROUP'")
                db.execSQL("ALTER TABLE trip_options ADD COLUMN costClass TEXT NOT NULL DEFAULT 'FIXED'")
                db.execSQL("UPDATE trip_options SET estimatedMinMinor = estimatedCostMinor, estimatedMaxMinor = estimatedCostMinor")
            }
        }
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE itinerary_events ADD COLUMN participantIds TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE trip_options ADD COLUMN participantIds TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE trip_options ADD COLUMN bookingDeadline INTEGER")
                db.execSQL("ALTER TABLE trip_options ADD COLUMN cancellationDeadline INTEGER")
                db.execSQL("CREATE TABLE IF NOT EXISTS option_price_observations (id TEXT NOT NULL, tripId TEXT NOT NULL, optionId TEXT NOT NULL, priceMinor INTEGER NOT NULL, currency TEXT NOT NULL, exchangeRate REAL NOT NULL, observedAt INTEGER NOT NULL, PRIMARY KEY(id))")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_option_price_observations_tripId ON option_price_observations(tripId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_option_price_observations_optionId ON option_price_observations(optionId)")
                db.execSQL("INSERT INTO option_price_observations (id, tripId, optionId, priceMinor, currency, exchangeRate, observedAt) SELECT id || '-initial', tripId, id, estimatedCostMinor, currency, exchangeRate, observedAt FROM trip_options")
            }
        }
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pending_operations ADD COLUMN tripId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE pending_operations ADD COLUMN entityType TEXT NOT NULL DEFAULT 'trip_document'")
                db.execSQL("ALTER TABLE pending_operations ADD COLUMN baseVersion INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE pending_operations ADD COLUMN deleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("DELETE FROM pending_operations WHERE tripId = ''")
            }
        }
    }
}
