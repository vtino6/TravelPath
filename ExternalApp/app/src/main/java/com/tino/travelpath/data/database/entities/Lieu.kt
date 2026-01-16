package com.tino.travelpath.data.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "lieux",
    indices = [
        Index(value = ["categorie"]),
        Index(value = ["latitude", "longitude"]),
        Index(value = ["nom"])
    ]
)
data class Lieu(
    @PrimaryKey
    val id: String,
    val nom: String,
    val categorie: Activite,
    val latitude: Double,
    val longitude: Double,
    val adresse: String?,
    val description: String?,
    val coutMoyen: Double?,
    val impactFroid: Int = 0,
    val impactChaleur: Int = 0,
    val impactHumidite: Int = 0,
    val tempsAttenteEstime: Int?,
    val dateCreation: Long = System.currentTimeMillis(),
    val dateModification: Long = System.currentTimeMillis()
)
