package com.travelpath.external;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.List;

/**
 * Client for OpenRouteService API (FREE tier: 2,000 requests/day)
 * Sign up: https://openrouteservice.org/dev/#/signup
 * Documentation: https://openrouteservice.org/dev/#/api-docs
 */
@Component
public class OpenRouteServiceClient {
    
    @Value("${openrouteservice.api.key:}")
    private String apiKey;
    
    private final WebClient webClient;
    private static final String ORS_BASE_URL = "https://api.openrouteservice.org/v2";
    
    public OpenRouteServiceClient() {
        this.webClient = WebClient.builder()
            .baseUrl(ORS_BASE_URL)
            .build();
    }
    
    /**
     * Get distance matrix between multiple locations
     * FREE: 2,000 requests/day
     */
    public DistanceMatrixResponse getDistanceMatrix(
        List<Location> locations,
        String profile
    ) {
        // Check if API key is configured
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("your-openrouteservice-api-key")) {
            System.out.println("[OpenRouteService] WARNING: API key not configured. Using fallback distance calculation.");
            return null; // Will trigger fallback in RouteGeneratorService
        }
        
        try {
            System.out.println("[OpenRouteService] Getting distance matrix for " + locations.size() + " locations...");
            DistanceMatrixRequest request = new DistanceMatrixRequest();
            request.locations = locations;
            request.profile = profile;
            request.metrics = new String[]{"distance", "duration"};
            request.units = "km";
            
            DistanceMatrixResponse response = webClient.post()
                .uri("/matrix/" + profile)
                .header("Authorization", apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(DistanceMatrixResponse.class)
                .block();
            
            if (response != null && response.distances != null) {
                System.out.println("[OpenRouteService] Successfully retrieved distance matrix (" + 
                                 response.distances.length + "x" + response.distances[0].length + ")");
            } else {
                System.out.println("[OpenRouteService] WARNING: Distance matrix response is null or empty");
            }
            
            return response;
        } catch (WebClientResponseException e) {
            System.err.println("[OpenRouteService] ERROR calling API: " + e.getStatusCode() + " - " + e.getMessage());
            if (e.getStatusCode().value() == 401) {
                System.err.println("[OpenRouteService] Authentication failed. Check your API key.");
            }
            return null;
        } catch (Exception e) {
            System.err.println("[OpenRouteService] Unexpected error: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Get directions/route between two points
     * Returns polyline for map display
     */
    public DirectionsResponse getDirections(
        double fromLat, double fromLon,
        double toLat, double toLon,
        String profile
    ) {
        // Check if API key is configured
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("your-openrouteservice-api-key")) {
            System.out.println("[OpenRouteService] WARNING: API key not configured. Using fallback distance calculation.");
            return null; // Will trigger fallback in RouteGeneratorService
        }
        
        try {
            DirectionsResponse response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/directions/" + profile)
                    .queryParam("api_key", apiKey)
                    .queryParam("start", fromLon + "," + fromLat)
                    .queryParam("end", toLon + "," + toLat)
                    .build())
                .retrieve()
                .bodyToMono(DirectionsResponse.class)
                .block();
            
            if (response != null && response.routes != null && !response.routes.isEmpty()) {
                System.out.println("[OpenRouteService] Got directions: " + 
                                 response.routes.get(0).summary.distance + " km, " +
                                 (response.routes.get(0).summary.duration / 60) + " min");
            }
            
            return response;
        } catch (WebClientResponseException e) {
            System.err.println("[OpenRouteService] ERROR getting directions: " + e.getStatusCode() + " - " + e.getMessage());
            if (e.getStatusCode().value() == 401) {
                System.err.println("[OpenRouteService] Authentication failed. Check your API key.");
            }
            return null;
        } catch (Exception e) {
            System.err.println("[OpenRouteService] Unexpected error getting directions: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Get route with multiple waypoints (optimized)
     */
    public DirectionsResponse getRouteWithWaypoints(
        List<Location> waypoints,
        String profile
    ) {
        try {
            StringBuilder coordinates = new StringBuilder();
            for (Location loc : waypoints) {
                if (coordinates.length() > 0) coordinates.append("|");
                coordinates.append(loc.longitude).append(",").append(loc.latitude);
            }
            
            DirectionsResponse response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/directions/" + profile)
                    .queryParam("api_key", apiKey)
                    .queryParam("coordinates", coordinates.toString())
                    .build())
                .retrieve()
                .bodyToMono(DirectionsResponse.class)
                .block();
            
            return response;
        } catch (Exception e) {
            System.err.println("Error getting route with waypoints: " + e.getMessage());
            return null;
        }
    }
    
    // Data classes
    public static class Location {
        public double latitude;
        public double longitude;
        
        public Location(double lat, double lon) {
            this.latitude = lat;
            this.longitude = lon;
        }
    }
    
    private static class DistanceMatrixRequest {
        public List<Location> locations;
        public String profile;
        public String[] metrics;
        public String units;
    }
    
    public static class DistanceMatrixResponse {
        public double[][] distances; // in km
        public double[][] durations; // in seconds
    }
    
    public static class DirectionsResponse {
        public List<Route> routes;
    }
    
    public static class Route {
        public Summary summary;
        public Geometry geometry; // Encoded polyline
    }
    
    public static class Summary {
        public double distance; // in km
        public double duration; // in seconds
    }
    
    public static class Geometry {
        public String coordinates; // Encoded polyline string
    }
}

