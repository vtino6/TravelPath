package com.tino.travelpath.data.database.dao

import androidx.room.*
import com.tino.travelpath.data.database.entities.Etape
import kotlinx.coroutines.flow.Flow

@Dao
interface EtapeDao {
    @Query("SELECT * FROM etapes WHERE parcoursId = :parcoursId ORDER BY ordre ASC")
    fun getByParcours(parcoursId: String): Flow<List<Etape>>

    @Query("SELECT * FROM etapes WHERE parcoursId = :parcoursId ORDER BY ordre ASC")
    suspend fun getByParcoursSync(parcoursId: String): List<Etape>

    @Query("SELECT * FROM etapes WHERE id = :id")
    suspend fun getById(id: String): Etape?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(etape: Etape)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(etapes: List<Etape>)

    @Update
    suspend fun update(etape: Etape)

    @Delete
    suspend fun delete(etape: Etape)

    @Query("DELETE FROM etapes WHERE parcoursId = :parcoursId")
    suspend fun deleteByParcours(parcoursId: String)
}
