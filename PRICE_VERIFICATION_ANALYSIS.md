# Price Verification Analysis

## Current Situation: Prices are ESTIMATED, Not Verified ❌

### How It Currently Works:

```java
// In OverpassClient.java - estimateCost()
private Double estimateCost(Map<String, String> tags) {
    String amenity = tags.get("amenity");
    if ("fast_food".equals(amenity)) return 10.0;      // Fixed estimate
    if ("cafe".equals(amenity)) return 15.0;           // Fixed estimate
    if ("restaurant".equals(amenity)) {
        if (tags.containsKey("cuisine") && tags.get("cuisine").contains("fine")) {
            return 50.0;  // Fixed estimate for "fine dining"
        }
        return 25.0;      // Fixed estimate for regular restaurant
    }
    return null;  // No price for other types
}
```

### Problems with Current Approach:

1. **No Real Data** - Prices are hardcoded estimates
2. **Not Location-Specific** - Same price for Paris and small towns
3. **Not Currency-Aware** - Assumes euros, but no conversion
4. **No Updates** - Prices never change
5. **No Verification** - Can't verify if estimates are accurate
6. **Missing Data** - Many places return `null` (no price)

### Impact on Route Generation:

- **Budget filtering** may be inaccurate
- **ECONOMIC vs COMFORT** routes may not reflect real prices
- **Total budget** calculations are estimates, not real costs

---

## How to Get Real Prices

### Option 1: Google Places API (Best Option) ✅

**What it provides:**
- `price_level` (1-4 scale, relative to area)
- Real-time data
- Location-specific pricing

**Implementation:**
```java
// Google Places API returns:
{
  "price_level": 2,  // 1=inexpensive, 2=moderate, 3=expensive, 4=very expensive
  "rating": 4.5,
  "user_ratings_total": 1234
}

// Convert to actual cost estimate:
private Double convertPriceLevelToCost(int priceLevel, String category, String city) {
    // Base prices by city (Paris is more expensive than Lyon)
    double cityMultiplier = getCityMultiplier(city);
    
    // Base prices by category
    double basePrice = switch(category) {
        case RESTAURANT -> switch(priceLevel) {
            case 1 -> 15.0;  // Inexpensive restaurant
            case 2 -> 30.0;  // Moderate restaurant
            case 3 -> 60.0;  // Expensive restaurant
            case 4 -> 100.0; // Very expensive restaurant
            default -> 30.0;
        };
        case CULTURE -> switch(priceLevel) {
            case 1 -> 5.0;   // Free/low-cost museum
            case 2 -> 12.0;  // Moderate museum
            case 3 -> 20.0;  // Expensive museum
            case 4 -> 30.0;  // Premium museum
            default -> 12.0;
        };
        // ... other categories
    };
    
    return basePrice * cityMultiplier;
}
```

**Cost:** ~$0.017 per request (Places API)
**Pros:** Real data, location-specific, updated
**Cons:** Costs money, requires API key

---

### Option 2: User-Generated Price Data ✅

**How it works:**
- Users can report/update prices after visiting
- Store in your database
- Use for future route generation

**Implementation:**
```java
@Entity
public class PlacePrice {
    @Id
    private String id;
    
    private String placeId;
    private Double reportedPrice;
    private String currency;  // EUR, USD, etc.
    private LocalDateTime reportedAt;
    private String reportedBy;  // User ID
    
    // Aggregate multiple reports
    private Double averagePrice;
    private Integer reportCount;
}
```

**Pros:** Free, real data, improves over time
**Cons:** Requires users, may be incomplete initially

---

### Option 3: Web Scraping (Not Recommended) ⚠️

**What it involves:**
- Scrape restaurant websites
- Scrape booking sites (TripAdvisor, Yelp)
- Parse menu prices

**Pros:** Free, real data
**Cons:** 
- Legally questionable
- Breaks easily (website changes)
- Rate limiting issues
- Maintenance nightmare

