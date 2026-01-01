# Debugging Route Generation - Step by Step

## What I Added

I've added comprehensive logging at every step of the route generation process:

### Android Side (Logcat):
- `LoadingScreen` - Logs when route generation starts
- `RoutesRepository` - Logs the request being sent
- Shows all request parameters (activities, location, budget, etc.)

### Backend Side (Console):
- `RoutesController` - Logs when request is received
- `RouteGeneratorService` - Logs each step of generation
- Shows places found, weather data, route generation progress

---

## How to Debug

### Step 1: Check Android Logcat

1. Open Android Studio
2. Go to **Logcat** tab
3. Filter by: `LoadingScreen` or `RoutesRepository`
4. Click "Generate Itinerary" button
5. You should see:

```
D/LoadingScreen: === STARTING ROUTE GENERATION ===
D/LoadingScreen: Built RouteRequest:
D/LoadingScreen:   Activities: [RESTAURANT]
D/LoadingScreen:   Location: 48.8566, 2.3522
D/LoadingScreen:   Budget: 50.0
D/LoadingScreen:   Duration: 240
D/RoutesRepository: === SENDING ROUTE GENERATION REQUEST ===
D/RoutesRepository: Calling API: POST /api/routes/generate
```

**If you DON'T see these logs:**
- The button click isn't working
- Check if you selected at least one activity
- Check if the navigation is working

**If you see an error:**
- Note the error message
- Check if backend is running
- Check network connectivity

### Step 2: Check Backend Console

1. Look at your Spring Boot console (where you ran `mvn spring-boot:run`)
2. When you click "Generate", you should see:

```
=== ROUTE GENERATION REQUEST RECEIVED ===
Location: 48.8566, 2.3522
Activities: [RESTAURANT]
Budget: 50.0
Duration: 240
Effort Level: EASY
Required Places: []
Starting route generation...
[RouteGeneratorService] Starting route generation...
[RouteGeneratorService] Fetching places for 1 activities...
[RouteGeneratorService] Searching places for category: RESTAURANT
[RouteGeneratorService] Found X places for category RESTAURANT
[RouteGeneratorService] Total places found: X
[RouteGeneratorService] Getting weather data...
[RouteGeneratorService] Weather: 18.0°C, Clear
[RouteGeneratorService] Places after weather filter: X
[RouteGeneratorService] Generating 3 route types...
[RouteGeneratorService] ECONOMIC route generated
[RouteGeneratorService] BALANCED route generated
[RouteGeneratorService] COMFORT route generated
Route generation completed. Generated 3 routes.
```

**If you DON'T see these logs:**
- The request isn't reaching the backend
- Check if backend is running on port 8080
- Check if Android can reach `10.0.2.2:8080` (emulator) or your computer's IP
- Check CORS configuration

**If you see an error:**
- Note the error message and stack trace
- Common issues:
  - Database connection error
  - External API errors (Overpass, OpenRouteService)
  - Missing data

---

## Common Issues and Solutions

### Issue 1: No logs in Android Logcat

**Possible causes:**
- Button click not working
- Navigation not happening
- ViewModel not initialized

**Solution:**
- Check if you selected at least one activity
- Check if the button is actually clickable
- Verify navigation to "loading" screen

### Issue 2: Android logs show request, but backend doesn't receive it

**Possible causes:**
- Backend not running
- Wrong URL in RetrofitClient
- Network connectivity issue
- CORS blocking the request

**Solution:**
1. Verify backend is running: `curl http://localhost:8080/api/routes/generate` (should return 405 Method Not Allowed, not connection error)
2. Check `RetrofitClient.kt` - URL should be `http://10.0.2.2:8080/api/` for emulator
3. For physical device, use your computer's IP: `http://192.168.x.x:8080/api/`
4. Check backend CORS configuration

### Issue 3: Backend receives request but fails

**Possible causes:**
- Database connection error
- External API errors
- Missing required data

**Solution:**
1. Check database is running: `psql -U travelpath_user -d travelpath_db`
2. Check external API keys (OpenRouteService, Weather API)
3. Look at the full stack trace in backend console

### Issue 4: Backend processes but returns empty routes

**Possible causes:**
- No places found for selected activities
- All places filtered out by weather/budget
- Error in route generation

**Solution:**
1. Check logs: "Total places found: X"
2. If X = 0, check if Overpass API is working
3. Check weather filter logs
4. Try different activities or location

---

## Testing the Backend Directly

You can test if the backend is working by sending a request directly:

```bash
curl -X POST http://localhost:8080/api/routes/generate \
  -H "Content-Type: application/json" \
  -d '{
    "latitude": 48.8566,
    "longitude": 2.3522,
    "activities": ["RESTAURANT"],
    "maxBudget": 50.0,
    "duration": 240,
    "effortLevel": "EASY",
    "coldSensitivity": 0,
    "heatSensitivity": 0,
    "humiditySensitivity": 0,
    "requiredPlaceIds": []
  }'
```

**Expected response:**
- JSON array with 3 route objects
- Each route should have steps, budget, duration

**If you get an error:**
- Check the error message
- Verify database is running
- Check external API keys

---

## Quick Checklist

Before debugging, verify:

- [ ] Backend is running (`mvn spring-boot:run`)
- [ ] Database is running (PostgreSQL)
- [ ] At least one activity is selected in the app
- [ ] Location is set (defaults to Paris)
- [ ] Android emulator can reach backend (test with browser: `http://10.0.2.2:8080/api/routes/generate`)
- [ ] Logcat is open and filtered correctly
- [ ] Backend console is visible

---

## Next Steps

1. **Click "Generate Itinerary"**
2. **Check Android Logcat** - Do you see the request being sent?
3. **Check Backend Console** - Do you see the request being received?
4. **Share the logs** - This will help identify exactly where it's failing

The logs will show you exactly where the process stops, making it much easier to fix the issue!
