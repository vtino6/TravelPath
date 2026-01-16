package com.tino.travelpath.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.*

data class Directions(
    val distance: Double,
    val duration: Int,
    val polyline: String
)

class NavigationService {
    
    suspend fun getDirections(
        from: Coordinates,
        to: Coordinates
    ): Directions = withContext(Dispatchers.IO) {
        Directions(
            distance = calculateDistance(from, to),
            duration = (calculateDistance(from, to) * 12).toInt(),
            polyline = ""
        )
    }
    
    suspend fun calculateDistance(
        from: Coordinates,
        to: Coordinates
    ): Double = withContext(Dispatchers.Default) {
        val earthRadius = 6371.0
        val dLat = Math.toRadians(to.latitude - from.latitude)
        val dLon = Math.toRadians(to.longitude - from.longitude)
        
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(from.latitude)) * cos(Math.toRadians(to.latitude)) *
                sin(dLon / 2) * sin(dLon / 2)
        
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        earthRadius * c
    }
    
    suspend fun calculateDuration(
        from: Coordinates,
        to: Coordinates
    ): Int = withContext(Dispatchers.Default) {
        val distance = calculateDistance(from, to)
        (distance * 12).toInt()
    }
}
