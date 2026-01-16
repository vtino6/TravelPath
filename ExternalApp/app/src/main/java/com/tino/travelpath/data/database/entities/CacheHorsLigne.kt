package com.tino.travelpath.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cache_hors_ligne",
    foreignKeys = [
        ForeignKey(
            entity = Utilisateur::class,
            parentColumns = ["id"],
            childColumns = ["utilisateurId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["utilisateurId"]),
        Index(value = ["typeContenu", "contenuId"]),
        Index(value = ["dateExpiration"])
    ]
)
data class CacheHorsLigne(
    @PrimaryKey
    val id: String,
    val utilisateurId: String,
    val typeContenu: TypeContenu,
    val contenuId: String,
    val donneesCache: String?,
    val dateMiseEnCache: Long = System.currentTimeMillis(),
    val dateExpiration: Long?
)
