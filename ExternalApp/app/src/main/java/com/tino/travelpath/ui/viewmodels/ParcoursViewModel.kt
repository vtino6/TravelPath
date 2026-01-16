package com.tino.travelpath.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tino.travelpath.data.database.entities.Parcours
import com.tino.travelpath.data.database.entities.Preferences
import com.tino.travelpath.data.repository.ParcoursRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ParcoursViewModel(
    private val repository: ParcoursRepository
) : ViewModel() {
    
    private val _parcours = MutableStateFlow<List<Parcours>>(emptyList())
    val parcours: StateFlow<List<Parcours>> = _parcours.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    fun generateRoutes(preferences: Preferences) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val routes = repository.generateRoutes(preferences)
                _parcours.value = routes
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun saveParcours(parcours: Parcours) {
        viewModelScope.launch {
            repository.saveParcours(parcours)
        }
    }
    
    fun toggleFavori(parcours: Parcours) {
        viewModelScope.launch {
            repository.updateFavori(parcours.id, !parcours.estFavori)
        }
    }
}

class ParcoursViewModelFactory(
    private val repository: ParcoursRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ParcoursViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ParcoursViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}





