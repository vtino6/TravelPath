package com.travelpath.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "places")
public class Place {
    
    @Id
    private String id; // Google Places ID
    
    @Column(nullable = false)
    private String name;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlaceCategory category;
    
    @Column(nullable = false)
    private Double latitude;
    
    @Column(nullable = false)
    private Double longitude;
    
    private String address;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    private Double averageCost;
    
    private Integer coldImpact;
    
    private Integer heatImpact;
    
    private Integer humidityImpact;
    
    private Integer estimatedWaitTime; // in minutes
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    public Place() {}
    
    public Place(String id, String name, PlaceCategory category, Double latitude, Double longitude,
                String address, String description, Double averageCost, Integer coldImpact,
                Integer heatImpact, Integer humidityImpact, Integer estimatedWaitTime,
                LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.description = description;
        this.averageCost = averageCost;
        this.coldImpact = coldImpact;
        this.heatImpact = heatImpact;
        this.humidityImpact = humidityImpact;
        this.estimatedWaitTime = estimatedWaitTime;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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
    
    public Integer getColdImpact() { return coldImpact; }
    public void setColdImpact(Integer coldImpact) { this.coldImpact = coldImpact; }
    
    public Integer getHeatImpact() { return heatImpact; }
    public void setHeatImpact(Integer heatImpact) { this.heatImpact = heatImpact; }
    
    public Integer getHumidityImpact() { return humidityImpact; }
    public void setHumidityImpact(Integer humidityImpact) { this.humidityImpact = humidityImpact; }
    
    public Integer getEstimatedWaitTime() { return estimatedWaitTime; }
    public void setEstimatedWaitTime(Integer estimatedWaitTime) { this.estimatedWaitTime = estimatedWaitTime; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

