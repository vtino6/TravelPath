package com.travelpath.external;

import java.util.List;

public class GooglePlace {
    private String id;
    private String name;
    private Double latitude;
    private Double longitude;
    private String address;
    private String priceLevel;  // "PRICE_LEVEL_FREE", "PRICE_LEVEL_INEXPENSIVE", "PRICE_LEVEL_MODERATE", etc.
    private Double rating;
    private Integer userRatingCount;
    private List<String> types;
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    
    public String getPriceLevel() { return priceLevel; }
    public void setPriceLevel(String priceLevel) { this.priceLevel = priceLevel; }
    
    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }
    
    public Integer getUserRatingCount() { return userRatingCount; }
    public void setUserRatingCount(Integer userRatingCount) { this.userRatingCount = userRatingCount; }
    
    public List<String> getTypes() { return types; }
    public void setTypes(List<String> types) { this.types = types; }
}
