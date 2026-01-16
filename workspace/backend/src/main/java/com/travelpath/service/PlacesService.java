package com.travelpath.service;

import com.travelpath.dto.PlaceResponse;
import com.travelpath.external.*;
import com.travelpath.model.Place;
import com.travelpath.model.PlaceCategory;
import com.travelpath.repository.PlaceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class PlacesService {
    
    @Autowired
    private PlaceRepository placeRepository;
    
    @Autowired
    private OverpassClient overpassClient;
    
    @Autowired(required = false)
    private YelpPlacesService yelpPlacesService;
    
    @Autowired(required = false)
    private GooglePlacesService googlePlacesService;
    
    @Value("${yelp.api.enabled:true}")
    private boolean yelpEnabled;
    
    @Value("${google.places.enabled:true}")
    private boolean googleEnabled;
    
    public List<PlaceResponse> searchNearby(
        double latitude,
        double longitude,
        int radiusMeters,
        PlaceCategory category
    ) {
        List<Place> cached = placeRepository.findNearbyByCategory(
            latitude, longitude, radiusMeters / 1000.0, category.name()
        );
        
        if (!cached.isEmpty()) {
            return cached.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        }
        
        // Use searchNearbyEntities which has the hybrid API logic
        List<Place> places = searchNearbyEntities(latitude, longitude, radiusMeters, category);
        
        return places.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }
    
    @Cacheable(value = "places", key = "#latitude + '_' + #longitude + '_' + #radiusMeters + '_' + #category.name()")
    public List<Place> searchNearbyEntities(
        double latitude,
        double longitude,
        int radiusMeters,
        PlaceCategory category
    ) {
        List<Place> cached = placeRepository.findNearbyByCategory(
            latitude, longitude, radiusMeters / 1000.0, category.name()
        );
        
        if (!cached.isEmpty()) {
            return cached;
        }
        
        // Hybrid approach: Use Yelp for restaurants, Google for others
        List<Place> places = new ArrayList<>();
        
        try {
            if (category == PlaceCategory.RESTAURANT && yelpEnabled && yelpPlacesService != null) {
                // Use Yelp for restaurants
                System.out.println("[PlacesService] Using Yelp API for restaurants");
                List<YelpBusiness> yelpBusinesses = yelpPlacesService.searchByCategory(
                    "RESTAURANT", latitude, longitude, radiusMeters
                );
                places = yelpBusinesses.stream()
                    .map(this::convertYelpToPlace)
                    .collect(Collectors.toList());
            } else if (googleEnabled && googlePlacesService != null) {
                // Use Google Places for other categories
                System.out.println("[PlacesService] Using Google Places API for category: " + category);
                List<GooglePlace> googlePlaces = googlePlacesService.searchByCategory(
                    category.name(), latitude, longitude, radiusMeters
                );
                places = googlePlaces.stream()
                    .map(this::convertGoogleToPlace)
                    .collect(Collectors.toList());
            }
            
            // Fallback to Overpass if APIs didn't return results
            if (places.isEmpty()) {
                System.out.println("[PlacesService] APIs returned no results, falling back to Overpass API");
                places = overpassClient.searchNearby(latitude, longitude, radiusMeters, category);
            }
            
        } catch (Exception e) {
            System.err.println("[PlacesService] Error with external APIs, falling back to Overpass: " + e.getMessage());
            places = overpassClient.searchNearby(latitude, longitude, radiusMeters, category);
        }
        
        System.out.println("[PlacesService] Found " + places.size() + " places for category: " + category);
        
        if (!places.isEmpty()) {
            System.out.println("[PlacesService] Saving " + places.size() + " places to database...");
            placeRepository.saveAll(places);
            placeRepository.flush();
            System.out.println("[PlacesService] Places saved successfully");
        }
        
        return places;
    }
    
    /**
     * Convert Yelp business to Place entity
     */
    private Place convertYelpToPlace(YelpBusiness yelp) {
        Place place = new Place();
        
        place.setId(yelp.getId() != null ? yelp.getId() : UUID.randomUUID().toString());
        place.setName(yelp.getName());
        place.setCategory(PlaceCategory.RESTAURANT);
        place.setLatitude(yelp.getLatitude() != null ? yelp.getLatitude() : 0.0);
        place.setLongitude(yelp.getLongitude() != null ? yelp.getLongitude() : 0.0);
        place.setAddress(yelp.getDisplayAddress() != null ? yelp.getDisplayAddress() : yelp.getAddress());
        place.setAverageCost(convertYelpPriceToCost(yelp.getPrice(), yelp.getLatitude(), yelp.getLongitude()));
        
        // Add rating info to description
        if (yelp.getRating() != null) {
            String ratingInfo = String.format("Rating: %.1f/5 (%d reviews)",
                yelp.getRating(),
                yelp.getReviewCount() != null ? yelp.getReviewCount() : 0);
            place.setDescription(ratingInfo);
        }
        
        place.setEstimatedWaitTime(0);
        place.setCreatedAt(LocalDateTime.now());
        place.setUpdatedAt(LocalDateTime.now());
        
        return place;
    }
    
    /**
     * Convert Google Place to Place entity
     */
    private Place convertGoogleToPlace(GooglePlace google) {
        Place place = new Place();
        
        place.setId(google.getId() != null ? google.getId() : UUID.randomUUID().toString());
        place.setName(google.getName());
        place.setCategory(determineCategoryFromGoogleTypes(google.getTypes()));
        place.setLatitude(google.getLatitude() != null ? google.getLatitude() : 0.0);
        place.setLongitude(google.getLongitude() != null ? google.getLongitude() : 0.0);
        place.setAddress(google.getAddress());
        place.setAverageCost(convertGooglePriceToCost(google.getPriceLevel(), google.getLatitude(), google.getLongitude()));
        
        // Add rating info to description
        if (google.getRating() != null) {
            String ratingInfo = String.format("Rating: %.1f/5 (%d reviews)",
                google.getRating(),
                google.getUserRatingCount() != null ? google.getUserRatingCount() : 0);
            place.setDescription(ratingInfo);
        }
        
        place.setEstimatedWaitTime(0);
        place.setCreatedAt(LocalDateTime.now());
        place.setUpdatedAt(LocalDateTime.now());
        
        return place;
    }
    
    /**
     * Convert Yelp price indicator to cost
     */
    private Double convertYelpPriceToCost(String yelpPrice, Double lat, Double lng) {
        if (yelpPrice == null || yelpPrice.isEmpty()) {
            return 30.0; // Default
        }
        
        double basePrice = switch(yelpPrice) {
            case "$", "€" -> 15.0;
            case "$$", "€€" -> 30.0;
            case "$$$", "€€€" -> 60.0;
            case "$$$$", "€€€€" -> 100.0;
            default -> 30.0;
        };
        
        // Apply city multiplier
        String city = getCityFromCoordinates(lat, lng);
        double cityMultiplier = getCityCostMultiplier(city);
        
        return basePrice * cityMultiplier;
    }
    
    /**
     * Convert Google price level to cost
     */
    private Double convertGooglePriceToCost(String priceLevel, Double lat, Double lng) {
        if (priceLevel == null || priceLevel.isEmpty()) {
            return 30.0; // Default
        }
        
        double basePrice = switch(priceLevel) {
            case "PRICE_LEVEL_FREE" -> 0.0;
            case "PRICE_LEVEL_INEXPENSIVE" -> 15.0;
            case "PRICE_LEVEL_MODERATE" -> 30.0;
            case "PRICE_LEVEL_EXPENSIVE" -> 60.0;
            case "PRICE_LEVEL_VERY_EXPENSIVE" -> 100.0;
            default -> 30.0;
        };
        
        // Apply city multiplier
        String city = getCityFromCoordinates(lat, lng);
        double cityMultiplier = getCityCostMultiplier(city);
        
        return basePrice * cityMultiplier;
    }
    
    /**
     * Get city from coordinates (simplified)
     */
    private String getCityFromCoordinates(Double lat, Double lng) {
        if (lat == null || lng == null) {
            return "Unknown";
        }
        
        if (lat > 48.8 && lat < 48.9 && lng > 2.2 && lng < 2.4) {
            return "Paris";
        }
        if (lat > 51.4 && lat < 51.6 && lng > -0.2 && lng < 0.1) {
            return "London";
        }
        if (lat > 40.6 && lat < 40.8 && lng > -74.1 && lng < -73.9) {
            return "New York";
        }
        
        return "Unknown";
    }
    
    /**
     * Get cost multiplier based on city
     */
    private double getCityCostMultiplier(String city) {
        return switch(city.toLowerCase()) {
            case "paris", "london", "new york" -> 1.5;
            case "lyon", "marseille" -> 1.0;
            default -> 0.7;
        };
    }
    
    /**
     * Determine category from Google Place types
     */
    private PlaceCategory determineCategoryFromGoogleTypes(List<String> googleTypes) {
        if (googleTypes == null || googleTypes.isEmpty()) {
            return PlaceCategory.DISCOVERY;
        }
        
        String typesStr = String.join(" ", googleTypes).toLowerCase();
        
        if (typesStr.contains("restaurant") || typesStr.contains("food") || typesStr.contains("cafe")) {
            return PlaceCategory.RESTAURANT;
        }
        
        if (typesStr.contains("museum") || typesStr.contains("art_gallery") || typesStr.contains("library")) {
            return PlaceCategory.CULTURE;
        }
        
        if (typesStr.contains("park") || typesStr.contains("zoo") || typesStr.contains("amusement_park")) {
            return PlaceCategory.LEISURE;
        }
        
        return PlaceCategory.DISCOVERY;
    }
    
    public PlaceResponse getPlaceDetails(String placeId) {
        Place place = placeRepository.findById(placeId)
            .orElseThrow(() -> new RuntimeException("Place not found: " + placeId));
        
        return toResponse(place);
    }
    
    private PlaceResponse toResponse(Place place) {
        return new PlaceResponse(
            place.getId(),
            place.getName(),
            place.getCategory(),
            place.getLatitude(),
            place.getLongitude(),
            place.getAddress(),
            place.getDescription(),
            place.getAverageCost(),
            place.getEstimatedWaitTime()
        );
    }
    
}

