package com.tino.travelpath.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tino.travelpath.data.api.dto.RouteResponse
import com.tino.travelpath.data.repository.RoutesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SavedRoutesViewModel(
    private val routesRepository: RoutesRepository
) : ViewModel() {
    
    // Cached routes from previous searches (Tous tab)
    private val _cachedRoutes = MutableStateFlow<List<RouteResponse>>(emptyList())
    val cachedRoutes: StateFlow<List<RouteResponse>> = _cachedRoutes.asStateFlow()
    
    // Liked routes persisted in backend (Likes tab)
    private val _likedRoutes = MutableStateFlow<List<RouteResponse>>(emptyList())
    val likedRoutes: StateFlow<List<RouteResponse>> = _likedRoutes.asStateFlow()
    
    // Expose for debugging
    fun getCachedRoutesCount() = _cachedRoutes.value.size
    fun getLikedRoutesCount() = _likedRoutes.value.size
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _selectedFilter = MutableStateFlow<RouteFilter>(RouteFilter.TOUS)
    val selectedFilter: StateFlow<RouteFilter> = _selectedFilter.asStateFlow()
    
    init {
        loadLikedRoutes()
    }
    
    /**
     * Set cached routes from RouteSelectionViewModel (in-memory, not saved to DB)
     * Used for the "Tous" tab
     */
    fun setCachedRoutes(routes: List<RouteResponse>) {
        android.util.Log.d("SavedRoutesViewModel", "Setting ${routes.size} cached routes for Tous tab")
        _cachedRoutes.value = routes
    }
    
    /**
     * Load all saved routes from backend for the user (Tous tab - user-specific)
     * NOTE: This is no longer used - "Tous" tab now uses cached routes from RouteSelectionViewModel
     */
    fun loadSavedRoutes(userId: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                android.util.Log.d("SavedRoutesViewModel", "=== LOADING SAVED ROUTES (Tous tab) ===")
                android.util.Log.d("SavedRoutesViewModel", "UserId: $userId")
                val routes = routesRepository.getSavedRoutes(userId)
                android.util.Log.d("SavedRoutesViewModel", "Got ${routes.size} saved routes from backend for user")
                _cachedRoutes.value = routes
                android.util.Log.d("SavedRoutesViewModel", "=== LOADING SAVED ROUTES COMPLETED ===")
            } catch (e: Exception) {
                android.util.Log.e("SavedRoutesViewModel", "Error loading saved routes", e)
                _error.value = e.message ?: "Error loading saved routes"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Load liked routes - offline-first approach (Room first, then sync with backend)
     */
    fun loadLikedRoutes(userId: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                android.util.Log.d("SavedRoutesViewModel", "=== LOADING LIKED ROUTES (Offline-First) ===")
                android.util.Log.d("SavedRoutesViewModel", "UserId: $userId")
                
                // 1. Load from Room first (instant, offline access)
                val roomRoutes = routesRepository.getLikedRoutesFromRoom(userId)
                android.util.Log.d("SavedRoutesViewModel", "Loaded ${roomRoutes.size} liked routes from Room")
                _likedRoutes.value = roomRoutes
                
                // 2. Sync with backend in background (update Room if backend has newer data)
                try {
                    val backendRoutes = routesRepository.getSavedRoutes(userId)
                    android.util.Log.d("SavedRoutesViewModel", "Got ${backendRoutes.size} saved routes from backend")
                    
                    val likedBackendRoutes = backendRoutes.filter { it.isFavorite == true }
                    android.util.Log.d("SavedRoutesViewModel", "Filtered to ${likedBackendRoutes.size} liked routes from backend")
                    
                    // Update Room with any new/changed routes from backend
                    likedBackendRoutes.forEach { route ->
                        routesRepository.saveRouteToRoom(route, userId)
                    }
                    
                    // Update UI with backend data (may have newer timestamps)
                    _likedRoutes.value = likedBackendRoutes
                    android.util.Log.d("SavedRoutesViewModel", "Synced with backend, now showing ${likedBackendRoutes.size} liked routes")
                } catch (e: Exception) {
                    android.util.Log.w("SavedRoutesViewModel", "Error syncing with backend, using Room data: ${e.message}")
                    // Keep Room data if backend sync fails
                }
                
                android.util.Log.d("SavedRoutesViewModel", "=== LOADING LIKED ROUTES COMPLETED ===")
            } catch (e: Exception) {
                android.util.Log.e("SavedRoutesViewModel", "Error loading liked routes", e)
                e.printStackTrace()
                _error.value = e.message ?: "Error loading liked routes"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Delete a route
     */
    fun deleteRoute(routeId: String, userId: String? = null) {
        viewModelScope.launch {
            try {
                // Always delete from Room (for both liked and cached routes)
                routesRepository.deleteRouteFromRoom(routeId)
                
                // If userId is provided, also delete from backend
                if (userId != null) {
                    routesRepository.deleteRoute(routeId, userId)
                    // Reload liked routes
                    loadLikedRoutes(userId)
                } else {
                    // If no userId, just remove from cached routes
                    _cachedRoutes.value = _cachedRoutes.value.filter { it.id != routeId }
                }
            } catch (e: Exception) {
                _error.value = "Error deleting route: ${e.message}"
            }
        }
    }
    
    /**
     * Toggle like status - saves to backend and Room
     */
    fun toggleLike(routeId: String, userId: String? = null) {
        viewModelScope.launch {
            try {
                val finalUserId = userId ?: "anonymous"
                android.util.Log.d("SavedRoutesViewModel", "=== TOGGLE LIKE STARTED ===")
                android.util.Log.d("SavedRoutesViewModel", "Route ID: $routeId, UserId: $finalUserId")
                
                // Get current liked status before toggling
                val currentLikedRoutes = _likedRoutes.value
                val wasLiked = currentLikedRoutes.any { it.id == routeId }
                android.util.Log.d("SavedRoutesViewModel", "Route $routeId was liked: $wasLiked")
                
                // Find the route in cached routes to save it with updated isFavorite status
                val routeToSave = _cachedRoutes.value.find { it.id == routeId }
                if (routeToSave != null) {
                    android.util.Log.d("SavedRoutesViewModel", "Found route in cache, saving with toggled isFavorite")
                    try {
                        val newFavoriteStatus = !wasLiked
                        val routeWithNewStatus = routeToSave.copy(isFavorite = newFavoriteStatus)
                        
                        // Save to backend (which will also save to Room if liked)
                        val savedRoute = routesRepository.saveRoute(routeWithNewStatus, finalUserId)
                        if (savedRoute != null) {
                            android.util.Log.d("SavedRoutesViewModel", "Route saved successfully with isFavorite: ${savedRoute.isFavorite}")
                            
                            // If unliked, also remove from Room
                            if (!newFavoriteStatus) {
                                routesRepository.deleteRouteFromRoom(routeId)
                            }
                        } else {
                            android.util.Log.w("SavedRoutesViewModel", "Route save returned null")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("SavedRoutesViewModel", "Error saving route", e)
                        throw e
                    }
                } else {
                    android.util.Log.w("SavedRoutesViewModel", "Route $routeId not found in cached routes")
                    // If route not in cache, just use toggleFavorite as fallback
                    android.util.Log.d("SavedRoutesViewModel", "Calling toggleFavorite API as fallback")
                    routesRepository.toggleFavorite(routeId, finalUserId)
                }
                
                // Update cached routes immediately to reflect the change in UI
                val newFavoriteStatus = !wasLiked
                val updatedCachedRoutes = _cachedRoutes.value.map { route ->
                    if (route.id == routeId) {
                        route.copy(isFavorite = newFavoriteStatus)
                    } else {
                        route
                    }
                }
                _cachedRoutes.value = updatedCachedRoutes
                android.util.Log.d("SavedRoutesViewModel", "Updated cached routes, route $routeId isFavorite: $newFavoriteStatus")
                
                // Also update liked routes list immediately
                val updatedLikedRoutes = if (newFavoriteStatus) {
                    // Add to liked routes if it's now liked
                    val routeToAdd = updatedCachedRoutes.find { it.id == routeId }
                    if (routeToAdd != null && !_likedRoutes.value.any { it.id == routeId }) {
                        _likedRoutes.value + routeToAdd
                    } else {
                        _likedRoutes.value
                    }
                } else {
                    // Remove from liked routes if it's now unliked
                    _likedRoutes.value.filter { it.id != routeId }
                }
                _likedRoutes.value = updatedLikedRoutes
                android.util.Log.d("SavedRoutesViewModel", "Updated liked routes list, now has ${updatedLikedRoutes.size} liked routes")
                
                // Reload liked routes from Room/backend after a short delay
                kotlinx.coroutines.delay(300)
                loadLikedRoutes(userId)
                
                android.util.Log.d("SavedRoutesViewModel", "=== TOGGLE LIKE COMPLETED ===")
            } catch (e: Exception) {
                android.util.Log.e("SavedRoutesViewModel", "Error toggling like", e)
                e.printStackTrace()
                _error.value = "Error toggling like: ${e.message}"
            }
        }
    }
    
    /**
     * Set filter
     */
    fun setFilter(filter: RouteFilter) {
        _selectedFilter.value = filter
    }
    
    /**
     * Get filtered routes
     */
    fun getFilteredRoutes(): List<RouteResponse> {
        return when (_selectedFilter.value) {
            RouteFilter.TOUS -> _cachedRoutes.value
            RouteFilter.LIKES -> _likedRoutes.value
        }
    }
}

enum class RouteFilter {
    TOUS,
    LIKES
}

class SavedRoutesViewModelFactory(
    private val routesRepository: RoutesRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SavedRoutesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SavedRoutesViewModel(routesRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

