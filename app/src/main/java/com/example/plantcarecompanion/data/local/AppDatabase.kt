package com.example.plantcarecompanion.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.plantcarecompanion.data.local.dao.CareLogDao
import com.example.plantcarecompanion.data.local.dao.PlantDao
import com.example.plantcarecompanion.data.local.entity.CareLogEntry
import com.example.plantcarecompanion.data.local.entity.Plant
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.plantcarecompanion.data.local.dao.SpeciesDao
import com.example.plantcarecompanion.data.local.entity.CachedSpecies

@Database(
    entities = [Plant::class, CareLogEntry::class, CachedSpecies::class],
    version = 2
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun plantDao(): PlantDao
    abstract fun careLogDao(): CareLogDao
    abstract fun speciesDao(): SpeciesDao

    companion object {
        private var db: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `cached_species` (" +
                        "`id` INTEGER NOT NULL, " +
                        "`commonName` TEXT NOT NULL, " +
                        "`scientificName` TEXT, " +
                        "`wateringLabel` TEXT, " +
                        "`wateringIntervalDays` INTEGER, " +
                        "`lightRequirement` TEXT, " +
                        "`imageUrl` TEXT, " +
                        "`cachedAt` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`id`))"
                )
            }
        }

        fun get(context: Context): AppDatabase {
            if (db == null) {
                db = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "plant_care.db"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
            }
            return db!!
        }
    }
}