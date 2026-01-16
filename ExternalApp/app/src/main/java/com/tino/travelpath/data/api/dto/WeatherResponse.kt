package com.tino.travelpath.data.api.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO for Weather API response from Spring Boot backend
 */
data class WeatherResponse(
    val temperature: Double,
    val condition: String,
    val description: String,
    val humidity: Int,
    @SerializedName("windSpeed")
    val windSpeed: Double,
    @SerializedName("feelsLike")
    val feelsLike: Double
)





