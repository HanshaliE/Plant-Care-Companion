package com.example.plantcarecompanion.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.plantcarecompanion.data.local.dao.CareLogDao
import com.example.plantcarecompanion.data.local.dao.PlantDao
import com.example.plantcarecompanion.data.local.entity.CareLogEntry
import com.example.plantcarecompanion.data.local.entity.Plant

@Database(
    entities = [Plant::class, CareLogEntry::class],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun plantDao(): PlantDao
    abstract fun careLogDao(): CareLogDao

    companion object {
        private var db: AppDatabase? = null

        fun get(context: Context): AppDatabase {
            if (db == null) {
                db = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "plant_care.db"
                ).build()
            }
            return db!!
        }
    }
}