package com.tino.travelpath.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "parcours",
    foreignKeys = [
        ForeignKey(
            entity = Utilisateur::class,
            parentColumns = ["id"],
            childColumns = ["utilisateurId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["utilisateurId"]),
        Index(value = ["typeParcours"]),
        Index(value = ["estSauvegarde"]),
        Index(value = ["estFavori"])
    ]
)
data class Parcours(
    @PrimaryKey
    val id: String,
    val utilisateurId: String?,
    val nom: String,
    val typeParcours: TypeParcours,
    val budgetTotal: Double,
    val dureeTotale: Int,
    val transportationMode: TransportationMode, // Replaced niveauEffort
    val ville: String?,
    val dateCreation: Long = System.currentTimeMillis(),
    val dateModification: Long = System.currentTimeMillis(),
    val estSauvegarde: Boolean = false,
    val estFavori: Boolean = false,
    val stepsJson: String? = null // JSON string containing all steps
)
