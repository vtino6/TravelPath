package com.tino.travelpath.data.database.dao

import androidx.room.*
import com.tino.travelpath.data.database.entities.Activite
import com.tino.travelpath.data.database.entities.Lieu
import kotlinx.coroutines.flow.Flow

@Dao
interface LieuDao {
    @Query("SELECT * FROM lieux WHERE id = :id")
    suspend fun getById(id: String): Lieu?

    @Query("SELECT * FROM lieux WHERE categorie = :categorie")
    fun getByCategorie(categorie: Activite): Flow<List<Lieu>>

    @Query("SELECT * FROM lieux WHERE nom LIKE :query OR description LIKE :query OR adresse LIKE :query")
    suspend fun search(query: String): List<Lieu>

    @Query("""
        SELECT * FROM lieux 
        WHERE (latitude BETWEEN :minLat AND :maxLat) 
        AND (longitude BETWEEN :minLng AND :maxLng)
    """)
    suspend fun getByLocation(minLat: Double, maxLat: Double, minLng: Double, maxLng: Double): List<Lieu>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(lieu: Lieu)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(lieux: List<Lieu>)

    @Update
    suspend fun update(lieu: Lieu)

    @Delete
    suspend fun delete(lieu: Lieu)
}
