package com.tino.travelpath.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "etapes",
    foreignKeys = [
        ForeignKey(
            entity = Parcours::class,
            parentColumns = ["id"],
            childColumns = ["parcoursId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Lieu::class,
            parentColumns = ["id"],
            childColumns = ["lieuId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["parcoursId"]),
        Index(value = ["parcoursId", "ordre"]),
        Index(value = ["lieuId"])
    ]
)
data class Etape(
    @PrimaryKey
    val id: String,
    val parcoursId: String,
    val lieuId: String,
    val ordre: Int,
    val creneau: Creneau,
    val dureeEstimee: Int,
    val distancePrecedente: Double?,
    val cout: Double = 0.0,
    val notes: String?
)
