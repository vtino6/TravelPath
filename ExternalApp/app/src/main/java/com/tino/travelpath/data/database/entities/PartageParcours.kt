package com.tino.travelpath.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "partages_parcours",
    foreignKeys = [
        ForeignKey(
            entity = Parcours::class,
            parentColumns = ["id"],
            childColumns = ["parcoursId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Utilisateur::class,
            parentColumns = ["id"],
            childColumns = ["utilisateurSourceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["parcoursId"]),
        Index(value = ["utilisateurSourceId"])
    ]
)
data class PartageParcours(
    @PrimaryKey
    val id: String,
    val parcoursId: String,
    val utilisateurSourceId: String,
    val typePartage: TypePartage,
    val destinataire: String?,
    val datePartage: Long = System.currentTimeMillis()
)
