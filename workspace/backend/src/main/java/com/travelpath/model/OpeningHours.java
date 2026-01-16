package com.travelpath.model;

import jakarta.persistence.*;

@Entity
@Table(name = "opening_hours")
public class OpeningHours {
    
    @Id
    @OneToOne
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;
    
    private String mondayOpen;
    private String mondayClose;
    private String tuesdayOpen;
    private String tuesdayClose;
    private String wednesdayOpen;
    private String wednesdayClose;
    private String thursdayOpen;
    private String thursdayClose;
    private String fridayOpen;
    private String fridayClose;
    private String saturdayOpen;
    private String saturdayClose;
    private String sundayOpen;
    private String sundayClose;
    
    public OpeningHours() {}
    
    // Getters and Setters
    public Place getPlace() { return place; }
    public void setPlace(Place place) { this.place = place; }
    
    public String getMondayOpen() { return mondayOpen; }
    public void setMondayOpen(String mondayOpen) { this.mondayOpen = mondayOpen; }
    
    public String getMondayClose() { return mondayClose; }
    public void setMondayClose(String mondayClose) { this.mondayClose = mondayClose; }
    
    public String getTuesdayOpen() { return tuesdayOpen; }
    public void setTuesdayOpen(String tuesdayOpen) { this.tuesdayOpen = tuesdayOpen; }
    
    public String getTuesdayClose() { return tuesdayClose; }
    public void setTuesdayClose(String tuesdayClose) { this.tuesdayClose = tuesdayClose; }
    
    public String getWednesdayOpen() { return wednesdayOpen; }
    public void setWednesdayOpen(String wednesdayOpen) { this.wednesdayOpen = wednesdayOpen; }
    
    public String getWednesdayClose() { return wednesdayClose; }
    public void setWednesdayClose(String wednesdayClose) { this.wednesdayClose = wednesdayClose; }
    
    public String getThursdayOpen() { return thursdayOpen; }
    public void setThursdayOpen(String thursdayOpen) { this.thursdayOpen = thursdayOpen; }
    
    public String getThursdayClose() { return thursdayClose; }
    public void setThursdayClose(String thursdayClose) { this.thursdayClose = thursdayClose; }
    
    public String getFridayOpen() { return fridayOpen; }
    public void setFridayOpen(String fridayOpen) { this.fridayOpen = fridayOpen; }
    
    public String getFridayClose() { return fridayClose; }
    public void setFridayClose(String fridayClose) { this.fridayClose = fridayClose; }
    
    public String getSaturdayOpen() { return saturdayOpen; }
    public void setSaturdayOpen(String saturdayOpen) { this.saturdayOpen = saturdayOpen; }
    
    public String getSaturdayClose() { return saturdayClose; }
    public void setSaturdayClose(String saturdayClose) { this.saturdayClose = saturdayClose; }
    
    public String getSundayOpen() { return sundayOpen; }
    public void setSundayOpen(String sundayOpen) { this.sundayOpen = sundayOpen; }
    
    public String getSundayClose() { return sundayClose; }
    public void setSundayClose(String sundayClose) { this.sundayClose = sundayClose; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OpeningHours that = (OpeningHours) o;
        return place != null && place.equals(that.place);
    }
    
    @Override
    public int hashCode() {
        return place != null ? place.hashCode() : 0;
    }
}

