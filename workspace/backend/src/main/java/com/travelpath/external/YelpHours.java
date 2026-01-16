package com.travelpath.external;

public class YelpHours {
    private Integer day;  // 0=Monday, 6=Sunday
    private String start;  // "1200" = 12:00
    private String end;    // "1430" = 14:30
    private Boolean isOvernight;
    
    // Getters and Setters
    public Integer getDay() { return day; }
    public void setDay(Integer day) { this.day = day; }
    
    public String getStart() { return start; }
    public void setStart(String start) { this.start = start; }
    
    public String getEnd() { return end; }
    public void setEnd(String end) { this.end = end; }
    
    public Boolean getIsOvernight() { return isOvernight; }
    public void setIsOvernight(Boolean isOvernight) { this.isOvernight = isOvernight; }
}
