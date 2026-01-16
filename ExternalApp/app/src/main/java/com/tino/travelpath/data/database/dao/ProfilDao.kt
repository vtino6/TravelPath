package com.tino.travelpath.data.database.dao

import androidx.room.*
import com.tino.travelpath.data.database.entities.Profil
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfilDao {
    @Query("SELECT * FROM profils WHERE utilisateurId = :utilisateurId")
    fun getByUtilisateur(utilisateurId: String): Flow<List<Profil>>
    
    @Query("SELECT * FROM profils WHERE utilisateurId = :utilisateurId")
    suspend fun getByUtilisateurSync(utilisateurId: String): List<Profil>

    @Query("SELECT * FROM profils WHERE id = :id")
    suspend fun getById(id: String): Profil?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profil: Profil)

    @Update
    suspend fun update(profil: Profil)

    @Delete
    suspend fun delete(profil: Profil)
}
