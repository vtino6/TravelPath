package com.tino.travelpath.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.tino.travelpath.data.model.Photo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

/**
 * Repository for querying photos from TravelShare's Firestore database
 */
class PhotoRepository {
    private val db = FirebaseFirestore.getInstance()
    private val photosCollection = db.collection("photos")
    
    /**
     * Get all photos (for extracting unique cities)
     */
    fun getAllPhotos(): Flow<List<Photo>> = flow {
        try {
            val snapshot = photosCollection
                .get()
                .await()
            
            val photos = snapshot.documents.mapNotNull { document ->
                try {
                    val data = document.data ?: emptyMap()
                    documentToPhoto(document.id, data)
                } catch (e: Exception) {
                    android.util.Log.e("PhotoRepository", "Error parsing photo ${document.id}: ${e.message}")
                    null
                }
            }
            
            emit(photos)
        } catch (e: Exception) {
            android.util.Log.e("PhotoRepository", "Error fetching photos: ${e.message}", e)
            emit(emptyList())
        }
    }
    
    /**
     * Get photos by city name (exact match)
     */
    fun getPhotosByCity(cityName: String): Flow<List<Photo>> = flow {
        try {
            val snapshot = photosCollection
                .whereEqualTo("locationName", cityName)
                .limit(50)
                .get()
                .await()
            
            // Sort by timestamp in memory (to avoid requiring Firestore composite index)
            val photos = snapshot.documents.mapNotNull { document ->
                try {
                    val data = document.data ?: emptyMap()
                    documentToPhoto(document.id, data)
                } catch (e: Exception) {
                    android.util.Log.e("PhotoRepository", "Error parsing photo ${document.id}: ${e.message}")
                    null
                }
            }.sortedByDescending { photo ->
                // Sort by timestamp if available, otherwise by date string
                photo.timestamp?.seconds ?: 0L
            }
            
            emit(photos)
        } catch (e: Exception) {
            android.util.Log.e("PhotoRepository", "Error fetching photos for city $cityName: ${e.message}", e)
            emit(emptyList())
        }
    }
    
    /**
     * Search cities by query (returns unique city names matching the query)
     * Handles landmark-to-city mapping (e.g., "Tour Eiffel" -> "Paris")
     */
    fun searchCities(query: String): Flow<List<String>> = flow {
        try {
            if (query.isBlank()) {
                emit(emptyList())
                return@flow
            }
            
            val normalizedQuery = query.trim().lowercase()
            
            // Landmark keywords that should match specific locations
            // Key: search term, Value: location name pattern to match
            val landmarkKeywords = mapOf(
                "paris" to listOf("tour eiffel", "eiffel"),
                "eiffel" to listOf("tour eiffel", "eiffel"),
                "rome" to listOf("colisée", "coliseum", "colosseum"),
                "colisée" to listOf("colisée", "coliseum", "colosseum"),
                "coliseum" to listOf("colisée", "coliseum", "colosseum"),
                "grand canyon" to listOf("grand canyon"),
                "canyon" to listOf("grand canyon")
            )
            
            val snapshot = photosCollection
                .get()
                .await()
            
            // Extract unique city names that contain the query (case-insensitive)
            val matchingCities = snapshot.documents
                .mapNotNull { document ->
                    val data = document.data
                    val locationName = data?.get("locationName") as? String
                    if (locationName == null) return@mapNotNull null
                    
                    val normalizedLocation = locationName.trim().lowercase()
                    
                    // Check if query matches location name directly
                    if (normalizedLocation.contains(normalizedQuery)) {
                        locationName.trim()
                    }
                    // Check if query matches any landmark keywords
                    else {
                        landmarkKeywords.entries.firstOrNull { (keyword, patterns) ->
                            normalizedQuery.contains(keyword) && 
                            patterns.any { pattern -> normalizedLocation.contains(pattern) }
                        }?.let { locationName.trim() }
                    }
                }
                .distinct()
                .sorted()
                .take(10) // Limit to top 10 matches
            
            emit(matchingCities)
        } catch (e: Exception) {
            android.util.Log.e("PhotoRepository", "Error searching cities: ${e.message}", e)
            emit(emptyList())
        }
    }
    
    /**
     * Convert Firestore document to Photo model
     * Handles both old and new data formats
     */
    private fun documentToPhoto(id: String, data: Map<String, Any?>): Photo {
        return Photo(
            id = id,
            locationName = data["locationName"] as? String,
            imageUrl = data["imageUrl"] as? String,
            url = data["url"] as? String,
            authorName = data["authorName"] as? String,
            userName = data["userName"] as? String,
            title = data["title"] as? String,
            date = data["date"] as? String,
            timestamp = data["timestamp"] as? com.google.firebase.Timestamp,
            visibility = data["visibility"] as? String,
            likesCount = (data["likesCount"] as? Long)?.toInt() ?: 0,
            reportsCount = (data["reportsCount"] as? Long)?.toInt() ?: 0
        )
    }
}
