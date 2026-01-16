package com.tino.travelpath.data.repository

import com.tino.travelpath.data.api.RetrofitClient
import com.tino.travelpath.data.api.dto.PlaceResponse
import com.tino.travelpath.data.database.dao.LieuDao
import com.tino.travelpath.data.database.entities.Activite
import com.tino.travelpath.data.database.entities.Lieu
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first

/**
 * Repository that combines local (Room) and remote (Spring Boot API) data sources
 */
class PlacesRepository(
    private val lieuDao: LieuDao,
    private val apiService: com.tino.travelpath.data.api.TravelPathApiService = RetrofitClient.apiService
) {
    
    /**
     * Search for places - offline-first approach
     * 1. Returns cached data immediately
     * 2. Fetches from API in background
     * 3. Updates cache and emits new data
     */
    fun searchPlaces(
        latitude: Double,
        longitude: Double,
        category: Activite,
        forceRefresh: Boolean = false
    ): Flow<List<Lieu>> = flow {
        // 1. Emit cached data immediately (offline-first)
        val cached = lieuDao.getByCategorie(category).first()
        emit(cached)
        
        // 2. Fetch from backend API if needed
        if (forceRefresh || cached.isEmpty()) {
            try {
                // Map Android Activite to backend PlaceCategory
                val backendCategory = when (category) {
                    Activite.RESTAURATION -> "RESTAURANT"
                    Activite.LOISIRS -> "LEISURE"
                    Activite.DECOUVERTE -> "DISCOVERY"
                    Activite.CULTURE -> "CULTURE"
                }
                
                val apiPlaces = apiService.searchPlaces(
                    latitude = latitude,
                    longitude = longitude,
                    radius = 5000,
                    category = backendCategory
                )
                
                // 3. Convert API response to Room entities
                val entities = apiPlaces.map { it.toLieuEntity() }
                
                // 4. Save to Room database
                lieuDao.insertAll(entities)
                
                // 5. Emit updated data
                emit(entities)
            } catch (e: Exception) {
                // If network fails, return cached data
                // Error is logged but doesn't crash the app
                emit(cached)
            }
        }
    }
    
    /**
     * Get place by ID - checks cache first, then API
     */
    suspend fun getPlaceById(id: String): Lieu? {
        // Check cache first
        val cached = lieuDao.getById(id)
        if (cached != null) return cached
        
        // Fetch from API if not in cache
        return try {
            val apiPlace = apiService.getPlaceDetails(id)
            val entity = apiPlace.toLieuEntity()
            lieuDao.insert(entity)
            entity
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Convert API PlaceResponse to Room Lieu entity
     */
    private fun PlaceResponse.toLieuEntity(): Lieu {
        // Map backend PlaceCategory to Android Activite
        val activite = when (this.category) {
            "RESTAURANT" -> Activite.RESTAURATION
            "LEISURE" -> Activite.LOISIRS
            "DISCOVERY" -> Activite.DECOUVERTE
            "CULTURE" -> Activite.CULTURE
            else -> Activite.CULTURE // Default fallback
        }
        
        return Lieu(
            id = this.id,
            nom = this.name,
            categorie = activite,
            latitude = this.latitude,
            longitude = this.longitude,
            adresse = this.address,
            description = this.description,
            coutMoyen = this.averageCost,
            tempsAttenteEstime = this.estimatedWaitTime
        )
    }
}


