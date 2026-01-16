package com.travelpath.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Component
public class GooglePlacesService {
    
    @Value("${google.places.api.key}")
    private String apiKey;
    
    @Value("${google.places.enabled:true}")
    private boolean enabled;
    
    private static final String GOOGLE_PLACES_BASE_URL = "https://places.googleapis.com/v1/places:searchNearby";
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Search for places near a location
     */
    public List<GooglePlace> searchPlaces(double latitude, 
                                         double longitude, 
                                         int radius,
                                         List<String> types) {
        if (!enabled || apiKey == null || apiKey.isEmpty()) {
            return new ArrayList<>();
        }
        
        try {
            // Build request body
            String requestBody = buildSearchRequest(latitude, longitude, radius, types);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Goog-Api-Key", apiKey);
            headers.set("X-Goog-FieldMask", "places.id,places.displayName,places.location,places.priceLevel,places.rating,places.userRatingCount,places.formattedAddress,places.types");
            
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                GOOGLE_PLACES_BASE_URL,
                HttpMethod.POST,
                entity,
                String.class
            );
            
            return parseSearchResponse(response.getBody());
            
        } catch (Exception e) {
            System.err.println("Google Places API error: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    /**
     * Search by single category
     */
    public List<GooglePlace> searchByCategory(String category, 
                                             double latitude, 
                                             double longitude,
                                             int radius) {
        List<String> types = List.of(category);
        return searchPlaces(latitude, longitude, radius, types);
    }
    
    private String buildSearchRequest(double lat, double lng, int radius, List<String> types) {
        // Convert TravelPath categories to Google types
        List<String> googleTypes = types.stream()
            .map(this::mapCategoryToGoogleType)
            .filter(type -> !type.isEmpty())
            .toList();
        
        // Build JSON request
        try {
            return objectMapper.writeValueAsString(new SearchRequest(
                googleTypes,
                20, // maxResultCount
                new LocationRestriction(
                    new Circle(
                        new Center(lat, lng),
                        radius
                    )
                )
            ));
        } catch (Exception e) {
            // Fallback to manual JSON building
            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"includedTypes\": [");
            for (int i = 0; i < googleTypes.size(); i++) {
                json.append("\"").append(googleTypes.get(i)).append("\"");
                if (i < googleTypes.size() - 1) json.append(",");
            }
            json.append("],\n");
            json.append("  \"maxResultCount\": 20,\n");
            json.append("  \"locationRestriction\": {\n");
            json.append("    \"circle\": {\n");
            json.append("      \"center\": {\n");
            json.append("        \"latitude\": ").append(lat).append(",\n");
            json.append("        \"longitude\": ").append(lng).append("\n");
            json.append("      },\n");
            json.append("      \"radius\": ").append(radius).append("\n");
            json.append("    }\n");
            json.append("  }\n");
            json.append("}");
            return json.toString();
        }
    }
    
    private String mapCategoryToGoogleType(String travelPathCategory) {
        return switch(travelPathCategory.toUpperCase()) {
            case "RESTAURANT" -> "restaurant";
            case "CULTURE" -> "museum";
            case "LEISURE" -> "park";
            case "DISCOVERY" -> "tourist_attraction";
            default -> "point_of_interest";
        };
    }
    
    private List<GooglePlace> parseSearchResponse(String json) {
        List<GooglePlace> places = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode placesNode = root.get("places");
            
            if (placesNode != null && placesNode.isArray()) {
                for (JsonNode placeNode : placesNode) {
                    places.add(parsePlace(placeNode));
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing Google Places response: " + e.getMessage());
            e.printStackTrace();
        }
        return places;
    }
    
    private GooglePlace parsePlace(JsonNode node) {
        GooglePlace place = new GooglePlace();
        
        place.setId(node.has("id") ? node.get("id").asText() : null);
        
        if (node.has("displayName")) {
            JsonNode displayName = node.get("displayName");
            place.setName(displayName.has("text") ? displayName.get("text").asText() : null);
        }
        
        if (node.has("location")) {
            JsonNode location = node.get("location");
            place.setLatitude(location.has("latitude") ? location.get("latitude").asDouble() : null);
            place.setLongitude(location.has("longitude") ? location.get("longitude").asDouble() : null);
        }
        
        place.setAddress(node.has("formattedAddress") ? node.get("formattedAddress").asText() : null);
        place.setPriceLevel(node.has("priceLevel") ? node.get("priceLevel").asText() : null);
        place.setRating(node.has("rating") ? node.get("rating").asDouble() : null);
        place.setUserRatingCount(node.has("userRatingCount") ? node.get("userRatingCount").asInt() : 0);
        
        if (node.has("types")) {
            List<String> types = new ArrayList<>();
            for (JsonNode type : node.get("types")) {
                types.add(type.asText());
            }
            place.setTypes(types);
        }
        
        return place;
    }
    
    // Helper classes for JSON serialization
    private static class SearchRequest {
        public List<String> includedTypes;
        public int maxResultCount;
        public LocationRestriction locationRestriction;
        
        public SearchRequest(List<String> includedTypes, int maxResultCount, LocationRestriction locationRestriction) {
            this.includedTypes = includedTypes;
            this.maxResultCount = maxResultCount;
            this.locationRestriction = locationRestriction;
        }
    }
    
    private static class LocationRestriction {
        public Circle circle;
        
        public LocationRestriction(Circle circle) {
            this.circle = circle;
        }
    }
    
    private static class Circle {
        public Center center;
        public int radius;
        
        public Circle(Center center, int radius) {
            this.center = center;
            this.radius = radius;
        }
    }
    
    private static class Center {
        public double latitude;
        public double longitude;
        
        public Center(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }
}
