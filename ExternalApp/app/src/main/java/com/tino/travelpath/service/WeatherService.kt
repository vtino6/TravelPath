package com.tino.travelpath.service

import com.tino.travelpath.data.api.RetrofitClient
import com.tino.travelpath.data.api.dto.WeatherResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class Weather(
    val temperature: Double,
    val condition: String,
    val description: String,
    val humidity: Int,
    val windSpeed: Double,
    val feelsLike: Double
)

data class WeatherForecast(
    val current: Weather,
    val hourly: List<Weather>
)

class WeatherService(
    private val apiService: com.tino.travelpath.data.api.TravelPathApiService = RetrofitClient.apiService
) {
    
    suspend fun getCurrentWeather(location: Coordinates): Weather = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getCurrentWeather(location.latitude, location.longitude)
            Weather(
                temperature = response.temperature,
                condition = response.condition,
                description = response.description,
                humidity = response.humidity,
                windSpeed = response.windSpeed,
                feelsLike = response.feelsLike
            )
        } catch (e: Exception) {
            // Fallback to default weather if API fails
            Weather(
                temperature = 20.0,
                condition = "Clear",
                description = "Ensoleillé",
                humidity = 60,
                windSpeed = 10.0,
                feelsLike = 20.0
            )
        }
    }
    
    suspend fun getForecast(location: Coordinates): WeatherForecast = withContext(Dispatchers.IO) {
        try {
            val current = getCurrentWeather(location)
            val forecast = apiService.getWeatherForecast(location.latitude, location.longitude)
            val hourly = forecast.map { response ->
                Weather(
                    temperature = response.temperature,
                    condition = response.condition,
                    description = response.description,
                    humidity = response.humidity,
                    windSpeed = response.windSpeed,
                    feelsLike = response.feelsLike
                )
            }
            WeatherForecast(current = current, hourly = hourly)
        } catch (e: Exception) {
            // Fallback
            WeatherForecast(
                current = getCurrentWeather(location),
                hourly = emptyList()
            )
        }
    }
    
    suspend fun isWeatherSuitable(
        location: Coordinates,
        coldSensitivity: Int,
        heatSensitivity: Int,
        humiditySensitivity: Int
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            apiService.checkWeatherSuitability(
                latitude = location.latitude,
                longitude = location.longitude,
                coldSensitivity = coldSensitivity,
                heatSensitivity = heatSensitivity,
                humiditySensitivity = humiditySensitivity
            )
        } catch (e: Exception) {
            true // Default to suitable if API fails
        }
    }
}
