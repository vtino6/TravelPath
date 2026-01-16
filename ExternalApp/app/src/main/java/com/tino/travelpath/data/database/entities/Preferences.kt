package com.tino.travelpath.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "preferences",
    foreignKeys = [
        ForeignKey(
            entity = Profil::class,
            parentColumns = ["id"],
            childColumns = ["profilId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["profilId"])]
)
data class Preferences(
    @PrimaryKey
    val id: String,
    val profilId: String,
    val budgetMax: Double?,
    val duree: Int?,
    val niveauEffort: NiveauEffort,
    val sensibiliteFroid: Int = 0,
    val sensibiliteChaleur: Int = 0,
    val sensibiliteHumidite: Int = 0,
    val dateCreation: Long = System.currentTimeMillis()
)
