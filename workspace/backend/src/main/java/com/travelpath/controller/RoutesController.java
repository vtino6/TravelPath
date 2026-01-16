package com.travelpath.controller;

import com.travelpath.dto.RouteRequest;
import com.travelpath.dto.RouteResponse;
import com.travelpath.service.RouteGeneratorService;
import com.travelpath.service.RouteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/routes")
@CrossOrigin(origins = "*")
public class RoutesController {
    
    @Autowired
    private RouteGeneratorService routeGeneratorService;
    
    @Autowired
    private RouteService routeService;
    
    @GetMapping("/test")
    public ResponseEntity<Map<String, String>> test() {
        System.out.println("=== TEST ENDPOINT CALLED ===");
        System.out.println("Backend is running and reachable!");
        return ResponseEntity.ok(Map.of(
            "status", "ok",
            "message", "Backend is running",
            "timestamp", java.time.LocalDateTime.now().toString()
        ));
    }
    
    @PostMapping("/generate")
    public ResponseEntity<List<RouteResponse>> generateRoutes(@RequestBody RouteRequest request) {
        System.out.println("=== ROUTE GENERATION REQUEST RECEIVED ===");
        System.out.println("Location: " + request.getLatitude() + ", " + request.getLongitude());
        System.out.println("Activities: " + request.getActivities());
        System.out.println("Budget: " + request.getMaxBudget());
        System.out.println("Number of Places: " + request.getNumberOfPlaces());
        System.out.println("Transportation Mode: " + request.getTransportationMode());
        System.out.println("Required Places: " + request.getRequiredPlaceIds());
        
        // Validate and set default transportation mode if null
        if (request.getTransportationMode() == null) {
            System.out.println("WARNING: TransportationMode is null, defaulting to MIXED");
            request.setTransportationMode(com.travelpath.model.TransportationMode.MIXED);
        } else {
            // Log the received mode for debugging
            System.out.println("TransportationMode received: " + request.getTransportationMode() + " (type: " + request.getTransportationMode().getClass().getName() + ")");
        }
        
        System.out.println("Starting route generation...");
        
        try {
            List<RouteResponse> routes = routeGeneratorService.generateRoutes(request);
            System.out.println("Route generation completed. Generated " + routes.size() + " routes.");
            return ResponseEntity.ok(routes);
        } catch (Exception e) {
            System.err.println("ERROR in route generation: " + e.getMessage());
            System.err.println("Exception type: " + e.getClass().getName());
            e.printStackTrace();
            // Return error response instead of throwing to prevent app crash
            return ResponseEntity.status(500).body(java.util.Collections.emptyList());
        }
    }
    
    @PostMapping("/save")
    public ResponseEntity<RouteResponse> saveRoute(
            @RequestBody RouteResponse routeResponse,
            @RequestParam(required = false) String userId
    ) {
        System.out.println("=== SAVE ROUTE ENDPOINT CALLED ===");
        System.out.println("Route ID: " + routeResponse.getId());
        System.out.println("Route name: " + routeResponse.getName());
        System.out.println("Route isFavorite: " + routeResponse.getIsFavorite());
        System.out.println("UserId: " + userId);
        
        try {
            com.travelpath.model.Route savedRoute = routeService.saveRoute(routeResponse, userId);
            System.out.println("Route saved successfully, isFavorite: " + savedRoute.getIsFavorite());
            
            // Convert saved route to response directly (avoid loading from DB which can cause session conflicts)
            RouteResponse response = routeService.convertToResponse(savedRoute);
            System.out.println("Returning route response, isFavorite: " + response.getIsFavorite());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("ERROR saving route: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    @GetMapping("/saved")
    public ResponseEntity<List<RouteResponse>> getSavedRoutes(
            @RequestParam(required = false) String userId
    ) {
        List<RouteResponse> routes = routeService.getSavedRoutes(userId != null ? userId : "anonymous");
        return ResponseEntity.ok(routes);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<RouteResponse> getRouteById(@PathVariable String id) {
        RouteResponse route = routeService.getRouteById(id);
        return ResponseEntity.ok(route);
    }
    
    @PostMapping("/{id}/favorite")
    public ResponseEntity<Map<String, Boolean>> toggleFavorite(
            @PathVariable String id,
            @RequestParam String userId
    ) {
        routeService.toggleFavorite(id, userId);
        return ResponseEntity.ok(Map.of("isFavorite", true));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoute(
            @PathVariable String id,
            @RequestParam String userId
    ) {
        routeService.deleteRoute(id, userId);
        return ResponseEntity.noContent().build();
    }
}