---

### Option 4: Third-Party Price APIs

**Services:**
- **Yelp Fusion API** - Has price indicators ($, $$, $$$, $$$$)
- **Foursquare Places API** - Price tier data
- **Zomato API** - Restaurant prices (limited regions)

**Example (Yelp):**
```java
// Yelp API returns:
{
  "price": "$$",  // $ = inexpensive, $$ = moderate, $$$ = expensive, $$$$ = very expensive
  "rating": 4.5
}

// Convert to cost:
private Double yelpPriceToCost(String price) {
    return switch(price) {
        case "$" -> 15.0;
        case "$$" -> 30.0;
        case "$$$" -> 60.0;
        case "$$$$" -> 100.0;
        default -> 30.0;
    };
}
```

**Cost:** Varies (some free tiers available)
**Pros:** Real data, multiple sources
**Cons:** May not cover all regions

---

### Option 5: Hybrid Approach (Recommended) ✅

**Combine multiple sources:**

```java
public Double getPlacePrice(Place place) {
    // 1. Check user-reported prices (most accurate)
    PlacePrice userPrice = placePriceRepository.findByPlaceId(place.getId());
    if (userPrice != null && userPrice.getReportCount() >= 3) {
        return userPrice.getAveragePrice();  // Use if enough reports
    }
    
    // 2. Check Google Places API (if available)
    if (googlePlacesEnabled) {
        Integer priceLevel = googlePlacesClient.getPriceLevel(place.getId());
        if (priceLevel != null) {
            return convertPriceLevelToCost(priceLevel, place.getCategory(), place.getCity());
        }
    }
    
    // 3. Check Yelp/Foursquare (if available)
    if (yelpEnabled) {
        String priceTier = yelpClient.getPriceTier(place.getId());
        if (priceTier != null) {
            return yelpPriceToCost(priceTier);
        }
    }
    
    // 4. Fallback to estimation (current method)
    return estimateCostFromTags(place);
}
```

---

## Implementation Plan

### Phase 1: Improve Estimation (Quick Win) ✅

**Current:** Fixed prices regardless of location
**Improvement:** Location-aware estimation

```java
private Double estimateCost(Map<String, String> tags, double latitude, double longitude) {
    // Get city/country from coordinates
    String city = geocodingService.getCity(latitude, longitude);
    double cityMultiplier = getCityCostMultiplier(city);
    
    // Base price by amenity type
    double basePrice = getBasePriceByAmenity(tags.get("amenity"));
    
    return basePrice * cityMultiplier;
}

private double getCityCostMultiplier(String city) {
    // Paris, London, NYC = expensive (1.5x)
    // Lyon, Marseille = moderate (1.0x)
    // Small towns = cheap (0.7x)
    return switch(city.toLowerCase()) {
        case "paris", "london", "new york" -> 1.5;
        case "lyon", "marseille", "toulouse" -> 1.0;
        default -> 0.7;
    };
}
```

### Phase 2: Add Google Places Integration ✅

**When:** Budget allows (~$34/month for 1,000 users)

```java
@Service
public class PriceService {
    
    @Autowired
    private GooglePlacesClient googlePlacesClient;
    
    public Double getPrice(String placeId, PlaceCategory category, String city) {
        // Try Google Places first
        try {
            Integer priceLevel = googlePlacesClient.getPriceLevel(placeId);
            if (priceLevel != null) {
                return convertPriceLevelToCost(priceLevel, category, city);
            }
        } catch (Exception e) {
            // Fallback to estimation
        }
        
        // Fallback to improved estimation
        return estimateCost(category, city);
    }
}
```

### Phase 3: Add User Price Reporting ✅

**When:** You have active users

