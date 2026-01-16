package com.travelpath.service;

import com.travelpath.external.WeatherApiClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class WeatherService {
    
    @Autowired
    private WeatherApiClient weatherApiClient;
    
    public WeatherData getCurrentWeather(double latitude, double longitude) {
        WeatherApiClient.WeatherData weather = weatherApiClient.getCurrentWeather(latitude, longitude);
        
        if (weather == null) {
            return new WeatherData(20.0, "Clear", "Ensoleillé", 60, 10.0, 20.0);
        }
        
        return new WeatherData(
            weather.temperature,
            weather.condition,
            weather.description,
            weather.humidity,
            weather.windSpeed,
            weather.feelsLike
        );
    }
    
    public List<WeatherData> getForecast(double latitude, double longitude) {
        List<WeatherApiClient.WeatherData> apiForecast = weatherApiClient.getForecast(latitude, longitude);
        return apiForecast.stream()
            .map(apiData -> new WeatherData(
                apiData.temperature,
                apiData.condition,
                apiData.description,
                apiData.humidity,
                apiData.windSpeed,
                apiData.feelsLike
            ))
            .collect(java.util.stream.Collectors.toList());
    }
    
    public boolean isWeatherSuitable(
        WeatherData weather,
        int coldSensitivity,
        int heatSensitivity,
        int humiditySensitivity
    ) {
        if (coldSensitivity > 0 && weather.temperature < (15 - coldSensitivity * 2)) {
            return false;
        }
        
        if (heatSensitivity > 0 && weather.temperature > (25 + heatSensitivity * 2)) {
            return false;
        }
        
        if (humiditySensitivity > 0 && weather.humidity > (70 + humiditySensitivity * 5)) {
            return false;
        }
        
        if (weather.condition.equalsIgnoreCase("Rain") || 
            weather.condition.equalsIgnoreCase("Thunderstorm") ||
            weather.condition.equalsIgnoreCase("Snow")) {
            return coldSensitivity < 3 && heatSensitivity < 3;
        }
        
        return true;
    }
    
    public static class WeatherData {
        public final double temperature;
        public final String condition;
        public final String description;
        public final int humidity;
        public final double windSpeed;
        public final double feelsLike;
        
        public WeatherData(double temperature, String condition, String description,
                          int humidity, double windSpeed, double feelsLike) {
            this.temperature = temperature;
            this.condition = condition;
            this.description = description;
            this.humidity = humidity;
            this.windSpeed = windSpeed;
            this.feelsLike = feelsLike;
        }
    }
}

