package com.tino.travelpath.data.repository

import com.google.gson.Gson
import com.tino.travelpath.data.api.RetrofitClient
import com.tino.travelpath.data.api.dto.RouteRequest
import com.tino.travelpath.data.api.dto.RouteResponse
import com.tino.travelpath.data.api.dto.StepResponse
import com.tino.travelpath.data.database.dao.ParcoursDao
import com.tino.travelpath.data.database.entities.Parcours
import com.tino.travelpath.data.database.entities.TransportationMode
import com.tino.travelpath.data.database.entities.TypeParcours
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

class RoutesRepository(
    private val apiService: com.tino.travelpath.data.api.TravelPathApiService = RetrofitClient.apiService,
    private val parcoursDao: ParcoursDao? = null
) {
    private val gson = Gson()
    
    suspend fun generateRoutes(request: RouteRequest): List<RouteResponse> {
        android.util.Log.d("RoutesRepository", "=== SENDING ROUTE GENERATION REQUEST ===")
        android.util.Log.d("RoutesRepository", "Location: ${request.latitude}, ${request.longitude}")
        android.util.Log.d("RoutesRepository", "Activities: ${request.activities}")
        android.util.Log.d("RoutesRepository", "Budget: ${request.maxBudget}")
        android.util.Log.d("RoutesRepository", "Number of Places: ${request.numberOfPlaces}")
        android.util.Log.d("RoutesRepository", "Transportation Mode: ${request.transportationMode}")
        android.util.Log.d("RoutesRepository", "Required Places: ${request.requiredPlaceIds}")
        
        return try {
            android.util.Log.d("RoutesRepository", "Calling API: POST /api/routes/generate")
            val response = apiService.generateRoutes(request)
            android.util.Log.d("RoutesRepository", "API call successful. Received ${response.size} routes.")
            
            response.forEachIndexed { index, route ->
                android.util.Log.d("RoutesRepository", "Route $index: id=${route.id}, name=${route.name}, type=${route.routeType}, budget=${route.totalBudget}, duration=${route.totalDuration}, steps=${route.steps.size}")
                route.steps.forEachIndexed { stepIndex, step ->
                    android.util.Log.d("RoutesRepository", "  Step $stepIndex: place=${step.place.name}, order=${step.order}, cost=${step.cost}, category=${step.place.category}")
                }
            }
            
            if (response.isEmpty()) {
                android.util.Log.w("RoutesRepository", "WARNING: Received empty route list from backend!")
            }
            
            response
        } catch (e: com.google.gson.JsonSyntaxException) {
            android.util.Log.e("RoutesRepository", "JSON parsing error - response format doesn't match expected DTO", e)
            android.util.Log.e("RoutesRepository", "Exception details: ${e.message}")
            e.printStackTrace()
            throw RuntimeException("Erreur de format JSON: ${e.message}", e)
        } catch (e: retrofit2.HttpException) {
            val errorBody = try {
                e.response()?.errorBody()?.string() ?: e.message()
            } catch (ex: Exception) {
                e.message() ?: "Unknown error"
            }
            android.util.Log.e("RoutesRepository", "HTTP Error ${e.code()}: $errorBody")
            android.util.Log.e("RoutesRepository", "Response headers: ${e.response()?.headers()}")
            throw RuntimeException("HTTP ${e.code()}: $errorBody", e)
        } catch (e: java.net.SocketTimeoutException) {
            android.util.Log.e("RoutesRepository", "Request timeout", e)
            throw RuntimeException("Request timeout - le serveur met trop de temps à répondre", e)
        } catch (e: java.net.UnknownHostException) {
            android.util.Log.e("RoutesRepository", "Cannot connect to server", e)
            throw RuntimeException("Impossible de se connecter au serveur. Vérifiez votre connexion.", e)
        } catch (e: Exception) {
            android.util.Log.e("RoutesRepository", "Error generating routes", e)
            android.util.Log.e("RoutesRepository", "Exception type: ${e.javaClass.name}")
            android.util.Log.e("RoutesRepository", "Exception message: ${e.message}")
            e.printStackTrace()
            throw RuntimeException("Erreur lors de la génération: ${e.message ?: "Erreur inconnue"}", e)
        }
    }
    
    suspend fun saveRoute(route: RouteResponse, userId: String? = null): RouteResponse? {
        return try {
            android.util.Log.d("RoutesRepository", "=== SAVING ROUTE ===")
            android.util.Log.d("RoutesRepository", "Route ID: ${route.id}, Route name: ${route.name}")
            android.util.Log.d("RoutesRepository", "Route isFavorite: ${route.isFavorite}")
            android.util.Log.d("RoutesRepository", "UserId: $userId")
            
            // Save to backend first
            val savedRoute = apiService.saveRoute(route, userId)
            android.util.Log.d("RoutesRepository", "Route saved to backend successfully, isFavorite: ${savedRoute?.isFavorite}")
            
            // If route is liked, also save to Room for offline access
            if (savedRoute != null && (savedRoute.isFavorite == true || route.isFavorite == true)) {
                saveRouteToRoom(savedRoute, userId)
            }
            
            android.util.Log.d("RoutesRepository", "=== SAVE ROUTE COMPLETED ===")
            savedRoute
        } catch (e: Exception) {
            android.util.Log.e("RoutesRepository", "Error saving route: ${e.message}", e)
            e.printStackTrace()
            null
        }
    }
    
    suspend fun getSavedRoutes(userId: String? = null): List<RouteResponse> {
        return try {
            apiService.getSavedRoutes(userId)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    suspend fun getRouteById(routeId: String): RouteResponse? {
        return try {
            apiService.getRouteById(routeId)
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun toggleFavorite(routeId: String, userId: String) {
        try {
            apiService.toggleFavorite(routeId, userId)
        } catch (e: Exception) {
            // ignore
        }
    }
    
    suspend fun deleteRoute(routeId: String, userId: String) {
        try {
            apiService.deleteRoute(routeId, userId)
            // Also delete from Room
            deleteRouteFromRoom(routeId)
        } catch (e: Exception) {
            // ignore
        }
    }

    /**
     * Save route to Room database (SQLite) for offline access
     */
    suspend fun saveRouteToRoom(route: RouteResponse, userId: String? = null) {
        if (parcoursDao == null) {
            android.util.Log.w("RoutesRepository", "ParcoursDao is null, cannot save to Room")
            return
        }
        try {
            val parcours = routeToParcours(route, userId)
            parcoursDao.insert(parcours)
            android.util.Log.d("RoutesRepository", "Route ${route.id} saved to Room database")
        } catch (e: Exception) {
            android.util.Log.e("RoutesRepository", "Error saving route to Room: ${e.message}", e)
        }
    }

    /**
     * Load liked routes from Room database (offline-first)
     */
    suspend fun getLikedRoutesFromRoom(userId: String? = null): List<RouteResponse> {
        if (parcoursDao == null) {
            android.util.Log.w("RoutesRepository", "ParcoursDao is null, cannot load from Room")
            return emptyList()
        }
        return try {
            val parcoursList = if (userId != null) {
                parcoursDao.getFavorisByUser(userId).first()
            } else {
                parcoursDao.getFavorisSync()
            }
            android.util.Log.d("RoutesRepository", "Loaded ${parcoursList.size} liked routes from Room")
            parcoursList.map { parcoursToRoute(it) }
        } catch (e: Exception) {
            android.util.Log.e("RoutesRepository", "Error loading routes from Room: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Delete route from Room database
     */
    suspend fun deleteRouteFromRoom(routeId: String) {
        if (parcoursDao == null) return
        try {
            val parcours = parcoursDao.getById(routeId)
            if (parcours != null) {
                parcoursDao.delete(parcours)
                android.util.Log.d("RoutesRepository", "Route $routeId deleted from Room")
            }
        } catch (e: Exception) {
            android.util.Log.e("RoutesRepository", "Error deleting route from Room: ${e.message}", e)
        }
    }

    /**
     * Convert RouteResponse to Parcours entity
     */
    private fun routeToParcours(route: RouteResponse, userId: String?): Parcours {
        val typeParcours = when (route.routeType) {
            "ECONOMIC" -> TypeParcours.ECONOMIQUE
            "BALANCED" -> TypeParcours.EQUILIBRE
            "COMFORT" -> TypeParcours.CONFORT
            else -> TypeParcours.ECONOMIQUE
        }

        val transportationMode = when (route.transportationMode) {
            "WALKING" -> TransportationMode.WALKING
            "BICYCLE" -> TransportationMode.BICYCLE
            "PUBLIC_TRANSPORT" -> TransportationMode.PUBLIC_TRANSPORT
            "CAR" -> TransportationMode.CAR
            "MIXED" -> TransportationMode.MIXED
            else -> TransportationMode.MIXED
        }

        // Convert steps to JSON string
        val stepsJson = gson.toJson(route.steps)

        return Parcours(
            id = route.id,
            utilisateurId = userId,
            nom = route.name,
            typeParcours = typeParcours,
            budgetTotal = route.totalBudget,
            dureeTotale = route.totalDuration,
            transportationMode = transportationMode,
            ville = route.city,
            dateCreation = System.currentTimeMillis(),
            dateModification = System.currentTimeMillis(),
            estSauvegarde = false,
            estFavori = route.isFavorite ?: false,
            stepsJson = stepsJson
        )
    }

    /**
     * Convert Parcours entity to RouteResponse
     */
    private fun parcoursToRoute(parcours: Parcours): RouteResponse {
        val routeType = when (parcours.typeParcours) {
            TypeParcours.ECONOMIQUE -> "ECONOMIC"
            TypeParcours.EQUILIBRE -> "BALANCED"
            TypeParcours.CONFORT -> "COMFORT"
        }

        val transportationMode = when (parcours.transportationMode) {
            TransportationMode.WALKING -> "WALKING"
            TransportationMode.BICYCLE -> "BICYCLE"
            TransportationMode.PUBLIC_TRANSPORT -> "PUBLIC_TRANSPORT"
            TransportationMode.CAR -> "CAR"
            TransportationMode.MIXED -> "MIXED"
        }

        // Parse steps from JSON string
        val steps = if (parcours.stepsJson != null && parcours.stepsJson.isNotBlank()) {
            try {
                gson.fromJson(parcours.stepsJson, Array<StepResponse>::class.java).toList()
            } catch (e: Exception) {
                android.util.Log.e("RoutesRepository", "Error parsing steps JSON: ${e.message}", e)
                emptyList()
            }
        } else {
            emptyList()
        }

        return RouteResponse(
            id = parcours.id,
            name = parcours.nom,
            routeType = routeType,
            totalBudget = parcours.budgetTotal,
            totalDuration = parcours.dureeTotale,
            transportationMode = transportationMode,
            city = parcours.ville,
            isFavorite = parcours.estFavori,
            steps = steps
        )
    }
}


