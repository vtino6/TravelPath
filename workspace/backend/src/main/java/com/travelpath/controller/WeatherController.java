package com.travelpath.controller;

import com.travelpath.service.WeatherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/weather")  // Note: context-path=/api already prefixes this, so full path is /api/weather
@CrossOrigin(origins = "*")
public class WeatherController {
    
    @Autowired
    private WeatherService weatherService;
    
    @GetMapping
    public ResponseEntity<WeatherService.WeatherData> getCurrentWeather(
            @RequestParam double lat,
            @RequestParam double lng
    ) {
        WeatherService.WeatherData weather = weatherService.getCurrentWeather(lat, lng);
        return ResponseEntity.ok(weather);
    }
    
    @GetMapping("/forecast")
    public ResponseEntity<java.util.List<WeatherService.WeatherData>> getForecast(
            @RequestParam double lat,
            @RequestParam double lng
    ) {
        java.util.List<WeatherService.WeatherData> forecast = weatherService.getForecast(lat, lng);
        return ResponseEntity.ok(forecast);
    }
    
    @GetMapping("/check")
    public ResponseEntity<Boolean> checkWeatherSuitability(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "0") int coldSensitivity,
            @RequestParam(defaultValue = "0") int heatSensitivity,
            @RequestParam(defaultValue = "0") int humiditySensitivity
    ) {
        WeatherService.WeatherData weather = weatherService.getCurrentWeather(lat, lng);
        boolean suitable = weatherService.isWeatherSuitable(
            weather, coldSensitivity, heatSensitivity, humiditySensitivity
        );
        return ResponseEntity.ok(suitable);
    }
}

