package com.tino.travelpath.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "historique_navigation",
    foreignKeys = [
        ForeignKey(
            entity = Utilisateur::class,
            parentColumns = ["id"],
            childColumns = ["utilisateurId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Parcours::class,
            parentColumns = ["id"],
            childColumns = ["parcoursId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Etape::class,
            parentColumns = ["id"],
            childColumns = ["etapeActuelleId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["utilisateurId"]),
        Index(value = ["parcoursId"]),
        Index(value = ["etapeActuelleId"])
    ]
)
data class HistoriqueNavigation(
    @PrimaryKey
    val id: String,
    val utilisateurId: String,
    val parcoursId: String,
    val etapeActuelleId: String?,
    val dateDebut: Long = System.currentTimeMillis(),
    val dateFin: Long?,
    val estTermine: Boolean = false
)
