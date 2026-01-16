package com.travelpath.dto;

import com.travelpath.model.TimeSlot;

public class StepResponse {
    private String id;
    private Integer order;
    private PlaceResponse place;
    private TimeSlot timeSlot;
    private Integer estimatedDuration;
    private Double distanceFromPrevious;
    private Double cost;
    private String notes;
    
    // Constructors
    public StepResponse() {}
    
    public StepResponse(String id, Integer order, PlaceResponse place, TimeSlot timeSlot, 
                       Integer estimatedDuration, Double distanceFromPrevious, Double cost, String notes) {
        this.id = id;
        this.order = order;
        this.place = place;
        this.timeSlot = timeSlot;
        this.estimatedDuration = estimatedDuration;
        this.distanceFromPrevious = distanceFromPrevious;
        this.cost = cost;
        this.notes = notes;
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public Integer getOrder() { return order; }
    public void setOrder(Integer order) { this.order = order; }
    
    public PlaceResponse getPlace() { return place; }
    public void setPlace(PlaceResponse place) { this.place = place; }
    
    public TimeSlot getTimeSlot() { return timeSlot; }
    public void setTimeSlot(TimeSlot timeSlot) { this.timeSlot = timeSlot; }
    
    public Integer getEstimatedDuration() { return estimatedDuration; }
    public void setEstimatedDuration(Integer estimatedDuration) { this.estimatedDuration = estimatedDuration; }
    
    public Double getDistanceFromPrevious() { return distanceFromPrevious; }
    public void setDistanceFromPrevious(Double distanceFromPrevious) { this.distanceFromPrevious = distanceFromPrevious; }
    
    public Double getCost() { return cost; }
    public void setCost(Double cost) { this.cost = cost; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}

