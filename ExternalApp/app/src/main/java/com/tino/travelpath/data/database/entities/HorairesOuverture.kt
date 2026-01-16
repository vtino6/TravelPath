package com.tino.travelpath.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "horaires_ouverture",
    foreignKeys = [
        ForeignKey(
            entity = Lieu::class,
            parentColumns = ["id"],
            childColumns = ["lieuId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class HorairesOuverture(
    @PrimaryKey
    val lieuId: String,
    val lundiOuverture: String?,
    val lundiFermeture: String?,
    val mardiOuverture: String?,
    val mardiFermeture: String?,
    val mercrediOuverture: String?,
    val mercrediFermeture: String?,
    val jeudiOuverture: String?,
    val jeudiFermeture: String?,
    val vendrediOuverture: String?,
    val vendrediFermeture: String?,
    val samediOuverture: String?,
    val samediFermeture: String?,
    val dimancheOuverture: String?,
    val dimancheFermeture: String?
)
