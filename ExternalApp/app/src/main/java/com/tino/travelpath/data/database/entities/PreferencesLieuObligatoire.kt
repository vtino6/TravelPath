package com.tino.travelpath.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "preferences_lieux_obligatoires",
    primaryKeys = ["preferenceId", "lieuId"],
    foreignKeys = [
        ForeignKey(
            entity = Preferences::class,
            parentColumns = ["id"],
            childColumns = ["preferenceId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Lieu::class,
            parentColumns = ["id"],
            childColumns = ["lieuId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["lieuId"])]
)
data class PreferencesLieuObligatoire(
    val preferenceId: String,
    val lieuId: String
)
