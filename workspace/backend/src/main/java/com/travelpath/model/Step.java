package com.travelpath.model;

import jakarta.persistence.*;

@Entity
@Table(name = "steps")
public class Step {
    
    @Id
    private String id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;
    
    @Column(name = "step_order", nullable = false)
    private Integer order;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TimeSlot timeSlot;
    
    @Column(nullable = false)
    private Integer estimatedDuration; // in minutes
    
    private Double distanceFromPrevious; // in km
    
    @Column(nullable = false)
    private Double cost = 0.0;
    
    @Column(columnDefinition = "TEXT")
    private String notes;
    
    public Step() {}
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public Route getRoute() { return route; }
    public void setRoute(Route route) { this.route = route; }
    
    public Place getPlace() { return place; }
    public void setPlace(Place place) { this.place = place; }
    
    public Integer getOrder() { return order; }
    public void setOrder(Integer order) { this.order = order; }
    
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

