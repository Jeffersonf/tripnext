package com.tripnext.app

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tripnext.app.data.local.TripNextDatabase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TripNextMigrationTest {
    private val databaseName = "tripnext-migration-test"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        TripNextDatabase::class.java
    )

    @Test
    fun migration6To7PreservesOptionAndCreatesInitialPriceObservation() {
        helper.createDatabase(databaseName, 6).apply {
            insertTrip()
            insertOption()
            close()
        }

        helper.runMigrationsAndValidate(databaseName, 7, true, TripNextDatabase.MIGRATION_6_7).use { db ->
            db.query("SELECT participantIds, bookingDeadline FROM trip_options WHERE id = 'option-1'").use { cursor ->
                cursor.moveToFirst()
                assertEquals("", cursor.getString(0))
                assertEquals(true, cursor.isNull(1))
            }
            db.query("SELECT priceMinor, currency, exchangeRate FROM option_price_observations WHERE optionId = 'option-1'").use { cursor ->
                cursor.moveToFirst()
                assertEquals(90_000L, cursor.getLong(0))
                assertEquals("BRL", cursor.getString(1))
                assertEquals(1.0, cursor.getDouble(2), 0.0)
            }
        }
    }

    @Test
    fun migration7To8DropsOnlyLegacyUnaddressableOperations() {
        helper.createDatabase("$databaseName-v8", 7).apply {
            insertTrip()
            execSQL("INSERT INTO pending_operations (id,kind,entityId,payload,deduplicationKey,createdAt,attempts) VALUES ('old','UPSERT_TRIP','trip-1','{\"id\":\"trip-1\"}','UPSERT_TRIP:trip-1',1,0)")
            close()
        }
        helper.runMigrationsAndValidate("$databaseName-v8", 8, true, TripNextDatabase.MIGRATION_7_8).use { db ->
            db.query("SELECT name FROM trips WHERE id='trip-1'").use { cursor -> cursor.moveToFirst(); assertEquals("Rio", cursor.getString(0)) }
            db.query("SELECT COUNT(*) FROM pending_operations").use { cursor -> cursor.moveToFirst(); assertEquals(0, cursor.getInt(0)) }
        }
    }

    @Test
    fun migration8To9PreservesTripAndAddsPlanningProfileDefaults() {
        helper.createDatabase("$databaseName-v9", 8).apply { insertTrip(); close() }
        helper.runMigrationsAndValidate("$databaseName-v9", 9, true, TripNextDatabase.MIGRATION_8_9).use { db ->
            db.query("SELECT name, origin, flexibleDates, pace, preferredStartHour, restMinutes, maxWalkingMinutes FROM trips WHERE id='trip-1'").use { cursor ->
                cursor.moveToFirst(); assertEquals("Rio", cursor.getString(0)); assertEquals("", cursor.getString(1)); assertEquals(0, cursor.getInt(2)); assertEquals("BALANCED", cursor.getString(3)); assertEquals(9, cursor.getInt(4)); assertEquals(60, cursor.getInt(5)); assertEquals(30, cursor.getInt(6))
            }
        }
    }

    private fun SupportSQLiteDatabase.insertTrip() = execSQL(
        "INSERT INTO trips (id,name,destination,startDate,endDate,totalBudgetMinor,currency,isActive,archived,travelers,updatedAt,contingencyPercent) VALUES ('trip-1','Rio','Rio',1,2,200000,'BRL',1,0,2,1,10)"
    )

    private fun SupportSQLiteDatabase.insertOption() = execSQL(
        "INSERT INTO trip_options (id,tripId,decisionGroup,title,type,provider,location,estimatedCostMinor,currency,cancellationPolicy,inclusions,pros,cons,sourceUrl,chosen,observedAt,origin,destination,departsAt,arrivesAt,stopCount,roomType,nightCount,durationMinutes,estimatedMinMinor,estimatedMaxMinor,exchangeRate,quoteDate,costScope,costClass) VALUES ('option-1','trip-1','Hotel','Centro','CHECK_IN','','',90000,'BRL','','','','','',1,123,'','',NULL,NULL,0,'Duplo',2,0,80000,100000,1.0,123,'GROUP','FIXED')"
    )
}
