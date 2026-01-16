package com.tino.travelpath.data.database.dao

import androidx.room.*
import com.tino.travelpath.data.database.entities.Media
import com.tino.travelpath.data.database.entities.TypeMedia

@Dao
interface MediaDao {
    @Query("SELECT * FROM medias WHERE id = :id")
    suspend fun getById(id: String): Media?

    @Query("SELECT * FROM medias WHERE typeMedia = :type")
    suspend fun getByType(type: TypeMedia): List<Media>

    @Query("SELECT * FROM medias WHERE estEnCache = 1")
    suspend fun getCached(): List<Media>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(media: Media)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(medias: List<Media>)

    @Update
    suspend fun update(media: Media)

    @Delete
    suspend fun delete(media: Media)

    @Query("UPDATE medias SET estEnCache = :cached, cheminCache = :chemin WHERE id = :id")
    suspend fun updateCache(id: String, cached: Boolean, chemin: String?)
}
