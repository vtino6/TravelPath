package com.tino.travelpath.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tino.travelpath.data.api.dto.RouteRequest
import com.tino.travelpath.data.database.entities.Activite
import com.tino.travelpath.data.database.entities.Lieu
import com.tino.travelpath.data.database.entities.TransportationMode
import com.tino.travelpath.data.repository.PlacesRepository
import com.tino.travelpath.data.repository.RoutesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PreferencesViewModel(
    private val routesRepository: RoutesRepository = RoutesRepository(),
    private val placesRepository: PlacesRepository
) : ViewModel() {
    
    private val _selectedActivities = MutableStateFlow<Set<Activite>>(emptySet())
    val selectedActivities: StateFlow<Set<Activite>> = _selectedActivities.asStateFlow()
    
    private val _budget = MutableStateFlow(50f)
    val budget: StateFlow<Float> = _budget.asStateFlow()
    
    private val _numberOfPlaces = MutableStateFlow(1)
    val numberOfPlaces: StateFlow<Int> = _numberOfPlaces.asStateFlow()
    
    private val _selectedTransportationModes = MutableStateFlow<Set<TransportationMode>>(emptySet())
    val selectedTransportationModes: StateFlow<Set<TransportationMode>> = _selectedTransportationModes.asStateFlow()
    
    private val _coldSensitivity = MutableStateFlow(0)
    val coldSensitivity: StateFlow<Int> = _coldSensitivity.asStateFlow()
    
    private val _heatSensitivity = MutableStateFlow(0)
    val heatSensitivity: StateFlow<Int> = _heatSensitivity.asStateFlow()
    
    private val _humiditySensitivity = MutableStateFlow(0)
    val humiditySensitivity: StateFlow<Int> = _humiditySensitivity.asStateFlow()
    
    private val _location = MutableStateFlow<Pair<Double, Double>?>(null)
    val location: StateFlow<Pair<Double, Double>?> = _location.asStateFlow()
    
    private val _locationName = MutableStateFlow<String?>(null)
    val locationName: StateFlow<String?> = _locationName.asStateFlow()
    
    private val _selectedPlaces = MutableStateFlow<Set<String>>(emptySet())
    val selectedPlaces: StateFlow<Set<String>> = _selectedPlaces.asStateFlow()
    
    private val _availablePlaces = MutableStateFlow<List<Lieu>>(emptyList())
    val availablePlaces: StateFlow<List<Lieu>> = _availablePlaces.asStateFlow()
    
    private val _isLoadingPlaces = MutableStateFlow(false)
    val isLoadingPlaces: StateFlow<Boolean> = _isLoadingPlaces.asStateFlow()
    
    fun toggleActivity(activity: Activite) {
        val current = _selectedActivities.value.toMutableSet()
        if (current.contains(activity)) {
            current.remove(activity)
            android.util.Log.d("PreferencesViewModel", "Removed activity: $activity")
        } else {
            current.add(activity)
            android.util.Log.d("PreferencesViewModel", "Added activity: $activity")
        }
        _selectedActivities.value = current
        android.util.Log.d("PreferencesViewModel", "Selected activities now: ${_selectedActivities.value}")
        loadPlacesForSelectedActivities()
    }
    
    fun setBudget(value: Float) {
        _budget.value = value
    }
    
    fun setNumberOfPlaces(value: Int) {
        _numberOfPlaces.value = value.coerceIn(1, Int.MAX_VALUE)
    }
    
    fun toggleTransportationMode(mode: TransportationMode) {
        val current = _selectedTransportationModes.value.toMutableSet()
        if (current.contains(mode)) {
            current.remove(mode)
        } else {
            current.add(mode)
        }
        _selectedTransportationModes.value = current
    }
    
    fun setColdSensitivity(value: Int) {
        _coldSensitivity.value = value
    }
    
    fun setHeatSensitivity(value: Int) {
        _heatSensitivity.value = value
    }
    
    fun setHumiditySensitivity(value: Int) {
        _humiditySensitivity.value = value
    }
    
    fun setLocation(latitude: Double, longitude: Double, name: String? = null) {
        _location.value = Pair(latitude, longitude)
        _locationName.value = name ?: "Localisation sélectionnée"
        loadPlacesForSelectedActivities()
    }
    
    fun clearLocation() {
        _location.value = null
        _locationName.value = null
    }
    
    fun togglePlaceSelection(placeId: String) {
        val current = _selectedPlaces.value.toMutableSet()
        if (current.contains(placeId)) {
            current.remove(placeId)
        } else {
            current.add(placeId)
        }
        _selectedPlaces.value = current
    }
    
    fun loadPlacesForSelectedActivities() {
        if (_selectedActivities.value.isEmpty()) {
            android.util.Log.d("PreferencesViewModel", "No activities selected, clearing places")
            _availablePlaces.value = emptyList()
            _isLoadingPlaces.value = false
            return
        }
        
        viewModelScope.launch {
            _isLoadingPlaces.value = true
            android.util.Log.d("PreferencesViewModel", "Loading places for activities: ${_selectedActivities.value}")
            try {
                val allPlaces = mutableListOf<Lieu>()
                val location = _location.value
                if (location == null) {
                    android.util.Log.w("PreferencesViewModel", "No location set, cannot load places")
                    _availablePlaces.value = emptyList()
                    _isLoadingPlaces.value = false
                    return@launch
                }
                android.util.Log.d("PreferencesViewModel", "Searching at location: ${location.first}, ${location.second}")
                
                _selectedActivities.value.forEach { activity ->
                    try {
                        android.util.Log.d("PreferencesViewModel", "Loading places for activity: $activity")
                        val places = placesRepository.searchPlaces(
                            latitude = location.first,
                            longitude = location.second,
                            category = activity,
                            forceRefresh = true
                        ).first()
                        android.util.Log.d("PreferencesViewModel", "Found ${places.size} places for $activity")
                        allPlaces.addAll(places)
                    } catch (e: Exception) {
                        android.util.Log.e("PreferencesViewModel", "Error loading places for $activity: ${e.message}", e)
                    }
                }
                
                _availablePlaces.value = allPlaces.distinctBy { it.id }
                
                android.util.Log.d("PreferencesViewModel", "Total loaded: ${_availablePlaces.value.size} places for ${_selectedActivities.value.size} activities")
            } catch (e: Exception) {
                android.util.Log.e("PreferencesViewModel", "Error loading places: ${e.message}", e)
                _availablePlaces.value = emptyList()
            } finally {
                _isLoadingPlaces.value = false
            }
        }
    }
    
    fun buildRouteRequest(): RouteRequest {
        android.util.Log.d("PreferencesViewModel", "Building RouteRequest...")
        android.util.Log.d("PreferencesViewModel", "Selected activities: ${_selectedActivities.value}")
        
        val backendActivities = _selectedActivities.value.map { activity ->
            when (activity) {
                Activite.RESTAURATION -> "RESTAURANT"
                Activite.LOISIRS -> "LEISURE"
                Activite.DECOUVERTE -> "DISCOVERY"
                Activite.CULTURE -> "CULTURE"
            }
        }
        
        android.util.Log.d("PreferencesViewModel", "Mapped backend activities: $backendActivities")
        
        // Determine backend transportation mode from selected modes
        val backendTransportationMode = when {
            _selectedTransportationModes.value.isEmpty() -> "MIXED" // Default if none selected
            _selectedTransportationModes.value.size == 1 -> {
                // Single mode selected
                when (_selectedTransportationModes.value.first()) {
                    TransportationMode.WALKING -> "WALKING"
                    TransportationMode.BICYCLE -> "BICYCLE"
                    TransportationMode.PUBLIC_TRANSPORT -> "PUBLIC_TRANSPORT"
                    TransportationMode.CAR -> "CAR"
                    TransportationMode.MIXED -> "MIXED"
                }
            }
            else -> "MIXED" // Multiple modes selected = MIXED
        }
        
        val location = _location.value
        if (location == null) {
            throw IllegalStateException("Location must be set before generating routes")
        }
        
        val latitude = location.first
        val longitude = location.second
        val locationName = _locationName.value ?: "Localisation sélectionnée"
        
        android.util.Log.d("PreferencesViewModel", "Building RouteRequest with location: $locationName ($latitude, $longitude)")
        
        val request = RouteRequest(
            latitude = latitude,
            longitude = longitude,
            activities = backendActivities,
            maxBudget = _budget.value.toDouble(),
            numberOfPlaces = _numberOfPlaces.value,
            transportationMode = backendTransportationMode,
            coldSensitivity = _coldSensitivity.value,
            heatSensitivity = _heatSensitivity.value,
            humiditySensitivity = _humiditySensitivity.value,
            requiredPlaceIds = _selectedPlaces.value.toList()
        )
        
        android.util.Log.d("PreferencesViewModel", "RouteRequest built:")
        android.util.Log.d("PreferencesViewModel", "  Location: $locationName ($latitude, $longitude)")
        android.util.Log.d("PreferencesViewModel", "  Activities: ${request.activities.size}")
        android.util.Log.d("PreferencesViewModel", "  Budget: ${request.maxBudget}€")
        android.util.Log.d("PreferencesViewModel", "  Number of Places: ${request.numberOfPlaces}")
        
        return request
    }
    
    /**
     * Estimate total cost based on number of places, selected activities, and transportation mode
     * Uses category-based average costs + transportation costs
     */
    fun estimateCost(numberOfPlaces: Int): CostEstimate {
        if (_selectedActivities.value.isEmpty()) {
            return CostEstimate(0.0, emptyMap(), 0.0)
        }
        
        // Category-based average costs (in euros)
        val categoryAverages = mapOf(
            "RESTAURANT" to 20.0,  // Average restaurant cost
            "CULTURE" to 10.0,     // Museums, galleries, etc.
            "LEISURE" to 15.0,     // Parks, activities, etc.
            "DISCOVERY" to 12.0    // Tourist spots, viewpoints, etc.
        )
        
        // Count places per category (distribute evenly)
        val activityCount = _selectedActivities.value.size
        val placesPerCategory = numberOfPlaces.toDouble() / activityCount
        
        val breakdown = mutableMapOf<String, Double>()
        var totalEstimate = 0.0
        
        _selectedActivities.value.forEach { activity ->
            val category = when (activity) {
                Activite.RESTAURATION -> "RESTAURANT"
                Activite.CULTURE -> "CULTURE"
                Activite.LOISIRS -> "LEISURE"
                Activite.DECOUVERTE -> "DISCOVERY"
            }
            val avgCost = categoryAverages[category] ?: 15.0
            val categoryTotal = placesPerCategory * avgCost
            breakdown[category] = categoryTotal
            totalEstimate += categoryTotal
        }
        
        // Estimate transportation costs based on mode
        // Assume average distance of 2km between places
        val averageDistancePerSegment = 2.0 // km
        val numberOfSegments = (numberOfPlaces - 1).coerceAtLeast(0)
        // Use MIXED if multiple modes selected, otherwise use the single selected mode
        val modeForEstimate = when {
            _selectedTransportationModes.value.isEmpty() -> TransportationMode.MIXED
            _selectedTransportationModes.value.size == 1 -> _selectedTransportationModes.value.first()
            else -> TransportationMode.MIXED // Multiple modes = MIXED
        }
        val transportationCost = estimateTransportationCost(
            mode = modeForEstimate,
            numberOfSegments = numberOfSegments,
            averageDistancePerSegment = averageDistancePerSegment
        )
        
        return CostEstimate(totalEstimate, breakdown, transportationCost)
    }
    
    /**
     * Estimate transportation cost based on mode and number of segments
     */
    private fun estimateTransportationCost(
        mode: TransportationMode,
        numberOfSegments: Int,
        averageDistancePerSegment: Double
    ): Double {
        if (numberOfSegments <= 0) return 0.0
        
        return when (mode) {
            TransportationMode.WALKING -> 0.0 // Free
            TransportationMode.BICYCLE -> 0.0 // Free
            TransportationMode.PUBLIC_TRANSPORT -> {
                // 2.50€ per trip (city-dependent, using average)
                2.50 * numberOfSegments
            }
            TransportationMode.CAR -> {
                // Fuel cost: ~0.10€/km + parking: ~3€ per place
                val fuelCost = (numberOfSegments * averageDistancePerSegment) * 0.10
                val parkingCost = numberOfSegments * 3.0 // Assume parking at each destination
                fuelCost + parkingCost
            }
            TransportationMode.MIXED -> {
                // Smart selection: mix of walking (free) and public transport
                // Assume 50% walking, 50% public transport
                val walkingSegments = (numberOfSegments * 0.5).toInt()
                val transitSegments = numberOfSegments - walkingSegments
                transitSegments * 2.50
            }
        }
    }
    
    data class CostEstimate(
        val totalCost: Double,
        val breakdown: Map<String, Double>, // Category -> estimated cost
        val transportationCost: Double = 0.0 // Transportation cost estimate
    ) {
        val grandTotal: Double
            get() = totalCost + transportationCost
    }
}

class PreferencesViewModelFactory(
    private val placesRepository: PlacesRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PreferencesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PreferencesViewModel(placesRepository = placesRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
