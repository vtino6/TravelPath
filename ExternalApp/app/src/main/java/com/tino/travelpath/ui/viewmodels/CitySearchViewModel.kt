package com.tino.travelpath.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tino.travelpath.data.model.Photo
import com.tino.travelpath.data.repository.PhotoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class CitySearchViewModel(
    private val photoRepository: PhotoRepository = PhotoRepository()
) : ViewModel() {
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _cities = MutableStateFlow<List<String>>(emptyList())
    val cities: StateFlow<List<String>> = _cities.asStateFlow()
    
    private val _selectedCity = MutableStateFlow<String?>(null)
    val selectedCity: StateFlow<String?> = _selectedCity.asStateFlow()
    
    private val _photos = MutableStateFlow<List<Photo>>(emptyList())
    val photos: StateFlow<List<Photo>> = _photos.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    init {
        // Debounce search query to avoid too many API calls
        _searchQuery
            .debounce(300) // Wait 300ms after user stops typing
            .onEach { query ->
                if (query.isNotBlank()) {
                    searchCities(query)
                } else {
                    _cities.value = emptyList()
                }
            }
            .launchIn(viewModelScope)
    }
    
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        
        // If user is typing a new query that's different from selected city, clear selection
        if (selectedCity.value != null && query != selectedCity.value) {
            _selectedCity.value = null
            _photos.value = emptyList()
        }
        
        // If query is cleared, clear selection too
        if (query.isBlank()) {
            _selectedCity.value = null
            _photos.value = emptyList()
            _cities.value = emptyList()
        }
    }
    
    fun selectCity(cityName: String) {
        _selectedCity.value = cityName
        _searchQuery.value = cityName // Update search bar with selected city
        loadCityPhotos(cityName)
    }
    
    fun clearSelection() {
        _selectedCity.value = null
        _photos.value = emptyList()
        _searchQuery.value = ""
        _cities.value = emptyList()
    }
    
    private fun searchCities(query: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                photoRepository.searchCities(query)
                    .collect { matchingCities ->
                        _cities.value = matchingCities
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                android.util.Log.e("CitySearchViewModel", "Error searching cities: ${e.message}", e)
                _error.value = "Erreur lors de la recherche: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    fun loadCityPhotos(cityName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                photoRepository.getPhotosByCity(cityName)
                    .collect { photoList ->
                        _photos.value = photoList
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                android.util.Log.e("CitySearchViewModel", "Error loading photos for $cityName: ${e.message}", e)
                _error.value = "Erreur lors du chargement des photos: ${e.message}"
                _isLoading.value = false
            }
        }
    }
}
