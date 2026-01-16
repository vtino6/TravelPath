package com.tino.travelpath.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "profils",
    foreignKeys = [
        ForeignKey(
            entity = Utilisateur::class,
            parentColumns = ["id"],
            childColumns = ["utilisateurId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["utilisateurId"])]
)
data class Profil(
    @PrimaryKey
    val id: String,
    val utilisateurId: String,
    val nom: String,
    val typeProfil: TypeProfil,
    val dateCreation: Long = System.currentTimeMillis()
)
