package com.tino.travelpath.data.api.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO for route response from Spring Boot backend
 */
data class RouteResponse(
    val id: String,
    val name: String,
    @SerializedName("routeType")
    val routeType: String, // ECONOMIC, BALANCED, COMFORT
    @SerializedName("totalBudget")
    val totalBudget: Double,
    @SerializedName("totalDuration")
    val totalDuration: Int, // in minutes
    @SerializedName("transportationMode")
    val transportationMode: String? = "MIXED", // WALKING, BICYCLE, PUBLIC_TRANSPORT, CAR, MIXED
    val city: String?,
    @SerializedName("isFavorite")
    val isFavorite: Boolean? = false,
    val steps: List<StepResponse>
)

data class StepResponse(
    val id: String,
    val order: Int,
    val place: PlaceResponse,
    @SerializedName("timeSlot")
    val timeSlot: String, // MORNING, AFTERNOON, EVENING
    @SerializedName("estimatedDuration")
    val estimatedDuration: Int, // in minutes
    @SerializedName("distanceFromPrevious")
    val distanceFromPrevious: Double?, // in km
    val cost: Double,
    val notes: String?
)

