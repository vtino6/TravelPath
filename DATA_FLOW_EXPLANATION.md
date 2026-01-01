# Data Flow and Preference Matching Explanation

## 📍 Where Places Come From

### 1. **External Data Source: OpenStreetMap (Overpass API)**
The backend fetches places from **OpenStreetMap** using the **Overpass API** (FREE, no API key needed).

**Flow:**
```
User selects activities → Android app → Backend API → PlacesService → OverpassClient → OpenStreetMap
```

**Details:**
- **OverpassClient** (`workspace/backend/src/main/java/com/travelpath/external/OverpassClient.java`)
  - Queries OpenStreetMap for places based on:
    - Location (latitude/longitude)
    - Radius (default: 5000 meters)
    - Category (RESTAURANT, LEISURE, DISCOVERY, CULTURE)
  
- **Category Mapping:**
  - `RESTAURANT` → searches for `amenity=restaurant`, `amenity=cafe`, `amenity=fast_food`
  - `LEISURE` → searches for `amenity=park`, `amenity=leisure`, `amenity=zoo`
  - `DISCOVERY` → searches for `tourism=*` (tourist attractions)
  - `CULTURE` → searches for `amenity=museum`, `amenity=arts_centre`, `amenity=library`

- **Data Retrieved:**
  - Name, coordinates, address
  - Estimated cost (based on place type)
  - Description (if available)
  - All stored in PostgreSQL database for caching

### 2. **Caching Strategy**
- **First check:** Database cache (PostgreSQL)
- **If empty:** Fetch from Overpass API
- **Save to DB:** All fetched places are cached for future requests

---

## 🎯 How Preferences Are Used in Route Generation

### Complete Flow:

```
1. User selects preferences (activities, budget, duration, effort level, location)
   ↓
2. User optionally selects specific places to include
   ↓
3. Android app sends RouteRequest to backend
   ↓
4. RouteGeneratorService.generateRoutes() processes:
   a. Fetch places for selected activities
   b. Filter by weather conditions
   c. Include required places (user selections)
   d. Filter by budget
   e. Generate 3 route types (ECONOMIC, BALANCED, COMFORT)
   f. Optimize order (nearest neighbor algorithm)
   g. Calculate distances using OpenRouteService
   h. Calculate total duration (visit time + walking time)
   ↓
5. Return 3 route options to user
```

### Detailed Preference Matching:

#### **1. Activity Selection** (`activities`)
```java
// For each selected activity category:
placesService.searchNearby(latitude, longitude, 5000, category)
// Fetches all places of that type within 5km radius
```

#### **2. Weather Filtering** (`coldSensitivity`, `heatSensitivity`, `humiditySensitivity`)
```java
// Get current weather
WeatherData weather = weatherService.getCurrentWeather(lat, lng);

// Filter places based on weather suitability
if (!isWeatherSuitable(weather, coldSens, heatSens, humiditySens)) {
    // Remove outdoor places if weather is bad
    filterOutOutdoorPlaces();
}
```

**Weather Logic:**
- **Cold sensitivity:** If temp < (15 - sensitivity × 2) → filter outdoor places
- **Heat sensitivity:** If temp > (25 + sensitivity × 2) → filter outdoor places  
- **Humidity sensitivity:** If humidity > (70 + sensitivity × 5) → filter outdoor places
- **Extreme weather:** Rain/Thunderstorm/Snow → filter if sensitivities are high

#### **3. Required Places** (`requiredPlaceIds`)
```java
// User-selected places are ALWAYS included
if (request.getRequiredPlaceIds() != null) {
    List<Place> requiredPlaces = fetchByIds(requiredPlaceIds);
    filteredPlaces.addAll(requiredPlaces);
}
```

#### **4. Budget Filtering** (`maxBudget`)
```java
// Filter out places that exceed budget
filtered = places.stream()
    .filter(p -> p.getAverageCost() <= maxBudget)
    .collect(Collectors.toList());
```

#### **5. Route Type Selection** (ECONOMIC, BALANCED, COMFORT)
```java
switch (routeType) {
    case ECONOMIC:
        // Sort by cost (cheapest first), limit to 5 places
        places.sortedByCost().limit(5)
        
    case BALANCED:
        // Take first 7 places (balanced selection)
        places.limit(7)
        
    case COMFORT:
        // Sort by cost (most expensive first), limit to 8 places
        places.sortedByCostDesc().limit(8)
}
```

#### **6. Order Optimization** (Nearest Neighbor Algorithm)
```java
// Uses OpenRouteService to get real walking distances
// Optimizes route to minimize total walking distance
// Starts from place closest to user's location
// Then visits nearest unvisited place at each step
```

