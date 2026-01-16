package com.travelpath.dto;

import com.travelpath.model.RouteType;
import com.travelpath.model.TransportationMode;

import java.util.List;

public class RouteResponse {
    private String id;
    private String name;
    private RouteType routeType;
    private Double totalBudget;
    private Integer totalDuration;
    private TransportationMode transportationMode;
    private String city;
    private Boolean isFavorite;
    private List<StepResponse> steps;
    
    public RouteResponse() {}
    
    public RouteResponse(String id, String name, RouteType routeType, Double totalBudget, Integer totalDuration,
                       TransportationMode transportationMode, String city, Boolean isFavorite, List<StepResponse> steps) {
        this.id = id;
        this.name = name;
        this.routeType = routeType;
        this.totalBudget = totalBudget;
        this.totalDuration = totalDuration;
        this.transportationMode = transportationMode;
        this.city = city;
        this.isFavorite = isFavorite;
        this.steps = steps;
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
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
    
    public Boolean getIsFavorite() { return isFavorite; }
    public void setIsFavorite(Boolean isFavorite) { this.isFavorite = isFavorite; }
    
    public List<StepResponse> getSteps() { return steps; }
    public void setSteps(List<StepResponse> steps) { this.steps = steps; }
}

