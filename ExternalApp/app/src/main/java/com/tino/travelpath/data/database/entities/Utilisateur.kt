package com.tino.travelpath.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "utilisateurs")
data class Utilisateur(
    @PrimaryKey
    val id: String,
    val nom: String,
    val email: String,
    val password: String? = null, // Hashed password (stored locally for session, but not used for auth)
    val dateCreation: Long = System.currentTimeMillis(),
    val dateModification: Long = System.currentTimeMillis()
)
