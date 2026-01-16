package com.tino.travelpath.data.api.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO for Place API response from Spring Boot backend
 */
data class PlaceResponse(
    val id: String,
    val name: String,
    val category: String, // RESTAURANT, LEISURE, DISCOVERY, CULTURE
    val latitude: Double,
    val longitude: Double,
    val address: String?,
    val description: String?,
    @SerializedName("averageCost")
    val averageCost: Double?,
    @SerializedName("estimatedWaitTime")
    val estimatedWaitTime: Int?
)


