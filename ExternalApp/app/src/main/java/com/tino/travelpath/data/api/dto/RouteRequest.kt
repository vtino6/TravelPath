package com.tino.travelpath.data.api.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO for route generation request
 */
data class RouteRequest(
    val latitude: Double,
    val longitude: Double,
    val activities: List<String>, // RESTAURANT, LEISURE, DISCOVERY, CULTURE
    val maxBudget: Double?,
    val numberOfPlaces: Int, // Number of places user wants to visit
    val transportationMode: String, // WALKING, BICYCLE, PUBLIC_TRANSPORT, CAR, MIXED
    val coldSensitivity: Int = 0,
    val heatSensitivity: Int = 0,
    val humiditySensitivity: Int = 0,
    val requiredPlaceIds: List<String> = emptyList()
)