```java
@PostMapping("/places/{id}/price")
public ResponseEntity<Void> reportPrice(
    @PathVariable String id,
    @RequestBody PriceReport report
) {
    // Save user report
    placePriceService.addPriceReport(id, report.getPrice(), report.getUserId());
    
    // Update place average if enough reports
    if (placePriceService.getReportCount(id) >= 3) {
        Place place = placeRepository.findById(id).orElse(null);
        if (place != null) {
            place.setAverageCost(placePriceService.getAveragePrice(id));
            placeRepository.save(place);
        }
    }
    
    return ResponseEntity.ok().build();
}
```

---

## Currency Handling

### Current: Assumes Euros ❌

**Problem:** No currency conversion, assumes all prices in EUR

### Solution: Multi-Currency Support ✅

```java
@Entity
public class Place {
    private Double averageCost;
    private String currency;  // EUR, USD, GBP, etc.
    
    // Convert to user's preferred currency
    public Double getCostInCurrency(String targetCurrency) {
        if (this.currency.equals(targetCurrency)) {
            return this.averageCost;
        }
        return currencyConverter.convert(this.averageCost, this.currency, targetCurrency);
    }
}
```

**Free Currency APIs:**
- **ExchangeRate-API** (free tier: 1,500 requests/month)
- **Fixer.io** (free tier: 100 requests/month)
- **CurrencyLayer** (free tier: 1,000 requests/month)

---

## Recommendations

### Short Term (Now):
1. ✅ **Improve estimation** - Add location-based multipliers
2. ✅ **Add currency field** - Store currency with price
3. ✅ **Show price as "estimate"** - Be transparent with users

### Medium Term (When you have budget):
1. ✅ **Add Google Places API** - Get real price_level data
2. ✅ **Convert price_level to cost** - Use city-specific conversion
3. ✅ **Cache prices** - Don't call API for every request

### Long Term (When you have users):
1. ✅ **User price reporting** - Let users update prices
2. ✅ **Price validation** - Flag suspicious reports
3. ✅ **Price trends** - Track price changes over time

---

## Code Changes Needed

### 1. Improve Estimation (Immediate)

```java
// OverpassClient.java
private Double estimateCost(Map<String, String> tags, double lat, double lng) {
    String city = getCityFromCoordinates(lat, lng);
    double multiplier = getCityMultiplier(city);
    
    String amenity = tags.get("amenity");
    double basePrice = switch(amenity) {
        case "fast_food" -> 10.0;
        case "cafe" -> 15.0;
        case "restaurant" -> tags.get("cuisine").contains("fine") ? 50.0 : 25.0;
        case "museum" -> 12.0;
        default -> 20.0;
    };
    
    return basePrice * multiplier;
}
```

### 2. Add Price Source Tracking

```java
@Entity
public class Place {
    private Double averageCost;
    private PriceSource priceSource;  // ESTIMATED, GOOGLE_PLACES, USER_REPORTED
    private LocalDateTime priceUpdatedAt;
}
```

### 3. Show Price Confidence

```kotlin
// Android UI
Text(
    text = if (place.priceSource == PriceSource.ESTIMATED) {
        "~${place.averageCost}€ (estimation)"
    } else {
        "${place.averageCost}€"
    },
    style = if (place.priceSource == PriceSource.ESTIMATED) {
        TextStyle(fontStyle = FontStyle.Italic, color = Color.Gray)
    } else {
        TextStyle.Default
    }
)
```

---

## Summary

**Current State:** ❌ Prices are **estimated**, not verified
- Fixed prices regardless of location
- No real data from APIs
- No user input

**Recommended Path:**
1. **Now:** Improve estimation with location multipliers
2. **Phase 2:** Add Google Places API for real price_level data
3. **Phase 3:** Add user price reporting for accuracy

**Key Insight:** 
You don't need perfect prices for MVP. Estimates are fine, but:
- Be transparent ("~30€ (estimation)")
- Improve over time with real data
- Let users help by reporting prices

The most important thing is that your **route generation algorithm works**, even with estimated prices. You can improve price accuracy gradually.




