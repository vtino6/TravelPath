package com.tino.travelpath.data.repository

import com.tino.travelpath.data.api.RetrofitClient
import com.tino.travelpath.data.api.dto.LoginRequest
import com.tino.travelpath.data.api.dto.UserRequest
import com.tino.travelpath.data.api.dto.UserResponse
import com.tino.travelpath.data.database.dao.UtilisateurDao
import com.tino.travelpath.data.database.entities.Utilisateur
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException

/**
 * Repository for user management - synchronizes local (Room) and remote (Spring Boot API)
 */
class UserRepository(
    private val utilisateurDao: UtilisateurDao,
    private val apiService: com.tino.travelpath.data.api.TravelPathApiService = RetrofitClient.apiService
) {
    
    /**
     * Register a new user (with password)
     */
    suspend fun registerUser(name: String, email: String, password: String): UserResponse {
        try {
            // 1. Register on backend
            val userResponse = apiService.registerUser(UserRequest(name, email, password))
            
            // 2. Save locally in Room (don't store password hash locally)
            val utilisateur = Utilisateur(
                id = userResponse.id,
                nom = userResponse.name,
                email = userResponse.email,
                password = null, // Don't store password locally
                dateCreation = System.currentTimeMillis(),
                dateModification = System.currentTimeMillis()
            )
            utilisateurDao.insert(utilisateur)
            
            return userResponse
        } catch (e: retrofit2.HttpException) {
            // Handle HTTP errors
            val errorBody = e.response()?.errorBody()?.string() ?: e.message()
            throw RuntimeException("Registration failed: $errorBody", e)
        } catch (e: Exception) {
            // Handle other errors
            throw RuntimeException("Registration failed: ${e.message}", e)
        }
    }
    
    /**
     * Login user (with email and password)
     */
    suspend fun loginUser(email: String, password: String): UserResponse {
        // 1. Login on backend
        val userResponse = apiService.loginUser(LoginRequest(email, password))
        
        // 2. Save locally in Room (don't store password hash locally)
        val utilisateur = Utilisateur(
            id = userResponse.id,
            nom = userResponse.name,
            email = userResponse.email,
            password = null, // Don't store password locally
            dateCreation = System.currentTimeMillis(),
            dateModification = System.currentTimeMillis()
        )
        utilisateurDao.insert(utilisateur)
        
        return userResponse
    }
    
    /**
     * Create or update user (for backward compatibility)
     */
    suspend fun createOrUpdateUser(name: String, email: String, password: String? = null): UserResponse {
        // 1. Create/update on backend
        val userResponse = apiService.createOrUpdateUser(UserRequest(name, email, password))
        
        // 2. Save locally in Room
        val utilisateur = Utilisateur(
            id = userResponse.id,
            nom = userResponse.name,
            email = userResponse.email,
            password = null, // Don't store password locally
            dateCreation = System.currentTimeMillis(),
            dateModification = System.currentTimeMillis()
        )
        utilisateurDao.insert(utilisateur)
        
        return userResponse
    }
    
    /**
     * Get user by ID (checks local first, then backend)
     */
    suspend fun getUserById(id: String): Utilisateur? {
        // Check local first
        val local = utilisateurDao.getById(id)
        if (local != null) {
            return local
        }
        
        // If not found locally, fetch from backend
        try {
            val remote = apiService.getUserById(id)
            val utilisateur = Utilisateur(
                id = remote.id,
                nom = remote.name,
                email = remote.email,
                dateCreation = System.currentTimeMillis(),
                dateModification = System.currentTimeMillis()
            )
            utilisateurDao.insert(utilisateur)
            return utilisateur
        } catch (e: Exception) {
            return null
        }
    }
    
    /**
     * Get user by email (checks local first, then backend)
     */
    suspend fun getUserByEmail(email: String): Utilisateur? {
        // Check local first
        val local = utilisateurDao.getByEmail(email)
        if (local != null) {
            return local
        }
        
        // If not found locally, fetch from backend
        try {
            val remote = apiService.getUserByEmail(email)
            val utilisateur = Utilisateur(
                id = remote.id,
                nom = remote.name,
                email = remote.email,
                dateCreation = System.currentTimeMillis(),
                dateModification = System.currentTimeMillis()
            )
            utilisateurDao.insert(utilisateur)
            return utilisateur
        } catch (e: Exception) {
            return null
        }
    }
    
    /**
     * Get all users (local only)
     */
    fun getAllUsers(): Flow<List<Utilisateur>> {
        return utilisateurDao.getAllFlow()
    }
    
    /**
     * Check if user exists on backend
     */
    suspend fun userExists(email: String): Boolean {
        return try {
            apiService.checkUserExists(email)
        } catch (e: Exception) {
            false
        }
    }
}

