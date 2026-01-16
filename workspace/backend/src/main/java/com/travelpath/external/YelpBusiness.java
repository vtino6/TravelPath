package com.travelpath.external;

import java.util.List;

public class YelpBusiness {
    private String id;
    private String name;
    private String price;  // "$", "$$", "$$$", "$$$$"
    private Double rating;
    private Integer reviewCount;
    private Boolean isClosed;
    private Boolean isOpenNow;
    private Double latitude;
    private Double longitude;
    private String address;
    private String city;
    private String country;
    private String zipCode;
    private String displayAddress;
    private List<String> categories;
    private List<YelpHours> hours;
    private String phone;
    private String displayPhone;
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }
    
    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }
    
    public Integer getReviewCount() { return reviewCount; }
    public void setReviewCount(Integer reviewCount) { this.reviewCount = reviewCount; }
    
    public Boolean getIsClosed() { return isClosed; }
    public void setIsClosed(Boolean isClosed) { this.isClosed = isClosed; }
    
    public Boolean getIsOpenNow() { return isOpenNow; }
    public void setIsOpenNow(Boolean isOpenNow) { this.isOpenNow = isOpenNow; }
    
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    
    public String getZipCode() { return zipCode; }
    public void setZipCode(String zipCode) { this.zipCode = zipCode; }
    
    public String getDisplayAddress() { return displayAddress; }
    public void setDisplayAddress(String displayAddress) { this.displayAddress = displayAddress; }
    
    public List<String> getCategories() { return categories; }
    public void setCategories(List<String> categories) { this.categories = categories; }
    
    public List<YelpHours> getHours() { return hours; }
    public void setHours(List<YelpHours> hours) { this.hours = hours; }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    
    public String getDisplayPhone() { return displayPhone; }
    public void setDisplayPhone(String displayPhone) { this.displayPhone = displayPhone; }
}