#### **7. Duration Calculation**
```java
totalDuration = 
    sum(visitTime for each place) + 
    sum(walkingTime between places)

// Visit time: 60 minutes base + estimated wait time
// Walking time: from OpenRouteService distance matrix
```

#### **8. Effort Level** (`effortLevel`)
- Currently stored in route metadata
- Could be used to filter places by difficulty/accessibility (future enhancement)

---

## 🔄 Complete Data Flow Diagram

```
┌─────────────────┐
│  Android App    │
│  Preferences     │
│  Screen         │
└────────┬────────┘
         │
         │ 1. User selects activities
         ↓
┌─────────────────┐
│ PlacesRepository│
│ (Android)       │
└────────┬────────┘
         │
         │ 2. API call: GET /api/places/search
         ↓
┌─────────────────┐
│ PlacesController│
│ (Backend)       │
└────────┬────────┘
         │
         │ 3. Check database cache
         ↓
    ┌────┴────┐
    │ Cache?  │
    └────┬────┘
    Yes  │  No
    │    │
    │    ↓
    │ ┌─────────────────┐
    │ │ OverpassClient  │
    │ │ (OpenStreetMap) │
    │ └────────┬────────┘
    │          │
    │          │ 4. Query OpenStreetMap
    │          ↓
    │ ┌─────────────────┐
    │ │ OpenStreetMap   │
    │ │ (External API)  │
    │ └────────┬────────┘
    │          │
    │          │ 5. Return places
    │          ↓
    └──────────┴──────────┐
                          │
                          ↓
              ┌───────────────────┐
              │ Save to PostgreSQL│
              │ (Cache)            │
              └──────────┬────────┘
                         │
                         │ 6. Return to Android
                         ↓
              ┌───────────────────┐
              │ Display places     │
              │ in UI              │
              └───────────────────┘

┌─────────────────┐
│ User clicks     │
│ "Generate"      │
└────────┬────────┘
         │
         │ 7. POST /api/routes/generate
         ↓
┌─────────────────┐
│RouteGenerator   │
│Service          │
└────────┬────────┘
         │
         ├─→ 8a. Fetch places (from cache/Overpass)
         ├─→ 8b. Get weather (WeatherApiClient)
         ├─→ 8c. Filter by weather
         ├─→ 8d. Include required places
         ├─→ 8e. Filter by budget
         ├─→ 8f. Generate 3 route types
         ├─→ 8g. Optimize order (OpenRouteService)
         └─→ 8h. Calculate duration
         │
         ↓
┌─────────────────┐
│ Return 3 routes  │
│ to Android      │
└─────────────────┘
```

---

## 🛠️ External APIs Used

1. **Overpass API** (OpenStreetMap)
   - **Purpose:** Fetch places (restaurants, museums, parks, etc.)
   - **Cost:** FREE
   - **No API key needed**

2. **OpenRouteService**
   - **Purpose:** Calculate real walking distances and routes
   - **Cost:** FREE (with rate limits)
   - **Used for:** Distance matrix, route optimization

3. **Weather API** (via WeatherApiClient)
   - **Purpose:** Get current weather conditions
   - **Used for:** Filtering places based on weather sensitivity

---

## 📊 Example: How a Route is Generated

**User Preferences:**
- Activities: RESTAURATION, CULTURE
- Budget: 50€
- Duration: 4 hours
- Location: Paris (48.8566, 2.3522)
- Selected places: ["Louvre Museum", "Eiffel Tower"]

**Backend Process:**
1. Fetch all restaurants within 5km of Paris → ~50 places
2. Fetch all cultural places within 5km → ~30 places
3. Get current weather → 18°C, Clear
4. Filter by weather → All places OK (good weather)
5. Include required places → Add Louvre + Eiffel Tower
6. Filter by budget → Remove places > 50€
7. Generate routes:
   - **ECONOMIC:** 5 cheapest places
   - **BALANCED:** 7 places (mix)
   - **COMFORT:** 8 most expensive places
8. Optimize order for each route (minimize walking)
9. Calculate total duration (visit + walking time)
10. Return 3 route options

---

## ✅ Summary

- **Places come from:** OpenStreetMap (via Overpass API) - FREE, no API key
- **Preferences are used:** 
  - Activities → determine which places to fetch
  - Budget → filter expensive places
  - Weather sensitivities → filter outdoor places in bad weather
  - Selected places → always included
  - Duration → limits number of places
  - Route type → determines selection strategy (cheap vs. balanced vs. premium)
- **Optimization:** Routes are optimized for minimal walking distance using real distance data




