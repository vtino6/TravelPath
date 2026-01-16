package com.tino.travelpath.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tino.travelpath.data.api.dto.RouteRequest
import com.tino.travelpath.data.api.dto.RouteResponse
import com.tino.travelpath.data.database.entities.TransportationMode
import com.tino.travelpath.data.repository.RoutesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RouteSelectionViewModel(
    private val routesRepository: RoutesRepository
) : ViewModel() {
    
    private val _routes = MutableStateFlow<List<RouteResponse>>(emptyList())
    val routes: StateFlow<List<RouteResponse>> = _routes.asStateFlow()
    
    private val _startingPoint = MutableStateFlow<Pair<Double, Double>?>(null)
    val startingPoint: StateFlow<Pair<Double, Double>?> = _startingPoint.asStateFlow()
    
    private val _startingPointName = MutableStateFlow<String?>(null)
    val startingPointName: StateFlow<String?> = _startingPointName.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    // Store selected transportation modes from preferences
    private val _selectedTransportationModes = MutableStateFlow<Set<TransportationMode>>(emptySet())
    val selectedTransportationModes: StateFlow<Set<TransportationMode>> = _selectedTransportationModes.asStateFlow()
    
    fun generateRoutes(request: RouteRequest, userId: String? = null, locationName: String? = null, selectedModes: Set<TransportationMode> = emptySet()) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                android.util.Log.d("RouteSelectionViewModel", "=== GENERATING ROUTES ===")
                android.util.Log.d("RouteSelectionViewModel", "UserId: $userId")
                
                // Store the starting point from the request
                _startingPoint.value = Pair(request.latitude, request.longitude)
                _startingPointName.value = locationName ?: "Point de départ"
                android.util.Log.d("RouteSelectionViewModel", "Stored starting point: ${request.latitude}, ${request.longitude}, name: ${locationName ?: "Point de départ"}")
                
                // Store selected transportation modes
                _selectedTransportationModes.value = selectedModes
                android.util.Log.d("RouteSelectionViewModel", "Stored selected transportation modes: $selectedModes")
                
                val generatedRoutes = routesRepository.generateRoutes(request)
                android.util.Log.d("RouteSelectionViewModel", "Received ${generatedRoutes.size} routes from repository")
                android.util.Log.d("RouteSelectionViewModel", "Routes: ${generatedRoutes.map { it.id to it.name }}")
                _routes.value = generatedRoutes
                
                // Routes are NOT auto-saved - they remain in memory (cached) for the "Tous" tab
                // Only routes that are liked (heart clicked) will be saved to the database for the "Likes" tab
                
                android.util.Log.d("RouteSelectionViewModel", "Updated _routes StateFlow with ${_routes.value.size} routes")
            } catch (e: Exception) {
                android.util.Log.e("RouteSelectionViewModel", "Error generating routes", e)
                _error.value = e.message ?: "Error generating routes"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun saveRoute(route: RouteResponse, userId: String? = null) {
        viewModelScope.launch {
            try {
                android.util.Log.d("RouteSelectionViewModel", "Saving route ${route.id} with ${route.steps.size} steps")
                val savedRoute = routesRepository.saveRoute(route, userId)
                if (savedRoute != null) {
                    android.util.Log.d("RouteSelectionViewModel", "Route saved successfully: ${savedRoute.id}")
                } else {
                    android.util.Log.w("RouteSelectionViewModel", "Route save returned null")
                }
            } catch (e: Exception) {
                android.util.Log.e("RouteSelectionViewModel", "Error saving route", e)
                _error.value = "Error saving route: ${e.message}"
            }
        }
    }
    
    fun toggleLike(route: RouteResponse, userId: String? = null) {
        viewModelScope.launch {
            try {
                android.util.Log.d("RouteSelectionViewModel", "=== TOGGLE LIKE STARTED ===")
                android.util.Log.d("RouteSelectionViewModel", "Route ID: ${route.id}, Route name: ${route.name}")
                android.util.Log.d("RouteSelectionViewModel", "Current isFavorite: ${route.isFavorite}")
                android.util.Log.d("RouteSelectionViewModel", "UserId: $userId")
                
                val finalUserId = userId ?: "anonymous"
                val currentFavoriteStatus = route.isFavorite ?: false
                val newFavoriteStatus = !currentFavoriteStatus
                
                android.util.Log.d("RouteSelectionViewModel", "Toggling like for route ${route.id} with userId: $finalUserId")
                android.util.Log.d("RouteSelectionViewModel", "Current isFavorite: $currentFavoriteStatus -> New isFavorite: $newFavoriteStatus")
                
                // Save the route with the new isFavorite status
                val routeWithNewStatus = route.copy(isFavorite = newFavoriteStatus)
                android.util.Log.d("RouteSelectionViewModel", "Saving route ${route.id} with isFavorite: $newFavoriteStatus")
                val savedRoute = routesRepository.saveRoute(routeWithNewStatus, userId)
                
                if (savedRoute != null) {
                    android.util.Log.d("RouteSelectionViewModel", "Route ${route.id} saved successfully, isFavorite: ${savedRoute.isFavorite}")
                } else {
                    android.util.Log.e("RouteSelectionViewModel", "Route ${route.id} save FAILED - returned null")
                    _error.value = "Erreur lors de la sauvegarde de l'itinéraire"
                    return@launch
                }
                
                // Update the route in the local list to reflect like status change
                _routes.value = _routes.value.map { r ->
                    if (r.id == route.id) {
                        val updated = r.copy(isFavorite = newFavoriteStatus)
                        android.util.Log.d("RouteSelectionViewModel", "Updated route ${r.id} in local list, isFavorite: ${updated.isFavorite}")
                        updated
                    } else {
                        r
                    }
                }
                
                android.util.Log.d("RouteSelectionViewModel", "Route ${route.id} like status updated to: $newFavoriteStatus")
                android.util.Log.d("RouteSelectionViewModel", "=== TOGGLE LIKE COMPLETED ===")
            } catch (e: Exception) {
                android.util.Log.e("RouteSelectionViewModel", "Error toggling like", e)
                e.printStackTrace()
                _error.value = "Error toggling like: ${e.message}"
            }
        }
    }
}

class RouteSelectionViewModelFactory(
    private val routesRepository: RoutesRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RouteSelectionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RouteSelectionViewModel(routesRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}


