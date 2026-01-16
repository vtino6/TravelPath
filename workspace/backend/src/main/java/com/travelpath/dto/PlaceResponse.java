package com.travelpath.dto;

import com.travelpath.model.PlaceCategory;

public class PlaceResponse {
    private String id;
    private String name;
    private PlaceCategory category;
    private Double latitude;
    private Double longitude;
    private String address;
    private String description;
    private Double averageCost;
    private Integer estimatedWaitTime;
    
    public PlaceResponse() {}
    
    public PlaceResponse(String id, String name, PlaceCategory category, Double latitude, Double longitude,
                        String address, String description, Double averageCost, Integer estimatedWaitTime) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.description = description;
        this.averageCost = averageCost;
        this.estimatedWaitTime = estimatedWaitTime;
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public PlaceCategory getCategory() { return category; }
    public void setCategory(PlaceCategory category) { this.category = category; }
    
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Double getAverageCost() { return averageCost; }
    public void setAverageCost(Double averageCost) { this.averageCost = averageCost; }
    
    public Integer getEstimatedWaitTime() { return estimatedWaitTime; }
    public void setEstimatedWaitTime(Integer estimatedWaitTime) { this.estimatedWaitTime = estimatedWaitTime; }
}

