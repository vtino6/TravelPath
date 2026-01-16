package com.tino.travelpath.data.database.dao

import androidx.room.*
import com.tino.travelpath.data.database.entities.Parcours
import kotlinx.coroutines.flow.Flow

@Dao
interface ParcoursDao {
    @Query("SELECT * FROM parcours WHERE id = :id")
    suspend fun getById(id: String): Parcours?

    @Query("SELECT * FROM parcours WHERE utilisateurId = :utilisateurId")
    fun getByUtilisateur(utilisateurId: String): Flow<List<Parcours>>

    @Query("SELECT * FROM parcours WHERE utilisateurId = :utilisateurId AND estSauvegarde = 1")
    fun getSauvegardes(utilisateurId: String): Flow<List<Parcours>>

    @Query("SELECT * FROM parcours WHERE utilisateurId = :utilisateurId AND estFavori = 1")
    fun getFavoris(utilisateurId: String): Flow<List<Parcours>>

    @Query("SELECT * FROM parcours WHERE (utilisateurId = :utilisateurId OR utilisateurId IS NULL) AND estFavori = 1")
    fun getFavorisByUser(utilisateurId: String?): Flow<List<Parcours>>

    @Query("SELECT * FROM parcours WHERE estFavori = 1")
    suspend fun getFavorisSync(): List<Parcours>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(parcours: Parcours)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(parcours: List<Parcours>)

    @Update
    suspend fun update(parcours: Parcours)

    @Delete
    suspend fun delete(parcours: Parcours)

    @Query("UPDATE parcours SET estSauvegarde = :sauvegarde WHERE id = :id")
    suspend fun updateSauvegarde(id: String, sauvegarde: Boolean)

    @Query("UPDATE parcours SET estFavori = :favori WHERE id = :id")
    suspend fun updateFavori(id: String, favori: Boolean)
}
