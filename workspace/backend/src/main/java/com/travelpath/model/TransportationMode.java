package com.travelpath.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TransportationMode {
    WALKING,           // Walking only - Free
    BICYCLE,           // Bicycle only - Free
    PUBLIC_TRANSPORT,  // Public transportation only - ~2.50€ per trip
    CAR,               // Car only - Fuel + parking
    MIXED;             // Mixed (smart selection) - App chooses optimal mode
    
    @JsonCreator
    public static TransportationMode fromString(String value) {
        if (value == null || value.isEmpty()) {
            return MIXED; // Default to MIXED if null or empty
        }
        try {
            return TransportationMode.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            System.err.println("WARNING: Invalid TransportationMode value: " + value + ", defaulting to MIXED");
            return MIXED;
        }
    }
    
    @JsonValue
    public String toValue() {
        return this.name();
    }
}
