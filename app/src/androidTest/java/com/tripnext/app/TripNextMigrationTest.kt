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

    private fun SupportSQLiteDatabase.insertTrip() = execSQL(
        "INSERT INTO trips (id,name,destination,startDate,endDate,totalBudgetMinor,currency,isActive,archived,travelers,updatedAt,contingencyPercent) VALUES ('trip-1','Rio','Rio',1,2,200000,'BRL',1,0,2,1,10)"
    )

    private fun SupportSQLiteDatabase.insertOption() = execSQL(
        "INSERT INTO trip_options (id,tripId,decisionGroup,title,type,provider,location,estimatedCostMinor,currency,cancellationPolicy,inclusions,pros,cons,sourceUrl,chosen,observedAt,origin,destination,departsAt,arrivesAt,stopCount,roomType,nightCount,durationMinutes,estimatedMinMinor,estimatedMaxMinor,exchangeRate,quoteDate,costScope,costClass) VALUES ('option-1','trip-1','Hotel','Centro','CHECK_IN','','',90000,'BRL','','','','','',1,123,'','',NULL,NULL,0,'Duplo',2,0,80000,100000,1.0,123,'GROUP','FIXED')"
    )
}
