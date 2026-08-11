package com.tripnext.app

import android.app.Application
import androidx.room.Room
import com.tripnext.app.data.OfflineTripRemoteRepository
import com.tripnext.app.data.TripRepository
import com.tripnext.app.data.local.TripNextDatabase

class TripNextApplication : Application() {
    lateinit var repository: TripRepository; private set
    override fun onCreate() {
        super.onCreate()
        val database = Room.databaseBuilder(this, TripNextDatabase::class.java, "tripnext.db").addMigrations(TripNextDatabase.MIGRATION_1_2, TripNextDatabase.MIGRATION_2_3).build()
        repository = TripRepository(database.tripDao(), database.pendingOperationDao(), OfflineTripRemoteRepository())
    }
}
