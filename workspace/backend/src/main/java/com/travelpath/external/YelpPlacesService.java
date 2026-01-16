package com.travelpath.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class YelpPlacesService {
    
    @Value("${yelp.api.key}")
    private String apiKey;
    
    @Value("${yelp.api.enabled:true}")
    private boolean enabled;
    
    private static final String YELP_BASE_URL = "https://api.yelp.com/v3/businesses";
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Search for businesses near a location
     */
    public List<YelpBusiness> searchBusinesses(String term, 
                                               double latitude, 
                                               double longitude,
                                               int radius) {
        if (!enabled) {
            return new ArrayList<>();
        }
        
        try {
            String url = String.format(
                "%s/search?term=%s&latitude=%f&longitude=%f&radius=%d&limit=20",
                YELP_BASE_URL,
                URLEncoder.encode(term, StandardCharsets.UTF_8),
                latitude,
                longitude,
                radius
            );
            
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(apiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                String.class
            );
            
            return parseSearchResponse(response.getBody());
            
        } catch (Exception e) {
            System.err.println("Yelp API error: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Search for businesses by category
     */
    public List<YelpBusiness> searchByCategory(String category, 
                                             double latitude, 
                                             double longitude,
                                             int radius) {
        // Map TravelPath categories to Yelp categories
        String yelpCategory = mapCategoryToYelp(category);
        return searchBusinesses(yelpCategory, latitude, longitude, radius);
    }
    
    /**
     * Get detailed business information
     */
    public YelpBusiness getBusinessDetails(String businessId) {
        if (!enabled) {
            return null;
        }
        
        try {
            String url = YELP_BASE_URL + "/" + businessId;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(apiKey);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                String.class
            );
            
            return parseBusinessDetails(response.getBody());
            
        } catch (Exception e) {
            System.err.println("Yelp API error: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Find matching business by name and location
     */
    public YelpBusiness findMatchingBusiness(String placeName, 
                                            double latitude, 
                                            double longitude) {
        List<YelpBusiness> results = searchBusinesses(placeName, latitude, longitude, 500);
        
        if (results.isEmpty()) {
            return null;
        }
        
        // Return first result (closest match)
        return results.get(0);
    }
    
    private List<YelpBusiness> parseSearchResponse(String json) {
        List<YelpBusiness> businesses = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode businessesNode = root.get("businesses");
            
            if (businessesNode != null && businessesNode.isArray()) {
                for (JsonNode businessNode : businessesNode) {
                    businesses.add(parseBusiness(businessNode));
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing Yelp response: " + e.getMessage());
        }
        return businesses;
    }
    
    private YelpBusiness parseBusiness(JsonNode node) {
        YelpBusiness business = new YelpBusiness();
        
        business.setId(node.has("id") ? node.get("id").asText() : null);
        business.setName(node.has("name") ? node.get("name").asText() : null);
        business.setPrice(node.has("price") ? node.get("price").asText() : null);
        business.setRating(node.has("rating") ? node.get("rating").asDouble() : null);
        business.setReviewCount(node.has("review_count") ? node.get("review_count").asInt() : 0);
        business.setIsClosed(node.has("is_closed") ? node.get("is_closed").asBoolean() : true);
        
        // Coordinates
        if (node.has("coordinates")) {
            JsonNode coords = node.get("coordinates");
            business.setLatitude(coords.has("latitude") ? coords.get("latitude").asDouble() : null);
            business.setLongitude(coords.has("longitude") ? coords.get("longitude").asDouble() : null);
        }
        
        // Location
        if (node.has("location")) {
            JsonNode location = node.get("location");
            business.setAddress(location.has("address1") ? location.get("address1").asText() : null);
            business.setCity(location.has("city") ? location.get("city").asText() : null);
            business.setCountry(location.has("country") ? location.get("country").asText() : null);
            business.setZipCode(location.has("zip_code") ? location.get("zip_code").asText() : null);
            
            // Full display address
            if (location.has("display_address") && location.get("display_address").isArray()) {
                List<String> addressParts = new ArrayList<>();
                for (JsonNode addrPart : location.get("display_address")) {
                    addressParts.add(addrPart.asText());
                }
                business.setDisplayAddress(String.join(", ", addressParts));
            }
        }
        
        // Categories
        if (node.has("categories")) {
            List<String> categories = new ArrayList<>();
            for (JsonNode category : node.get("categories")) {
                if (category.has("title")) {
                    categories.add(category.get("title").asText());
                }
            }
            business.setCategories(categories);
        }
        
        // Hours (if available)
        if (node.has("hours") && node.get("hours").isArray() && node.get("hours").size() > 0) {
            JsonNode hoursNode = node.get("hours").get(0);
            if (hoursNode.has("open")) {
                business.setHours(parseHours(hoursNode.get("open")));
            }
            business.setIsOpenNow(hoursNode.has("is_open_now") ? hoursNode.get("is_open_now").asBoolean() : false);
        }
        
        // Phone
        business.setPhone(node.has("phone") ? node.get("phone").asText() : null);
        business.setDisplayPhone(node.has("display_phone") ? node.get("display_phone").asText() : null);
        
        return business;
    }
    
    private YelpBusiness parseBusinessDetails(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            return parseBusiness(node);
        } catch (Exception e) {
            System.err.println("Error parsing business details: " + e.getMessage());
            return null;
        }
    }
    
    private List<YelpHours> parseHours(JsonNode hoursArray) {
        List<YelpHours> hours = new ArrayList<>();
        try {
            for (JsonNode hourNode : hoursArray) {
                YelpHours hour = new YelpHours();
                hour.setDay(hourNode.has("day") ? hourNode.get("day").asInt() : null);
                hour.setStart(hourNode.has("start") ? hourNode.get("start").asText() : null);
                hour.setEnd(hourNode.has("end") ? hourNode.get("end").asText() : null);
                hour.setIsOvernight(hourNode.has("is_overnight") ? hourNode.get("is_overnight").asBoolean() : false);
                hours.add(hour);
            }
        } catch (Exception e) {
            System.err.println("Error parsing hours: " + e.getMessage());
        }
        return hours;
    }
    
    private String mapCategoryToYelp(String travelPathCategory) {
        // Map TravelPath categories to Yelp search terms
        return switch(travelPathCategory.toUpperCase()) {
            case "RESTAURANT" -> "restaurants";
            case "CULTURE" -> "museums";
            case "LEISURE" -> "parks";
            case "DISCOVERY" -> "attractions";
            default -> travelPathCategory.toLowerCase();
        };
    }
}
