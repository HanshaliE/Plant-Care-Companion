package com.example.plantcarecompanion.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey


@Entity(
    tableName = "care_log_entries",
    foreignKeys = [
        ForeignKey(
            entity = Plant::class,
            parentColumns = ["id"],
            childColumns = ["plantId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("plantId")]
)
data class CareLogEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val plantId: Int,
    val careType: String,
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String? = null
)