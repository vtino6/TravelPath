package com.tino.travelpath.data.model

/**
 * Photo model matching Firestore structure from TravelShare
 * Handles both old and new data formats
 */
data class Photo(
    val id: String = "",
    val locationName: String? = null,  // City/location name (nullable - some photos don't have it)
    val imageUrl: String? = null,       // New format: imageUrl
    val url: String? = null,            // Old format: url
    val authorName: String? = null,    // Old format: authorName
    val userName: String? = null,       // New format: userName
    val title: String? = null,
    val date: String? = null,           // Old format: date string
    val timestamp: com.google.firebase.Timestamp? = null, // New format: timestamp
    val visibility: String? = null,
    val likesCount: Int = 0,
    val reportsCount: Int = 0
) {
    /**
     * Get the image URL (handles both formats)
     */
    fun getImageUrlOrUrl(): String? {
        return imageUrl ?: url
    }
    
    /**
     * Get the author name (handles both formats)
     */
    fun getAuthorNameOrUserName(): String? {
        return userName ?: authorName
    }
    
    /**
     * Check if photo has location information
     */
    fun hasLocation(): Boolean {
        return !locationName.isNullOrBlank()
    }
}
