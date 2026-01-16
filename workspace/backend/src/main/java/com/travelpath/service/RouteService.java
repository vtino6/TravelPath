package com.travelpath.service;

import com.travelpath.dto.RouteResponse;
import com.travelpath.dto.StepResponse;
import com.travelpath.model.*;
import com.travelpath.repository.PlaceRepository;
import com.travelpath.repository.RouteRepository;
import com.travelpath.repository.StepRepository;
import com.travelpath.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class RouteService {
    
    @Autowired
    private RouteRepository routeRepository;
    
    @Autowired
    private StepRepository stepRepository;
    
    @Autowired
    private PlaceRepository placeRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private EntityManager entityManager;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public Route saveRoute(RouteResponse routeResponse, String userId) {
        try {
            // Check if route already exists
            Route route = routeRepository.findById(routeResponse.getId()).orElse(null);
            
            // If route exists, clear the session to detach any loaded steps that might cause conflicts
            if (route != null) {
                entityManager.detach(route);
                // Reload route without steps to avoid loading them into session
                route = routeRepository.findById(routeResponse.getId()).orElse(route);
            }
            
            System.out.println("=== SAVE ROUTE ===");
            System.out.println("Route ID: " + routeResponse.getId());
            System.out.println("Route name: " + routeResponse.getName());
            System.out.println("RouteResponse.isFavorite: " + routeResponse.getIsFavorite());
            System.out.println("UserId: " + userId);
            
            if (route == null) {
                // Create new route
                System.out.println("Creating new route");
                route = new Route();
                route.setId(routeResponse.getId());
                route.setName(routeResponse.getName());
                route.setRouteType(routeResponse.getRouteType());
                route.setTotalBudget(routeResponse.getTotalBudget());
                route.setTotalDuration(routeResponse.getTotalDuration());
                route.setTransportationMode(routeResponse.getTransportationMode());
                route.setCity(routeResponse.getCity());
                route.setIsSaved(true);
                // Preserve isFavorite if set, otherwise default to false
                route.setIsFavorite(routeResponse.getIsFavorite() != null ? routeResponse.getIsFavorite() : false);
                System.out.println("New route isFavorite set to: " + route.getIsFavorite());
            } else {
                // Update existing route
                System.out.println("Updating existing route, current isFavorite: " + route.getIsFavorite());
                route.setName(routeResponse.getName());
                route.setRouteType(routeResponse.getRouteType());
                route.setTotalBudget(routeResponse.getTotalBudget());
                route.setTotalDuration(routeResponse.getTotalDuration());
                route.setTransportationMode(routeResponse.getTransportationMode());
                route.setCity(routeResponse.getCity());
                route.setIsSaved(true);
                // Preserve isFavorite if set in request, otherwise keep existing value
                if (routeResponse.getIsFavorite() != null) {
                    route.setIsFavorite(routeResponse.getIsFavorite());
                    System.out.println("Updated isFavorite from request: " + route.getIsFavorite());
                } else {
                    System.out.println("Keeping existing isFavorite: " + route.getIsFavorite());
                }
            }
            
            if (userId != null && !userId.isEmpty() && !userId.equals("anonymous")) {
                // Always store userId as string for querying
                route.setUserIdString(userId);
                
                // Try to get the user from database
                java.util.Optional<User> userOpt = userRepository.findById(userId);
                if (userOpt.isPresent()) {
                    route.setUser(userOpt.get());
                    System.out.println("Set user for route: " + userId);
                } else {
                    System.out.println("User " + userId + " not found in database, storing userIdString only");
                    route.setUser(null);
                }
            } else {
                // Ensure user is null for anonymous routes
                route.setUser(null);
                route.setUserIdString(null);
            }
            
            // Serialize steps to JSON string if provided
            if (routeResponse.getSteps() != null && !routeResponse.getSteps().isEmpty()) {
                try {
                    String stepsJson = objectMapper.writeValueAsString(routeResponse.getSteps());
                    route.setStepsJson(stepsJson);
                    System.out.println("Serialized " + routeResponse.getSteps().size() + " steps to JSON");
                } catch (Exception e) {
                    System.err.println("Error serializing steps to JSON: " + e.getMessage());
                    e.printStackTrace();
                    throw new RuntimeException("Failed to serialize steps", e);
                }
            }
            
            Route savedRoute = routeRepository.save(route);
            System.out.println("Saved route " + savedRoute.getId() + " with isFavorite: " + savedRoute.getIsFavorite());
            
            System.out.println("=== SAVE ROUTE COMPLETED ===");
            return savedRoute;
        } catch (Exception e) {
            System.err.println("ERROR in saveRoute: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to save route: " + e.getMessage(), e);
        }
    }
    
    public List<RouteResponse> getSavedRoutes(String userId) {
        System.out.println("=== GET SAVED ROUTES ===");
        System.out.println("UserId: " + userId);
        
        List<Route> routes;
        
        if (userId == null || userId.isEmpty() || userId.equals("anonymous")) {
            // Get all saved routes without user association
            System.out.println("Getting routes for anonymous user (userId is null)");
            routes = routeRepository.findByUserIdIsNull();
            System.out.println("Found " + routes.size() + " routes with userId = null");
        } else {
            // Get saved routes for specific user (user-specific data)
            System.out.println("Getting routes for user: " + userId);
            routes = routeRepository.findSavedRoutesByUser(userId);
            System.out.println("Found " + routes.size() + " routes for user " + userId);
            
            // Log routes to verify they're user-specific
            routes.forEach(r -> {
                String routeUserId = r.getUserIdString() != null ? r.getUserIdString() : 
                    (r.getUser() != null ? r.getUser().getId() : "null");
                System.out.println("  Route " + r.getId() + " belongs to user: " + routeUserId);
            });
        }
        
        // Log all routes before filtering
        System.out.println("All routes before filtering:");
        routes.forEach(r -> System.out.println("  Route " + r.getId() + ": isSaved=" + r.getIsSaved() + ", isFavorite=" + r.getIsFavorite() + ", userId=" + (r.getUser() != null ? r.getUser().getId() : "null")));
        
        // Filter to only saved routes and convert to response
        List<RouteResponse> routeResponses = routes.stream()
            .filter(r -> r.getIsSaved() != null && r.getIsSaved())
            .map(this::convertToResponse)
            .collect(Collectors.toList());
        
        System.out.println("After filtering (isSaved=true), found " + routeResponses.size() + " routes");
        routeResponses.forEach(r -> System.out.println("  Route " + r.getId() + ": isFavorite=" + r.getIsFavorite() + ", name=" + r.getName()));
        System.out.println("=== GET SAVED ROUTES COMPLETED ===");
        
        return routeResponses;
    }
    
    public RouteResponse getRouteById(String routeId) {
        Route route = routeRepository.findById(routeId)
            .orElseThrow(() -> new RuntimeException("Route not found: " + routeId));
        
        return convertToResponse(route);
    }
    
    public void toggleFavorite(String routeId, String userId) {
        System.out.println("=== TOGGLE FAVORITE ===");
        System.out.println("Route ID: " + routeId);
        System.out.println("UserId: " + userId);
        
        Route route;
        
        try {
            // If userId is null or "anonymous", try to find route without user association
            if (userId == null || userId.isEmpty() || userId.equals("anonymous")) {
                System.out.println("Looking for route with userId = null");
                route = routeRepository.findById(routeId).orElse(null);
            } else {
                // Try to find route by ID and userId first
                System.out.println("Looking for route with userId: " + userId);
                route = routeRepository.findByIdAndUserId(routeId, userId)
                    .orElseGet(() -> {
                        System.out.println("Route not found with userId, trying to find by ID only");
                        // If not found, try to find by ID only (for routes that might not have user set)
                        return routeRepository.findById(routeId).orElse(null);
                    });
            }
            
            if (route == null) {
                System.out.println("Route not found: " + routeId + ". Route should be saved first via saveRoute endpoint.");
                throw new RuntimeException("Route not found: " + routeId + ". Please save the route first.");
            }
            
            System.out.println("Found route: " + route.getId() + ", current isFavorite: " + route.getIsFavorite());
            
            boolean currentFavorite = route.getIsFavorite() != null ? route.getIsFavorite() : false;
            route.setIsFavorite(!currentFavorite);
            route.setIsSaved(true); // Ensure route is marked as saved when favorited
            
            routeRepository.save(route);
            
            System.out.println("Toggled favorite for route " + routeId + " with userId: " + userId);
            System.out.println("Previous isFavorite: " + currentFavorite + ", New isFavorite: " + route.getIsFavorite());
            System.out.println("=== TOGGLE FAVORITE COMPLETED ===");
        } catch (Exception e) {
            System.err.println("ERROR in toggleFavorite: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    public void deleteRoute(String routeId, String userId) {
        Route route = routeRepository.findByIdAndUserId(routeId, userId)
            .orElseThrow(() -> new RuntimeException("Route not found: " + routeId));
        
        routeRepository.delete(route);
    }
    
    private Step convertToStep(StepResponse stepResponse, Route route) {
        Step step = new Step();
        step.setId(stepResponse.getId());
        step.setRoute(route);
        step.setOrder(stepResponse.getOrder());
        step.setTimeSlot(stepResponse.getTimeSlot());
        step.setEstimatedDuration(stepResponse.getEstimatedDuration());
        step.setDistanceFromPrevious(stepResponse.getDistanceFromPrevious() != null ? stepResponse.getDistanceFromPrevious() : null);
        step.setCost(stepResponse.getCost());
        step.setNotes(stepResponse.getNotes());
        
        // Get or create the place
        com.travelpath.dto.PlaceResponse placeResponse = stepResponse.getPlace();
        Place place = placeRepository.findById(placeResponse.getId())
            .orElseGet(() -> {
                // Create place if it doesn't exist
                System.out.println("Place " + placeResponse.getId() + " not found, creating it");
                Place newPlace = new Place();
                newPlace.setId(placeResponse.getId());
                
                // Required fields with null checks
                newPlace.setName(placeResponse.getName() != null && !placeResponse.getName().isEmpty() 
                    ? placeResponse.getName() : "Unknown Place");
                newPlace.setCategory(placeResponse.getCategory() != null 
                    ? placeResponse.getCategory() : PlaceCategory.DISCOVERY);
                newPlace.setLatitude(placeResponse.getLatitude() != null 
                    ? placeResponse.getLatitude() : 0.0);
                newPlace.setLongitude(placeResponse.getLongitude() != null 
                    ? placeResponse.getLongitude() : 0.0);
                
                // Optional fields
                newPlace.setAddress(placeResponse.getAddress());
                newPlace.setDescription(placeResponse.getDescription());
                newPlace.setAverageCost(placeResponse.getAverageCost());
                newPlace.setEstimatedWaitTime(placeResponse.getEstimatedWaitTime());
                
                // createdAt and updatedAt will be set by @PrePersist
                try {
                    return placeRepository.save(newPlace);
                } catch (Exception e) {
                    System.err.println("Error saving place " + placeResponse.getId() + ": " + e.getMessage());
                    e.printStackTrace();
                    throw new RuntimeException("Failed to create place: " + e.getMessage(), e);
                }
            });
        step.setPlace(place);
        
        return step;
    }
    
    public RouteResponse convertToResponse(Route route) {
        List<StepResponse> stepResponses = null;
        
        // Try to deserialize steps from JSON first
        if (route.getStepsJson() != null && !route.getStepsJson().isEmpty()) {
            try {
                stepResponses = objectMapper.readValue(
                    route.getStepsJson(),
                    new TypeReference<List<StepResponse>>() {}
                );
                System.out.println("Deserialized " + stepResponses.size() + " steps from JSON");
            } catch (Exception e) {
                System.err.println("Error deserializing steps from JSON: " + e.getMessage());
                e.printStackTrace();
                // Fall back to loading from Step entities if JSON deserialization fails
                stepResponses = null;
            }
        }
        
        // Fallback: Load steps from Step entities if JSON is not available
        if (stepResponses == null) {
            List<Step> steps = stepRepository.findByRouteIdOrderByOrderAsc(route.getId());
            stepResponses = steps.stream()
                .map(this::convertStepToResponse)
                .collect(Collectors.toList());
            System.out.println("Loaded " + stepResponses.size() + " steps from Step entities (fallback)");
        }
        
        // Ensure steps is never null
        if (stepResponses == null) {
            stepResponses = new ArrayList<>();
            System.out.println("[RouteService] WARNING: stepResponses was null, initializing empty list");
        }
        
        // Ensure transportationMode is never null
        com.travelpath.model.TransportationMode transportMode = route.getTransportationMode();
        if (transportMode == null) {
            transportMode = com.travelpath.model.TransportationMode.MIXED;
            System.out.println("[RouteService] WARNING: transportationMode was null, defaulting to MIXED");
        }
        
        return new RouteResponse(
            route.getId(),
            route.getName(),
            route.getRouteType(),
            route.getTotalBudget(),
            route.getTotalDuration(),
            transportMode,
            route.getCity(),
            route.getIsFavorite() != null ? route.getIsFavorite() : false,
            stepResponses
        );
    }
    
    private StepResponse convertStepToResponse(Step step) {
        com.travelpath.dto.PlaceResponse placeResponse = new com.travelpath.dto.PlaceResponse(
            step.getPlace().getId(),
            step.getPlace().getName(),
            step.getPlace().getCategory(),
            step.getPlace().getLatitude(),
            step.getPlace().getLongitude(),
            step.getPlace().getAddress(),
            step.getPlace().getDescription(),
            step.getPlace().getAverageCost(),
            step.getPlace().getEstimatedWaitTime()
        );
        
        return new StepResponse(
            step.getId(),
            step.getOrder(),
            placeResponse,
            step.getTimeSlot(),
            step.getEstimatedDuration(),
            step.getDistanceFromPrevious(),
            step.getCost(),
            step.getNotes()
        );
    }
}

