package com.tino.travelpath.ui.utils

object ImageUtils {
    
    private val RESTAURANT_IMAGES = listOf(
        "https://picsum.photos/seed/restaurant1/800/600",
        "https://picsum.photos/seed/restaurant2/800/600",
        "https://picsum.photos/seed/restaurant3/800/600",
        "https://picsum.photos/seed/restaurant4/800/600",
        "https://picsum.photos/seed/restaurant5/800/600"
    )
    
    private val LEISURE_IMAGES = listOf(
        "https://picsum.photos/seed/leisure1/800/600",
        "https://picsum.photos/seed/leisure2/800/600",
        "https://picsum.photos/seed/leisure3/800/600",
        "https://picsum.photos/seed/leisure4/800/600",
        "https://picsum.photos/seed/leisure5/800/600"
    )
    
    private val CULTURE_IMAGES = listOf(
        "https://picsum.photos/seed/culture1/800/600",
        "https://picsum.photos/seed/culture2/800/600",
        "https://picsum.photos/seed/culture3/800/600",
        "https://picsum.photos/seed/culture4/800/600",
        "https://picsum.photos/seed/culture5/800/600"
    )
    
    private val DISCOVERY_IMAGES = listOf(
        "https://picsum.photos/seed/discovery1/800/600",
        "https://picsum.photos/seed/discovery2/800/600",
        "https://picsum.photos/seed/discovery3/800/600",
        "https://picsum.photos/seed/discovery4/800/600",
        "https://picsum.photos/seed/discovery5/800/600"
    )
    
    fun getImageUrlForCategory(category: String, placeId: String? = null, placeName: String? = null): String {
        val images = when (category.uppercase()) {
            "RESTAURANT" -> RESTAURANT_IMAGES
            "LEISURE" -> LEISURE_IMAGES
            "CULTURE" -> CULTURE_IMAGES
            "DISCOVERY" -> DISCOVERY_IMAGES
            else -> DISCOVERY_IMAGES
        }
        
        val hashValue = when {
            placeId != null -> placeId.hashCode()
            placeName != null -> placeName.hashCode()
            else -> 0
        }
        val index = Math.abs(hashValue) % images.size
        
        return images[index]
    }
}

