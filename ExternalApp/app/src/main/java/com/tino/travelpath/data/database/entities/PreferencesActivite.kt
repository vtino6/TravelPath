package com.tino.travelpath.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "preferences_activites",
    primaryKeys = ["preferenceId", "activite"],
    foreignKeys = [
        ForeignKey(
            entity = Preferences::class,
            parentColumns = ["id"],
            childColumns = ["preferenceId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PreferencesActivite(
    val preferenceId: String,
    val activite: Activite
)
