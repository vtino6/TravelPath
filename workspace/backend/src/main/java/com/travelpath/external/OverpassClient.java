package com.travelpath.external;

import com.travelpath.model.Place;
import com.travelpath.model.PlaceCategory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class OverpassClient {
    
    private final WebClient webClient;
    private static final String OVERPASS_API_URL = "https://overpass-api.de/api/interpreter";
    
    public OverpassClient() {
        this.webClient = WebClient.builder()
            .baseUrl(OVERPASS_API_URL)
            .codecs(configurer -> configurer
                .defaultCodecs()
                .maxInMemorySize(10 * 1024 * 1024))
            .build();
    }
    
    public List<Place> searchNearby(
        double latitude,
        double longitude,
        int radiusMeters,
        PlaceCategory category
    ) {
        int initialRadius = Math.min(radiusMeters, 2000);
        
        try {
            return searchWithRadius(latitude, longitude, initialRadius, category);
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException.GatewayTimeout e) {
            System.err.println("[OverpassClient] ERROR: Gateway Timeout (504). Retrying with reduced radius...");
            return retryWithReducedRadius(latitude, longitude, initialRadius, category, 2);
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            if (e.getStatusCode().value() >= 500) {
                System.err.println("[OverpassClient] ERROR: Server error (" + e.getStatusCode() + "). Retrying with reduced radius...");
                return retryWithReducedRadius(latitude, longitude, initialRadius, category, 2);
            }
            System.err.println("[OverpassClient] ERROR calling Overpass API: " + e.getStatusCode() + " - " + e.getMessage());
            return new ArrayList<>();
        } catch (org.springframework.core.io.buffer.DataBufferLimitException e) {
            System.err.println("[OverpassClient] ERROR: Response too large. Retrying with reduced radius...");
            return retryWithReducedRadius(latitude, longitude, initialRadius, category, 2);
        } catch (Exception e) {
            System.err.println("[OverpassClient] ERROR calling Overpass API: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    private List<Place> searchWithRadius(
        double latitude,
        double longitude,
        int radiusMeters,
        PlaceCategory category
    ) {
        System.out.println("[OverpassClient] Searching for category: " + category + " at (" + latitude + ", " + longitude + ") within " + radiusMeters + "m");
        String overpassQuery = buildOverpassQuery(latitude, longitude, radiusMeters, category);
        System.out.println("[OverpassClient] Overpass query: " + overpassQuery);
        
        OverpassResponse response = webClient.post()
            .bodyValue(overpassQuery)
            .header("Content-Type", "text/plain")
            .retrieve()
            .bodyToMono(OverpassResponse.class)
            .block();
        
        if (response != null && response.elements != null) {
            System.out.println("[OverpassClient] Overpass API returned " + response.elements.size() + " elements");
            List<Place> places = response.elements.stream()
                .limit(500)
                .map(this::convertToPlace)
                .filter(p -> p != null)
                .collect(Collectors.toList());
            System.out.println("[OverpassClient] Converted to " + places.size() + " Place entities");
            return places;
        }
        
        System.out.println("[OverpassClient] Overpass API returned null or empty response");
        return new ArrayList<>();
    }
    
    /**
     * Retry search with progressively smaller radius
     */
    private List<Place> retryWithReducedRadius(
        double latitude,
        double longitude,
        int originalRadius,
        PlaceCategory category,
        int maxRetries
    ) {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            int reducedRadius = originalRadius / (attempt + 1); // Progressively smaller: original/2, original/3, etc.
            if (reducedRadius < 500) {
                // Don't go below 500m - too small to be useful
                System.err.println("[OverpassClient] Radius too small (" + reducedRadius + "m). Giving up.");
                return new ArrayList<>();
            }
            
            try {
                System.out.println("[OverpassClient] Retry attempt " + attempt + " with radius: " + reducedRadius + "m");
                return searchWithRadius(latitude, longitude, reducedRadius, category);
            } catch (org.springframework.web.reactive.function.client.WebClientResponseException.GatewayTimeout e) {
                System.err.println("[OverpassClient] Retry attempt " + attempt + " also timed out. Trying smaller radius...");
                // Continue to next retry
            } catch (Exception e) {
                System.err.println("[OverpassClient] Retry attempt " + attempt + " failed: " + e.getMessage());
                // Continue to next retry
            }
        }
        
        System.err.println("[OverpassClient] All retry attempts failed. Returning empty list.");
        return new ArrayList<>();
    }
    
    /**
     * Build Overpass QL query
     * Note: Overpass QL doesn't support OR in tag values
     * We need to use union() for multiple tag types
     * Limit results to prevent buffer overflow (max 1000 results per category)
     */
    private String buildOverpassQuery(double lat, double lon, int radius, PlaceCategory category) {
        List<String> tagQueries = mapCategoryToAmenities(category);
        
        // Reduce radius for large cities to prevent too many results and timeouts
        // Paris has thousands of restaurants, so we limit the search area
        int effectiveRadius = Math.min(radius, 2000); // Max 2km to prevent timeout and buffer overflow
        
        if (tagQueries.size() == 1) {
            // Single tag query - simple query with limit
            String tagQuery = tagQueries.get(0);
            // Parse tag query (format: "key=value")
            String[] parts = tagQuery.split("=");
            String key = parts[0];
            String value = parts[1];
            
            return String.format(
                "[out:json][timeout:10];" + // Reduced from 25 to 10 seconds to fail faster and retry
                "(" +
                "  node[\"%s\"=\"%s\"](around:%d,%f,%f);" +
                "  way[\"%s\"=\"%s\"](around:%d,%f,%f);" +
                "  relation[\"%s\"=\"%s\"](around:%d,%f,%f);" +
                ");" +
                "out center meta;", // Note: limit is not valid in out clause, we'll limit in Java code
                key, value, effectiveRadius, lat, lon,
                key, value, effectiveRadius, lat, lon,
                key, value, effectiveRadius, lat, lon
            );
        } else {
            // Multiple tag queries - use union with limit
            StringBuilder query = new StringBuilder("[out:json][timeout:10];("); // Reduced from 25 to 10 seconds
            for (String tagQuery : tagQueries) {
                // Parse tag query (format: "key=value")
                String[] parts = tagQuery.split("=");
                String key = parts[0];
                String value = parts[1];
                
                query.append(String.format(
                    "  node[\"%s\"=\"%s\"](around:%d,%f,%f);" +
                    "  way[\"%s\"=\"%s\"](around:%d,%f,%f);" +
                    "  relation[\"%s\"=\"%s\"](around:%d,%f,%f);",
                    key, value, effectiveRadius, lat, lon,
                    key, value, effectiveRadius, lat, lon,
                    key, value, effectiveRadius, lat, lon
                ));
            }
            query.append(");out center meta;"); // Note: limit is not valid in out clause, we'll limit in Java code
            return query.toString();
        }
    }
    
    /**
     * Map category to list of tag queries (Overpass QL format)
     * Note: Some categories use "amenity" key, others use "tourism" key
     */
    private List<String> mapCategoryToAmenities(PlaceCategory category) {
        return switch (category) {
            case RESTAURANT -> List.of("amenity=restaurant", "amenity=cafe", "amenity=fast_food");
            case LEISURE -> List.of("leisure=park", "amenity=zoo", "leisure=playground");
            case DISCOVERY -> List.of("tourism=attraction", "tourism=viewpoint", "tourism=information");
            case CULTURE -> List.of("tourism=museum", "amenity=arts_centre", "amenity=library", "amenity=theatre");
        };
    }
    
    private Place convertToPlace(OverpassElement element) {
        // Get coordinates from element or center
        Double lat = element.lat;
        Double lon = element.lon;
        
        if (lat == null && element.center != null) {
            lat = element.center.lat;
            lon = element.center.lon;
        }
        
        if (lat == null || lon == null || (lat == 0.0 && lon == 0.0)) {
            return null;
        }
        
        double latitude = lat;
        double longitude = lon;
        
        // Determine category from tags
        PlaceCategory category = determineCategory(element.tags);
        
        // Get name
        String name = element.tags.get("name");
        if (name == null || name.isEmpty()) {
            name = element.tags.getOrDefault("amenity", "Place");
        }
        
        // Get address
        String address = buildAddress(element.tags);
        
        // Estimate cost from tags (simplified)
        Double averageCost = estimateCost(element.tags);
        
        return new Place(
            "osm_" + element.id, // Use OSM ID as identifier
            name,
            category,
            latitude,
            longitude,
            address,
            element.tags.get("description"),
            averageCost,
            0, // coldImpact
            0, // heatImpact
            0, // humidityImpact
            null, // estimatedWaitTime
            null, // createdAt
            null  // updatedAt
        );
    }
    
    private PlaceCategory determineCategory(java.util.Map<String, String> tags) {
        String amenity = tags.get("amenity");
        String tourism = tags.get("tourism");
        String leisure = tags.get("leisure");
        
        // RESTAURANT: restaurants, cafes, fast food
        if ("restaurant".equals(amenity) || "cafe".equals(amenity) || "fast_food".equals(amenity)) {
            return PlaceCategory.RESTAURANT;
        }
        
        // CULTURE: museums, arts centres, libraries, theatres
        if ("museum".equals(amenity) || "museum".equals(tourism) || 
            "arts_centre".equals(amenity) || "library".equals(amenity) || 
            "theatre".equals(amenity)) {
            return PlaceCategory.CULTURE;
        }
        
        // LEISURE: parks, zoos, playgrounds
        if ("park".equals(leisure) || "zoo".equals(amenity) || 
            "playground".equals(leisure) || "sports_centre".equals(amenity)) {
            return PlaceCategory.LEISURE;
        }
        
        // DISCOVERY: attractions, viewpoints, information points
        String historic = tags.get("historic");
        if ("attraction".equals(tourism) || "viewpoint".equals(tourism) || 
            "information".equals(tourism) || (historic != null && !historic.isEmpty())) {
            return PlaceCategory.DISCOVERY;
        }
        
        // Default fallback
        return PlaceCategory.DISCOVERY;
    }
    
    private String buildAddress(java.util.Map<String, String> tags) {
        StringBuilder address = new StringBuilder();
        if (tags.containsKey("addr:street")) {
            address.append(tags.get("addr:street"));
        }
        if (tags.containsKey("addr:housenumber")) {
            address.append(" ").append(tags.get("addr:housenumber"));
        }
        if (tags.containsKey("addr:city")) {
            if (address.length() > 0) address.append(", ");
            address.append(tags.get("addr:city"));
        }
        return address.length() > 0 ? address.toString() : null;
    }
    
    private Double estimateCost(java.util.Map<String, String> tags) {
        // Simple cost estimation based on tags
        String amenity = tags.get("amenity");
        if ("fast_food".equals(amenity)) return 10.0;
        if ("cafe".equals(amenity)) return 15.0;
        if ("restaurant".equals(amenity)) {
            // Check for price indicators
            if (tags.containsKey("cuisine") && tags.get("cuisine").contains("fine")) {
                return 50.0;
            }
            return 25.0;
        }
        return null;
    }
    
    
    // Response classes
    private static class OverpassResponse {
        public List<OverpassElement> elements;
    }
    
    private static class OverpassElement {
        public String type;
        public Long id;
        public Double lat;
        public Double lon;
        public OverpassCenter center;
        public java.util.Map<String, String> tags;
    }
    
    private static class OverpassCenter {
        public Double lat;
        public Double lon;
    }
}

