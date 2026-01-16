package com.tino.travelpath.data.database.entities

enum class TransportationMode {
    WALKING,           // Walking only - Free
    BICYCLE,           // Bicycle only - Free
    PUBLIC_TRANSPORT,  // Public transportation only - ~2.50€ per trip
    CAR,               // Car only - Fuel + parking
    MIXED              // Mixed (smart selection) - App chooses optimal mode
}
