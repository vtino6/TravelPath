package com.tino.travelpath.data.api

import com.tino.travelpath.data.api.dto.LoginRequest
import com.tino.travelpath.data.api.dto.PlaceResponse
import com.tino.travelpath.data.api.dto.RouteRequest
import com.tino.travelpath.data.api.dto.RouteResponse
import com.tino.travelpath.data.api.dto.UserRequest
import com.tino.travelpath.data.api.dto.UserResponse
import com.tino.travelpath.data.api.dto.WeatherResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit interface for calling Spring Boot backend API
 */
interface TravelPathApiService {
    
    /**
     * Test endpoint to verify backend connectivity
     * GET /api/routes/test
     */
    @GET("routes/test")
    suspend fun testBackend(): Map<String, String>
    
    /**
     * Search for nearby places
     * GET /api/places/search?lat={lat}&lng={lng}&radius={radius}&category={category}
     */
    @GET("places/search")
    suspend fun searchPlaces(
        @Query("lat") latitude: Double,
        @Query("lng") longitude: Double,
        @Query("radius") radius: Int = 5000,
        @Query("category") category: String
    ): List<PlaceResponse>
    
    /**
     * Get place details by ID
     * GET /api/places/{id}
     */
    @GET("places/{id}")
    suspend fun getPlaceDetails(
        @Path("id") placeId: String
    ): PlaceResponse
    
    /**
     * Get current weather
     * GET /api/weather?lat={lat}&lng={lng}
     */
    @GET("weather")
    suspend fun getCurrentWeather(
        @Query("lat") latitude: Double,
        @Query("lng") longitude: Double
    ): WeatherResponse
    
    /**
     * Get weather forecast
     * GET /api/weather/forecast?lat={lat}&lng={lng}
     */
    @GET("weather/forecast")
    suspend fun getWeatherForecast(
        @Query("lat") latitude: Double,
        @Query("lng") longitude: Double
    ): List<WeatherResponse>
    
    /**
     * Check weather suitability
     * GET /api/weather/check?lat={lat}&lng={lng}&coldSensitivity={cold}&heatSensitivity={heat}&humiditySensitivity={humidity}
     */
    @GET("weather/check")
    suspend fun checkWeatherSuitability(
        @Query("lat") latitude: Double,
        @Query("lng") longitude: Double,
        @Query("coldSensitivity") coldSensitivity: Int = 0,
        @Query("heatSensitivity") heatSensitivity: Int = 0,
        @Query("humiditySensitivity") humiditySensitivity: Int = 0
    ): Boolean
    
    /**
     * Generate routes based on preferences
     * POST /api/routes/generate
     */
    @POST("routes/generate")
    suspend fun generateRoutes(
        @Body request: RouteRequest
    ): List<RouteResponse>
    
    /**
     * Save a route
     * POST /api/routes/save?userId={userId}
     */
    @POST("routes/save")
    suspend fun saveRoute(
        @Body route: RouteResponse,
        @Query("userId") userId: String? = null
    ): RouteResponse
    
    /**
     * Get saved routes for a user
     * GET /api/routes/saved?userId={userId}
     */
    @GET("routes/saved")
    suspend fun getSavedRoutes(
        @Query("userId") userId: String? = null
    ): List<RouteResponse>
    
    /**
     * Get route details by ID
     * GET /api/routes/{id}
     */
    @GET("routes/{id}")
    suspend fun getRouteById(
        @Path("id") routeId: String
    ): RouteResponse
    
    /**
     * Toggle favorite status
     * POST /api/routes/{id}/favorite?userId={userId}
     */
    @POST("routes/{id}/favorite")
    suspend fun toggleFavorite(
        @Path("id") routeId: String,
        @Query("userId") userId: String
    )
    
    /**
     * Delete a saved route
     * DELETE /api/routes/{id}?userId={userId}
     */
    @DELETE("routes/{id}")
    suspend fun deleteRoute(
        @Path("id") routeId: String,
        @Query("userId") userId: String
    )
    
    /**
     * Register a new user
     * POST /api/users/register
     */
    @POST("users/register")
    suspend fun registerUser(
        @Body request: UserRequest
    ): UserResponse
    
    /**
     * Login user
     * POST /api/users/login
     */
    @POST("users/login")
    suspend fun loginUser(
        @Body request: LoginRequest
    ): UserResponse
    
    /**
     * Create or update a user (for backward compatibility)
     * POST /api/users
     */
    @POST("users")
    suspend fun createOrUpdateUser(
        @Body request: UserRequest
    ): UserResponse
    
    /**
     * Get user by ID
     * GET /api/users/{id}
     */
    @GET("users/{id}")
    suspend fun getUserById(
        @Path("id") userId: String
    ): UserResponse
    
    /**
     * Get user by email
     * GET /api/users/email/{email}
     */
    @GET("users/email/{email}")
    suspend fun getUserByEmail(
        @Path("email") email: String
    ): UserResponse
    
    /**
     * Check if user exists
     * GET /api/users/check?email={email}
     */
    @GET("users/check")
    suspend fun checkUserExists(
        @Query("email") email: String
    ): Boolean
}

