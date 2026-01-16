package com.travelpath.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "routes")
public class Route {
    
    @Id
    private String id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    
    // Store userId as string for querying even if User entity doesn't exist
    // This ensures routes are always tied to a specific user
    @Column(name = "user_id_string")
    private String userIdString;
    
    @Column(nullable = false)
    private String name;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RouteType routeType;
    
    @Column(nullable = false)
    private Double totalBudget;
    
    @Column(nullable = false)
    private Integer totalDuration; // in minutes
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = true)  // Temporarily nullable to allow migration, will be set to NOT NULL after data migration
    private TransportationMode transportationMode;
    
    private String city;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    @Column(nullable = false)
    private Boolean isSaved = false;
    
    @Column(nullable = false)
    private Boolean isFavorite = false;
    
    // Store steps as JSON string instead of separate entities to avoid session conflicts
    @Column(name = "steps_json", columnDefinition = "TEXT")
    private String stepsJson;
    
    @OneToMany(mappedBy = "route", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Step> steps;
    
    public Route() {}
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    
    public String getUserIdString() { return userIdString; }
    public void setUserIdString(String userIdString) { this.userIdString = userIdString; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public RouteType getRouteType() { return routeType; }
    public void setRouteType(RouteType routeType) { this.routeType = routeType; }
    
    public Double getTotalBudget() { return totalBudget; }
    public void setTotalBudget(Double totalBudget) { this.totalBudget = totalBudget; }
    
    public Integer getTotalDuration() { return totalDuration; }
    public void setTotalDuration(Integer totalDuration) { this.totalDuration = totalDuration; }
    
    public TransportationMode getTransportationMode() { return transportationMode; }
    public void setTransportationMode(TransportationMode transportationMode) { this.transportationMode = transportationMode; }
    
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public Boolean getIsSaved() { return isSaved; }
    public void setIsSaved(Boolean isSaved) { this.isSaved = isSaved; }
    
    public Boolean getIsFavorite() { return isFavorite; }
    public void setIsFavorite(Boolean isFavorite) { this.isFavorite = isFavorite; }
    
    public String getStepsJson() { return stepsJson; }
    public void setStepsJson(String stepsJson) { this.stepsJson = stepsJson; }
    
    public List<Step> getSteps() { return steps; }
    public void setSteps(List<Step> steps) { this.steps = steps; }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        // Set default transportation mode if null (for migration compatibility)
        if (transportationMode == null) {
            transportationMode = TransportationMode.MIXED;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        // Ensure transportation mode is never null
        if (transportationMode == null) {
            transportationMode = TransportationMode.MIXED;
        }
    }
}

