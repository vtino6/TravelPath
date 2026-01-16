package com.tino.travelpath.data.database.dao

import androidx.room.*
import com.tino.travelpath.data.database.entities.Preferences

@Dao
interface PreferencesDao {
    @Query("SELECT * FROM preferences WHERE profilId = :profilId")
    suspend fun getByProfil(profilId: String): Preferences?

    @Query("SELECT * FROM preferences WHERE id = :id")
    suspend fun getById(id: String): Preferences?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(preferences: Preferences)

    @Update
    suspend fun update(preferences: Preferences)

    @Delete
    suspend fun delete(preferences: Preferences)
}
