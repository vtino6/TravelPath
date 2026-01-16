package com.travelpath.controller;

import com.travelpath.dto.PlaceResponse;
import com.travelpath.model.PlaceCategory;
import com.travelpath.service.PlacesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/places")  // Note: context-path=/api already prefixes this, so full path is /api/places
@CrossOrigin(origins = "*") // Configure properly for production
public class PlacesController {
    
    @Autowired
    private PlacesService placesService;
    
    @GetMapping("/search")
    public ResponseEntity<List<PlaceResponse>> searchPlaces(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "5000") int radius,
            @RequestParam PlaceCategory category
    ) {
        List<PlaceResponse> places = placesService.searchNearby(lat, lng, radius, category);
        return ResponseEntity.ok(places);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<PlaceResponse> getPlaceDetails(@PathVariable String id) {
        PlaceResponse place = placesService.getPlaceDetails(id);
        return ResponseEntity.ok(place);
    }
}

