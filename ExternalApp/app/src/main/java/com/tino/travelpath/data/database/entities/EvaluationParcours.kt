package com.tino.travelpath.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "evaluations_parcours",
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
        )
    ],
    indices = [
        Index(value = ["parcoursId"]),
        Index(value = ["utilisateurId", "parcoursId"], unique = true)
    ]
)
data class EvaluationParcours(
    @PrimaryKey
    val id: String,
    val utilisateurId: String,
    val parcoursId: String,
    val evaluation: Evaluation,
    val dateEvaluation: Long = System.currentTimeMillis()
)
