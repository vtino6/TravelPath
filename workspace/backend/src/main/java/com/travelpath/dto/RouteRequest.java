package com.travelpath.dto;

import com.travelpath.model.PlaceCategory;
import com.travelpath.model.TransportationMode;

import java.util.List;

public class RouteRequest {
    private Double latitude;
    private Double longitude;
    private List<PlaceCategory> activities;
    private Double maxBudget;
    private Integer numberOfPlaces; // Number of places user wants to visit
    private TransportationMode transportationMode;
    private Integer coldSensitivity;
    private Integer heatSensitivity;
    private Integer humiditySensitivity;
    private List<String> requiredPlaceIds; // Places that must be included
    
    // Constructors
    public RouteRequest() {}
    
    public RouteRequest(Double latitude, Double longitude, List<PlaceCategory> activities, 
                       Double maxBudget, Integer numberOfPlaces, TransportationMode transportationMode,
                       Integer coldSensitivity, Integer heatSensitivity, Integer humiditySensitivity,
                       List<String> requiredPlaceIds) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.activities = activities;
        this.maxBudget = maxBudget;
        this.numberOfPlaces = numberOfPlaces;
        this.transportationMode = transportationMode;
        this.coldSensitivity = coldSensitivity;
        this.heatSensitivity = heatSensitivity;
        this.humiditySensitivity = humiditySensitivity;
        this.requiredPlaceIds = requiredPlaceIds;
    }
    
    // Getters and Setters
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    
    public List<PlaceCategory> getActivities() { return activities; }
    public void setActivities(List<PlaceCategory> activities) { this.activities = activities; }
    
    public Double getMaxBudget() { return maxBudget; }
    public void setMaxBudget(Double maxBudget) { this.maxBudget = maxBudget; }
    
    public Integer getNumberOfPlaces() { return numberOfPlaces; }
    public void setNumberOfPlaces(Integer numberOfPlaces) { this.numberOfPlaces = numberOfPlaces; }
    
    public TransportationMode getTransportationMode() { return transportationMode; }
    public void setTransportationMode(TransportationMode transportationMode) { this.transportationMode = transportationMode; }
    
    public Integer getColdSensitivity() { return coldSensitivity; }
    public void setColdSensitivity(Integer coldSensitivity) { this.coldSensitivity = coldSensitivity; }
    
    public Integer getHeatSensitivity() { return heatSensitivity; }
    public void setHeatSensitivity(Integer heatSensitivity) { this.heatSensitivity = heatSensitivity; }
    
    public Integer getHumiditySensitivity() { return humiditySensitivity; }
    public void setHumiditySensitivity(Integer humiditySensitivity) { this.humiditySensitivity = humiditySensitivity; }
    
    public List<String> getRequiredPlaceIds() { return requiredPlaceIds; }
    public void setRequiredPlaceIds(List<String> requiredPlaceIds) { this.requiredPlaceIds = requiredPlaceIds; }
}

