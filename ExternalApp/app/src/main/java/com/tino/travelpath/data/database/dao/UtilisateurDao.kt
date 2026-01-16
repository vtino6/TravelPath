package com.tino.travelpath.data.database.dao

import androidx.room.*
import com.tino.travelpath.data.database.entities.Utilisateur
import kotlinx.coroutines.flow.Flow

@Dao
interface UtilisateurDao {
    @Query("SELECT * FROM utilisateurs WHERE id = :id")
    suspend fun getById(id: String): Utilisateur?

    @Query("SELECT * FROM utilisateurs WHERE email = :email")
    suspend fun getByEmail(email: String): Utilisateur?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(utilisateur: Utilisateur)

    @Update
    suspend fun update(utilisateur: Utilisateur)

    @Delete
    suspend fun delete(utilisateur: Utilisateur)

    @Query("SELECT * FROM utilisateurs")
    fun getAllFlow(): Flow<List<Utilisateur>>
}
