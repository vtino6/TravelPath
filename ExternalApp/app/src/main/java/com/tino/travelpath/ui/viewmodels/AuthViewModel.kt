package com.tino.travelpath.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tino.travelpath.data.database.TravelPathDatabase
import com.tino.travelpath.data.database.entities.Utilisateur
import com.tino.travelpath.data.repository.ProfileRepository
import com.tino.travelpath.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.content.Context

class AuthViewModel(
    private val userRepository: UserRepository,
    private val profileRepository: ProfileRepository? = null
) : ViewModel() {
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _currentUser = MutableStateFlow<Utilisateur?>(null)
    val currentUser: StateFlow<Utilisateur?> = _currentUser.asStateFlow()
    
    /**
     * Register a new user (with password)
     */
    fun register(name: String, email: String, password: String, onSuccess: (Utilisateur) -> Unit) {
        viewModelScope.launch {
            if (name.isBlank() || email.isBlank() || password.isBlank()) {
                _error.value = "Veuillez remplir tous les champs"
                return@launch
            }
            
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                _error.value = "Email invalide"
                return@launch
            }
            
            if (password.length < 6) {
                _error.value = "Le mot de passe doit contenir au moins 6 caractères"
                return@launch
            }
            
            _isLoading.value = true
            _error.value = null
            
            try {
                // Register user on backend and save locally
                val userResponse = userRepository.registerUser(name, email, password)
                
                // Create Utilisateur entity from response
                val user = Utilisateur(
                    id = userResponse.id,
                    nom = userResponse.name,
                    email = userResponse.email,
                    password = null,
                    dateCreation = System.currentTimeMillis(),
                    dateModification = System.currentTimeMillis()
                )
                
                // Create default profile and preferences for new users
                profileRepository?.createDefaultProfileIfNeeded(user.id, user.nom)
                
                _currentUser.value = user
                onSuccess(user)
            } catch (e: Exception) {
                android.util.Log.e("AuthViewModel", "Register error", e)
                _error.value = "Erreur: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Login user (with email and password)
     */
    fun login(email: String, password: String, onSuccess: (Utilisateur) -> Unit) {
        viewModelScope.launch {
            if (email.isBlank() || password.isBlank()) {
                _error.value = "Veuillez remplir tous les champs"
                return@launch
            }
            
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                _error.value = "Email invalide"
                return@launch
            }
            
            _isLoading.value = true
            _error.value = null
            
            try {
                // Login on backend and save locally
                val userResponse = userRepository.loginUser(email, password)
                
                // Create Utilisateur entity from response
                val user = com.tino.travelpath.data.database.entities.Utilisateur(
                    id = userResponse.id,
                    nom = userResponse.name,
                    email = userResponse.email,
                    password = null,
                    dateCreation = System.currentTimeMillis(),
                    dateModification = System.currentTimeMillis()
                )
                
                _currentUser.value = user
                onSuccess(user)
            } catch (e: Exception) {
                android.util.Log.e("AuthViewModel", "Login error", e)
                _error.value = "Email ou mot de passe incorrect: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Load current user from local database by email
     */
    suspend fun loadCurrentUserByEmail(email: String) {
        val user = userRepository.getUserByEmail(email)
        _currentUser.value = user
    }
    
    /**
     * Sign out
     */
    fun signOut() {
        _currentUser.value = null
    }
}

class AuthViewModelFactory(
    private val userRepository: UserRepository,
    private val profileRepository: ProfileRepository? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(userRepository, profileRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

