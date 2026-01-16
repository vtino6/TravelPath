package com.travelpath.service;

import com.travelpath.dto.RouteRequest;
import com.travelpath.dto.RouteResponse;
import com.travelpath.dto.StepResponse;
import com.travelpath.external.OpenRouteServiceClient;
import com.travelpath.model.*;
import com.travelpath.repository.PlaceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class RouteGeneratorService {
    
    @Autowired
    private PlaceRepository placeRepository;
    
    @Autowired
    private PlacesService placesService;
    
    @Autowired
    private WeatherService weatherService;
    
    @Autowired
    private OpenRouteServiceClient openRouteServiceClient;
    
    public List<RouteResponse> generateRoutes(RouteRequest request) {
        System.out.println("[RouteGeneratorService] Starting route generation...");
        System.out.println("[RouteGeneratorService] Request location: (" + request.getLatitude() + ", " + request.getLongitude() + ")");
        
        List<Place> allPlaces = new ArrayList<>();
        System.out.println("[RouteGeneratorService] Fetching places for " + request.getActivities().size() + " activities...");
        
        for (PlaceCategory category : request.getActivities()) {
            System.out.println("[RouteGeneratorService] Searching places for category: " + category + " at (" + request.getLatitude() + ", " + request.getLongitude() + ")");
            List<Place> places = placesService.searchNearbyEntities(
                request.getLatitude(),
                request.getLongitude(),
                2000,
                category
            );
            System.out.println("[RouteGeneratorService] Found " + places.size() + " places for category " + category);
            if (!places.isEmpty()) {
                System.out.println("[RouteGeneratorService] First place: " + places.get(0).getName() + " at (" + places.get(0).getLatitude() + ", " + places.get(0).getLongitude() + ")");
            }
            allPlaces.addAll(places);
        }
        
        System.out.println("[RouteGeneratorService] Total places found: " + allPlaces.size());
        
        System.out.println("[RouteGeneratorService] Getting weather data...");
        WeatherService.WeatherData weather = weatherService.getCurrentWeather(
            request.getLatitude(), request.getLongitude()
        );
        System.out.println("[RouteGeneratorService] Weather: " + weather.temperature + "°C, " + weather.condition);
        
        List<Place> filteredPlaces = filterByWeather(
            allPlaces,
            weather,
            request.getColdSensitivity(),
            request.getHeatSensitivity(),
            request.getHumiditySensitivity()
        );
        System.out.println("[RouteGeneratorService] Places after weather filter: " + filteredPlaces.size());
        
        if (request.getRequiredPlaceIds() != null && !request.getRequiredPlaceIds().isEmpty()) {
            System.out.println("[RouteGeneratorService] Adding " + request.getRequiredPlaceIds().size() + " required places...");
            List<Place> requiredPlaces = request.getRequiredPlaceIds().stream()
                .map(id -> placeRepository.findById(id).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            filteredPlaces.addAll(requiredPlaces);
        }
        
        System.out.println("[RouteGeneratorService] Generating route variants based on constraints...");
        List<RouteResponse> routes = new ArrayList<>();
        
        RouteResponse economicRoute = generateRoute(filteredPlaces, request, RouteType.ECONOMIC);
        if (isRouteValid(economicRoute, request)) {
            routes.add(economicRoute);
            System.out.println("[RouteGeneratorService] ECONOMIC route generated and validated");
        } else {
            System.out.println("[RouteGeneratorService] ECONOMIC route discarded (exceeds constraints)");
        }
        
        RouteResponse balancedRoute = generateRoute(filteredPlaces, request, RouteType.BALANCED);
        if (isRouteValid(balancedRoute, request)) {
            routes.add(balancedRoute);
            System.out.println("[RouteGeneratorService] BALANCED route generated and validated");
        } else {
            System.out.println("[RouteGeneratorService] BALANCED route discarded (exceeds constraints)");
        }
        
        RouteResponse comfortRoute = generateRoute(filteredPlaces, request, RouteType.COMFORT);
        if (isRouteValid(comfortRoute, request)) {
            routes.add(comfortRoute);
            System.out.println("[RouteGeneratorService] COMFORT route generated and validated");
        } else {
            System.out.println("[RouteGeneratorService] COMFORT route discarded (exceeds constraints)");
        }
        
        System.out.println("[RouteGeneratorService] Route generation complete. Returning " + routes.size() + " valid routes.");
        
        if (routes.isEmpty()) {
            System.out.println("[RouteGeneratorService] WARNING: All routes discarded or no routes generated.");
            System.out.println("[RouteGeneratorService] Filtered places available: " + filteredPlaces.size());
            
            if (!filteredPlaces.isEmpty()) {
                System.out.println("[RouteGeneratorService] Attempting to generate fallback route with relaxed constraints...");
                RouteResponse fallbackRoute = generateRoute(filteredPlaces, request, RouteType.BALANCED);
                if (fallbackRoute != null && fallbackRoute.getSteps() != null && fallbackRoute.getSteps().size() >= 1) {
                    routes.add(fallbackRoute);
                    System.out.println("[RouteGeneratorService] Generated fallback route with " + fallbackRoute.getSteps().size() + 
                                     " place(s) (constraints may be exceeded)");
                } else {
                    System.out.println("[RouteGeneratorService] ERROR: Fallback route generation also failed. Steps: " + 
                                     (fallbackRoute != null ? fallbackRoute.getSteps() != null ? fallbackRoute.getSteps().size() : "null" : "null"));
                }
            } else {
                System.out.println("[RouteGeneratorService] ERROR: No places available after filtering. Cannot generate routes.");
            }
        }
        
        System.out.println("[RouteGeneratorService] Final result: " + routes.size() + " route(s) to return.");
        return routes;
    }
    
    /**
     * CONSTRAINT VALIDATION: Check if route meets all constraints
     * Discard routes that exceed time or budget constraints
     */
    private boolean isRouteValid(RouteResponse route, RouteRequest request) {
        if (request.getMaxBudget() != null) {
            if (route.getTotalBudget() > request.getMaxBudget() * 1.1) {
                System.out.println("[RouteGeneratorService] Route exceeds budget: " + 
                                 route.getTotalBudget() + " > " + (request.getMaxBudget() * 1.1));
                return false;
            }
        }
        
        // Duration validation removed - we now use numberOfPlaces directly
        
        if (route.getSteps() == null || route.getSteps().size() < 1) {
            System.out.println("[RouteGeneratorService] Route has insufficient places: " + 
                             (route.getSteps() != null ? route.getSteps().size() : 0));
            return false;
        }
        
        if (route.getSteps().size() == 1) {
            System.out.println("[RouteGeneratorService] Route has only 1 place (acceptable if limited places available)");
        }
        
        return true;
    }
    
    private RouteResponse generateRoute(
        List<Place> places,
        RouteRequest request,
        RouteType routeType
    ) {
        System.out.println("[RouteGeneratorService] generateRoute called for type: " + routeType + ", input places: " + places.size());
        
        List<Place> selectedPlaces = selectPlacesByType(places, request, routeType);
        System.out.println("[RouteGeneratorService] Selected places for " + routeType + ": " + selectedPlaces.size());
        
        if (selectedPlaces.isEmpty()) {
            System.out.println("[RouteGeneratorService] ERROR: No places selected for route type " + routeType);
            // Get transportation mode, default to MIXED if null
            com.travelpath.model.TransportationMode defaultMode = request.getTransportationMode();
            if (defaultMode == null) {
                defaultMode = com.travelpath.model.TransportationMode.MIXED;
            }
            // Return empty route with empty steps list
            List<StepResponse> emptySteps = new ArrayList<>();
            return new RouteResponse(
                UUID.randomUUID().toString(),
                "Parcours " + routeType.name(),
                routeType,
                0.0,
                0,
                defaultMode,
                null,
                false,
                emptySteps
            );
        }
        
        List<Place> orderedPlaces = selectedPlaces;
        System.out.println("[RouteGeneratorService] Using iteratively selected order: " + orderedPlaces.size() + " places");
        
        double activityCost = orderedPlaces.stream()
            .mapToDouble(p -> p.getAverageCost() != null ? p.getAverageCost() : 0.0)
            .sum();
        
        // Get transportation mode, default to MIXED if null
        com.travelpath.model.TransportationMode mode = request.getTransportationMode();
        if (mode == null) {
            mode = com.travelpath.model.TransportationMode.MIXED;
            System.out.println("[RouteGeneratorService] WARNING: TransportationMode was null, defaulting to MIXED");
        }
        double transportCost = estimateTransportCost(orderedPlaces, mode);
        
        double totalBudget = activityCost + transportCost;
        
        int totalDuration = calculateTotalDuration(orderedPlaces);
        
        if (request.getMaxBudget() != null && totalBudget > request.getMaxBudget() * 1.1) {
            System.out.println("[RouteGeneratorService] WARNING: Route exceeds budget constraint. " +
                             "Budget: " + totalBudget + " > Max: " + request.getMaxBudget());
        }
        
        // Duration validation removed - we now use numberOfPlaces directly
        
        List<StepResponse> steps = new ArrayList<>();
        
        for (int i = 0; i < orderedPlaces.size(); i++) {
            Place place = orderedPlaces.get(i);
            
            double stepTransportCost = 0.0;
            if (i > 0) {
                Place previousPlace = orderedPlaces.get(i - 1);
                double distance = calculateDistance(previousPlace, place);
                com.travelpath.model.TransportationMode stepMode = request.getTransportationMode();
                if (stepMode == null) {
                    stepMode = com.travelpath.model.TransportationMode.MIXED;
                }
                stepTransportCost = calculateSegmentTransportCost(distance, stepMode);
            }
            
            double placeCost = place.getAverageCost() != null ? place.getAverageCost() : 0.0;
            double stepCost = placeCost + stepTransportCost;
            
            stepCost += 5.0;
            
            StepResponse step = new StepResponse(
                UUID.randomUUID().toString(),
                i + 1,
                convertToPlaceResponse(place),
                determineTimeSlot(i),
                estimateDuration(place),
                i > 0 ? calculateDistance(orderedPlaces.get(i-1), place) : null,
                stepCost,
                null
            );
            steps.add(step);
        }
        
        // Get transportation mode, default to MIXED if null
        com.travelpath.model.TransportationMode responseMode = request.getTransportationMode();
        if (responseMode == null) {
            responseMode = com.travelpath.model.TransportationMode.MIXED;
        }
        // Ensure steps is never null
        if (steps == null) {
            steps = new ArrayList<>();
            System.out.println("[RouteGeneratorService] WARNING: Steps was null, initializing empty list");
        }
        
        return new RouteResponse(
            UUID.randomUUID().toString(),
            "Parcours " + routeType.name(),
            routeType,
            totalBudget,
            totalDuration,
            responseMode,
            null,
            false,
            steps
        );
    }
    
    private List<Place> selectPlacesByType(
        List<Place> availablePlaces,
        RouteRequest request,
        RouteType routeType
    ) {
        int targetPlaces = deriveTargetNumberOfPlaces(request, routeType);
        System.out.println("[RouteGeneratorService] Target number of places: " + targetPlaces);
        
        if (availablePlaces.size() < targetPlaces) {
            System.out.println("[RouteGeneratorService] WARNING: Only " + availablePlaces.size() + 
                             " places available, adjusting target from " + targetPlaces);
            targetPlaces = Math.max(1, availablePlaces.size());
        }
        
        if (targetPlaces < 1) {
            System.out.println("[RouteGeneratorService] WARNING: Cannot generate route - target places < 1");
            return new ArrayList<>();
        }
        
        // Calculate search radius based on target number of places
        // Use a reasonable default radius (1500m) for finding nearby places
        int searchRadiusMeters = 1500;
        System.out.println("[RouteGeneratorService] Search radius between places: " + searchRadiusMeters + "m");
        
        List<Place> selectedPlaces = new ArrayList<>();
        Place currentPlace = findClosestPlace(availablePlaces, request.getLatitude(), request.getLongitude());
        
        if (currentPlace == null) {
            System.out.println("[RouteGeneratorService] ERROR: No places available to start route");
            return new ArrayList<>();
        }
        
        selectedPlaces.add(currentPlace);
        System.out.println("[RouteGeneratorService] Starting place: " + currentPlace.getName() + " at (" + 
                          currentPlace.getLatitude() + ", " + currentPlace.getLongitude() + ")");
        
        // Step 4: Iteratively find next places (if target > 1)
        Set<String> usedPlaceIds = new HashSet<>();
        usedPlaceIds.add(currentPlace.getId());
        
        Random random = new Random();
        Place current = currentPlace; // Make effectively final for lambda
        
        // Calculate initial remaining budget (90% for places, 10% reserved for transport)
        double remainingBudget = request.getMaxBudget() != null 
            ? request.getMaxBudget() * 0.9  // 90% of budget for places
            : Double.MAX_VALUE;
        
        // Subtract cost of first place from remaining budget
        if (request.getMaxBudget() != null && currentPlace.getAverageCost() != null) {
            remainingBudget -= currentPlace.getAverageCost();
            System.out.println("[RouteGeneratorService] Starting budget: " + request.getMaxBudget() + 
                             "€, Remaining after first place (" + currentPlace.getAverageCost() + "€): " + 
                             String.format("%.2f", remainingBudget) + "€");
        }
        
        // Only try to find more places if target > 1 and we have available places
        for (int i = 1; i < targetPlaces && i < availablePlaces.size(); i++) {
            final Place currentPlaceForIteration = current; // Final reference for lambda
            final double currentRemainingBudget = remainingBudget; // Final reference for lambda
            
            List<Place> candidates = availablePlaces.stream()
                .filter(p -> !usedPlaceIds.contains(p.getId()))
                .filter(p -> {
                    double distance = calculateDistance(currentPlaceForIteration, p);
                    return distance * 1000 <= searchRadiusMeters;
                })
                .filter(p -> {
                    if (request.getMaxBudget() != null) {
                        // Use remaining budget instead of total budget
                        return p.getAverageCost() == null || p.getAverageCost() <= currentRemainingBudget;
                    }
                    return true;
                })
                .collect(Collectors.toList());
            
            if (candidates.isEmpty()) {
                System.out.println("[RouteGeneratorService] WARNING: No candidates found for place " + (i + 1) + ", stopping");
                break;
            }
            
            Place nextPlace = selectNextPlace(candidates, currentPlaceForIteration, request, routeType, random);
            
            if (nextPlace == null) {
                System.out.println("[RouteGeneratorService] WARNING: Could not select next place, stopping");
                break;
            }
            
            selectedPlaces.add(nextPlace);
            usedPlaceIds.add(nextPlace.getId());
            current = nextPlace;
            
            // Update remaining budget after selecting a place
            if (request.getMaxBudget() != null && nextPlace.getAverageCost() != null) {
                remainingBudget -= nextPlace.getAverageCost();
                System.out.println("[RouteGeneratorService] Selected place " + (i + 1) + ": " + nextPlace.getName() + 
                                 " (cost: " + nextPlace.getAverageCost() + "€, distance: " + 
                                 String.format("%.2f", calculateDistance(selectedPlaces.get(i-1), nextPlace)) + 
                                 " km, remaining budget: " + String.format("%.2f", remainingBudget) + "€)");
            } else {
                System.out.println("[RouteGeneratorService] Selected place " + (i + 1) + ": " + nextPlace.getName() + 
                                 " (distance: " + String.format("%.2f", calculateDistance(selectedPlaces.get(i-1), nextPlace)) + " km)");
            }
            
            // Stop if budget is exhausted
            if (request.getMaxBudget() != null && remainingBudget <= 0) {
                System.out.println("[RouteGeneratorService] Budget exhausted, stopping place selection");
                break;
            }
        }
        
        System.out.println("[RouteGeneratorService] Selected " + selectedPlaces.size() + " places iteratively");
        return selectedPlaces;
    }
    
    private int deriveTargetNumberOfPlaces(RouteRequest request, RouteType routeType) {
        // Use numberOfPlaces directly from request
        int requestedPlaces = request.getNumberOfPlaces() != null ? request.getNumberOfPlaces() : 5;
        
        // Check budget constraint
        int maxPlacesByBudget = Integer.MAX_VALUE;
        if (request.getMaxBudget() != null) {
            double usableBudget = request.getMaxBudget() * 0.9;
            maxPlacesByBudget = (int)(usableBudget / 25.0);
        }
        
        double routeTypeMultiplier = switch (routeType) {
            case ECONOMIC -> 0.8;
            case BALANCED -> 1.0;
            case COMFORT -> 1.2;
        };
        
        // Apply route type multiplier to requested places
        int targetPlaces = (int)(requestedPlaces * routeTypeMultiplier);
        
        // Ensure we don't exceed budget constraint
        targetPlaces = Math.min(targetPlaces, maxPlacesByBudget);
        
        // Ensure minimum of 1 place and respect user's requested number (no hard max limit)
        return Math.max(1, targetPlaces);
    }
    
    private Place findClosestPlace(List<Place> places, double startLat, double startLng) {
        if (places.isEmpty()) return null;
        
        Place closest = places.get(0);
        double minDist = Double.MAX_VALUE;
        
        for (Place place : places) {
            double dist = Math.sqrt(
                Math.pow(place.getLatitude() - startLat, 2) +
                Math.pow(place.getLongitude() - startLng, 2)
            );
            if (dist < minDist) {
                minDist = dist;
                closest = place;
            }
        }
        
        return closest;
    }
    
    private Place selectNextPlace(
        List<Place> candidates,
        Place currentPlace,
        RouteRequest request,
        RouteType routeType,
        Random random
    ) {
        if (candidates.isEmpty()) return null;
        
        List<Place> sorted = switch (routeType) {
            case ECONOMIC -> candidates.stream()
                .sorted(Comparator.comparing(p -> {
                    double cost = p.getAverageCost() != null ? p.getAverageCost() : 0.0;
                    double distance = calculateDistance(currentPlace, p);
                    return cost + (distance * 2);
                }))
                .collect(Collectors.toList());
            case BALANCED -> candidates.stream()
                .sorted(Comparator.comparing(p -> calculateDistance(currentPlace, p)))
                .collect(Collectors.toList());
            case COMFORT -> candidates.stream()
                .sorted(Comparator.comparing((Place p) -> {
                    double cost = p.getAverageCost() != null ? p.getAverageCost() : 0.0;
                    return -cost;
                }).thenComparing(p -> calculateDistance(currentPlace, p)))
                .collect(Collectors.toList());
        };
        
        int topN = Math.min(3, sorted.size());
        int randomIndex = random.nextInt(topN);
        return sorted.get(randomIndex);
    }
    
    private List<Place> optimizeOrder(List<Place> places, double startLat, double startLng) {
        if (places.isEmpty()) return places;
        if (places.size() == 1) return places;
        
        double[][] distanceMatrix = getDistanceMatrix(places);
        
        List<Place> ordered = new ArrayList<>();
        List<Integer> remaining = new ArrayList<>();
        for (int i = 0; i < places.size(); i++) {
            remaining.add(i);
        }
        
        int currentIndex = findClosestToStart(places, startLat, startLng);
        ordered.add(places.get(currentIndex));
        remaining.remove(Integer.valueOf(currentIndex));
        
        while (!remaining.isEmpty()) {
            int nearestIndex = findNearestIndex(currentIndex, remaining, distanceMatrix);
            ordered.add(places.get(nearestIndex));
            remaining.remove(Integer.valueOf(nearestIndex));
            currentIndex = nearestIndex;
        }
        
        return ordered;
    }
    
    private int findClosestToStart(List<Place> places, double startLat, double startLng) {
        int closest = 0;
        double minDist = Double.MAX_VALUE;
        
        for (int i = 0; i < places.size(); i++) {
            Place place = places.get(i);
            double dist = Math.sqrt(
                Math.pow(place.getLatitude() - startLat, 2) +
                Math.pow(place.getLongitude() - startLng, 2)
            );
            if (dist < minDist) {
                minDist = dist;
                closest = i;
            }
        }
        return closest;
    }
    
    private int findNearestIndex(int fromIndex, List<Integer> candidates, double[][] distanceMatrix) {
        int nearest = candidates.get(0);
        double minDist = distanceMatrix[fromIndex][nearest];
        
        for (int candidate : candidates) {
            if (distanceMatrix[fromIndex][candidate] < minDist) {
                minDist = distanceMatrix[fromIndex][candidate];
                nearest = candidate;
            }
        }
        return nearest;
    }
    
    private double calculateDistance(Place from, Place to) {
        try {
            OpenRouteServiceClient.DirectionsResponse response = openRouteServiceClient.getDirections(
                from.getLatitude(), from.getLongitude(),
                to.getLatitude(), to.getLongitude(),
                "foot-walking"
            );
            
            if (response != null && response.routes != null && !response.routes.isEmpty()) {
                double distance = response.routes.get(0).summary.distance;
                System.out.println("[RouteGeneratorService] OpenRouteService distance: " + distance + " km");
                return distance;
            }
        } catch (Exception e) {
            System.err.println("[RouteGeneratorService] Error getting distance from OpenRouteService: " + e.getMessage());
        }
        
        double earthRadius = 6371.0;
        double dLat = Math.toRadians(to.getLatitude() - from.getLatitude());
        double dLon = Math.toRadians(to.getLongitude() - from.getLongitude());
        
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(from.getLatitude())) * Math.cos(Math.toRadians(to.getLatitude())) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double haversineDistance = earthRadius * c;
        System.out.println("[RouteGeneratorService] Using Haversine fallback distance: " + haversineDistance + " km");
        return haversineDistance;
    }
    
    private double estimateTransportCost(List<Place> places, com.travelpath.model.TransportationMode mode) {
        if (places.size() <= 1) return 0.0;
        
        double totalTransportCost = 0.0;
        
        for (int i = 0; i < places.size() - 1; i++) {
            Place from = places.get(i);
            Place to = places.get(i + 1);
            
            double distance = calculateDistance(from, to);
            totalTransportCost += calculateSegmentTransportCost(distance, mode);
        }
        
        // Add food/refreshments estimate (5€ per place)
        totalTransportCost += places.size() * 5.0;
        
        return totalTransportCost;
    }
    
    /**
     * Calculate transport cost for a single segment based on distance and mode
     */
    private double calculateSegmentTransportCost(double distance, com.travelpath.model.TransportationMode mode) {
        if (mode == null) {
            mode = com.travelpath.model.TransportationMode.MIXED; // Default to mixed
        }
        
        switch (mode) {
            case WALKING:
                return 0.0; // Free
                
            case BICYCLE:
                return 0.0; // Free
                
            case PUBLIC_TRANSPORT:
                // 2.50€ per trip (city-dependent, using average)
                return 2.50;
                
            case CAR:
                // Fuel cost: ~0.10€/km + parking: ~3€ per destination
                double fuelCost = distance * 0.10;
                double parkingCost = 3.0; // Per destination
                return fuelCost + parkingCost;
                
            case MIXED:
                // Smart selection: mix of walking (free) and public transport
                if (distance < 1.5) {
                    return 0.0; // Walking
                } else if (distance < 5.0) {
                    return 2.50; // Public transport
                } else {
                    // For longer distances, use car or public transport
                    double carCost = (distance * 0.10) + 3.0;
                    return Math.min(carCost, 2.50); // Choose cheaper option
                }
                
            default:
                return 0.0;
        }
    }
    
    private double[][] getDistanceMatrix(List<Place> places) {
        System.out.println("[RouteGeneratorService] Building distance matrix for " + places.size() + " places...");
        
        List<OpenRouteServiceClient.Location> locations = places.stream()
            .map(p -> new OpenRouteServiceClient.Location(p.getLatitude(), p.getLongitude()))
            .collect(Collectors.toList());
        
        OpenRouteServiceClient.DistanceMatrixResponse matrix = openRouteServiceClient.getDistanceMatrix(
            locations, "foot-walking"
        );
        
        if (matrix != null && matrix.distances != null) {
            System.out.println("[RouteGeneratorService] Using OpenRouteService distance matrix");
            return matrix.distances;
        }
        
        System.out.println("[RouteGeneratorService] OpenRouteService unavailable, using Haversine fallback for distance matrix");
        double[][] distances = new double[places.size()][places.size()];
        for (int i = 0; i < places.size(); i++) {
            for (int j = 0; j < places.size(); j++) {
                if (i == j) {
                    distances[i][j] = 0;
                } else {
                    distances[i][j] = calculateDistance(places.get(i), places.get(j));
                }
            }
        }
        return distances;
    }
    
    private List<Place> filterByWeather(
        List<Place> places,
        WeatherService.WeatherData weather,
        int coldSensitivity,
        int heatSensitivity,
        int humiditySensitivity
    ) {
        if (!weatherService.isWeatherSuitable(weather, coldSensitivity, heatSensitivity, humiditySensitivity)) {
            return places.stream()
                .filter(p -> p.getColdImpact() == 0 && p.getHeatImpact() == 0 && p.getHumidityImpact() == 0)
                .collect(Collectors.toList());
        }
        return places;
    }
    
    private TimeSlot determineTimeSlot(int index) {
        if (index < 2) return TimeSlot.MORNING;
        if (index < 5) return TimeSlot.AFTERNOON;
        return TimeSlot.EVENING;
    }
    
    private int estimateDuration(Place place) {
        int baseDuration = 60;
        return baseDuration + (place.getEstimatedWaitTime() != null ? place.getEstimatedWaitTime() : 0);
    }
    
    private int calculateTotalDuration(List<Place> places) {
        if (places.isEmpty()) return 0;
        if (places.size() == 1) return estimateDuration(places.get(0));
        
        List<OpenRouteServiceClient.Location> locations = places.stream()
            .map(p -> new OpenRouteServiceClient.Location(p.getLatitude(), p.getLongitude()))
            .collect(Collectors.toList());
        
        OpenRouteServiceClient.DistanceMatrixResponse matrix = openRouteServiceClient.getDistanceMatrix(
            locations, "foot-walking"
        );
        
        int totalDuration = 0;
        
        for (Place place : places) {
            totalDuration += estimateDuration(place);
        }
        
        if (matrix != null && matrix.durations != null) {
            for (int i = 0; i < places.size() - 1; i++) {
                totalDuration += (int)(matrix.durations[i][i + 1] / 60);
            }
        } else {
            for (int i = 0; i < places.size() - 1; i++) {
                double distance = calculateDistance(places.get(i), places.get(i + 1));
                totalDuration += (int)(distance * 12);
            }
        }
        
        return totalDuration;
    }
    
    private com.travelpath.dto.PlaceResponse convertToPlaceResponse(Place place) {
        return new com.travelpath.dto.PlaceResponse(
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

