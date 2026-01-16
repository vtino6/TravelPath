package com.travelpath.controller;

import com.travelpath.external.OpenRouteServiceClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/directions")  // Note: context-path=/api already prefixes this, so full path is /api/directions
@CrossOrigin(origins = "*")
public class DirectionsController {
    
    @Autowired
    private OpenRouteServiceClient openRouteServiceClient;
    
    @GetMapping
    public ResponseEntity<Map<String, Object>> getDirections(
            @RequestParam double fromLat,
            @RequestParam double fromLon,
            @RequestParam double toLat,
            @RequestParam double toLon,
            @RequestParam(defaultValue = "foot-walking") String profile
    ) {
        OpenRouteServiceClient.DirectionsResponse response = openRouteServiceClient.getDirections(
            fromLat, fromLon, toLat, toLon, profile
        );
        
        if (response == null || response.routes == null || response.routes.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        OpenRouteServiceClient.Route route = response.routes.get(0);
        Map<String, Object> result = new HashMap<>();
        result.put("distance", route.summary.distance);
        result.put("duration", route.summary.duration);
        result.put("polyline", route.geometry.coordinates);
        
        return ResponseEntity.ok(result);
    }
}

