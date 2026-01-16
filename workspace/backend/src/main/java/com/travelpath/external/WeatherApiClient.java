package com.travelpath.external;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.List;

/**
 * Client for OpenWeatherMap API
 * Free tier: 60 calls/minute, 1,000,000 calls/month
 */
@Component
public class WeatherApiClient {
    
    @Value("${weather.api.key}")
    private String apiKey;
    
    private final WebClient webClient;
    
    public WeatherApiClient() {
        this.webClient = WebClient.builder()
            .baseUrl("https://api.openweathermap.org/data/2.5")
            .build();
    }
    
    /**
     * Get current weather for a location
     */
    public WeatherData getCurrentWeather(double latitude, double longitude) {
        try {
            WeatherResponse response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/weather")
                    .queryParam("lat", latitude)
                    .queryParam("lon", longitude)
                    .queryParam("appid", apiKey)
                    .queryParam("units", "metric") // Celsius
                    .queryParam("lang", "fr") // French descriptions
                    .build())
                .retrieve()
                .bodyToMono(WeatherResponse.class)
                .block();
            
            if (response != null) {
                return new WeatherData(
                    response.main.temp,
                    response.weather.get(0).main,
                    response.weather.get(0).description,
                    response.main.humidity,
                    response.wind.speed,
                    response.main.feels_like
                );
            }
            
            return null;
        } catch (WebClientResponseException e) {
            System.err.println("Error calling Weather API: " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Get weather forecast (5-day, 3-hour intervals)
     */
    public List<WeatherData> getForecast(double latitude, double longitude) {
        try {
            ForecastResponse response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/forecast")
                    .queryParam("lat", latitude)
                    .queryParam("lon", longitude)
                    .queryParam("appid", apiKey)
                    .queryParam("units", "metric")
                    .queryParam("lang", "fr")
                    .build())
                .retrieve()
                .bodyToMono(ForecastResponse.class)
                .block();
            
            if (response != null && response.list != null) {
                return response.list.stream()
                    .map(item -> new WeatherData(
                        item.main.temp,
                        item.weather.get(0).main,
                        item.weather.get(0).description,
                        item.main.humidity,
                        item.wind.speed,
                        item.main.feels_like
                    ))
                    .toList();
            }
            
            return new ArrayList<>();
        } catch (Exception e) {
            System.err.println("Error getting forecast: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    // Data classes for API responses
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
    
    private static class WeatherResponse {
        public Main main;
        public List<Weather> weather;
        public Wind wind;
    }
    
    private static class ForecastResponse {
        public List<ForecastItem> list;
    }
    
    private static class ForecastItem {
        public Main main;
        public List<Weather> weather;
        public Wind wind;
        public String dt_txt;
    }
    
    private static class Main {
        public double temp;
        public double feels_like;
        public int humidity;
    }
    
    private static class Weather {
        public String main;
        public String description;
    }
    
    private static class Wind {
        public double speed;
    }
}

