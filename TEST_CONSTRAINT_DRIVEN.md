# Testing Constraint-Driven Design

## Quick Verification Checklist

### ✅ Backend Compilation
- [x] No compilation errors
- [x] All constraint logic implemented
- [x] Edge cases handled (empty routes, tight constraints)

### ✅ UI Updates
- [x] All inputs labeled as "Contrainte:"
- [x] Estimates clearly marked
- [x] Number of places shown as "calculé automatiquement"

---

## How to Test

### Test 1: Basic Constraint-Driven Generation

**Steps:**
1. Open app → Preferences screen
2. Select at least one activity (e.g., "Restauration")
3. Set budget: 50€ (slider)
4. Set time: 4 hours
5. Set effort level: Facile
6. Click "Générer l'itinéraire"

**Expected:**
- Loading screen appears
- Backend console shows:
  ```
  [RouteGeneratorService] Derived max places: X (time constraint: Y, budget constraint: Z)
  [RouteGeneratorService] ECONOMIC route generated and validated
  [RouteGeneratorService] BALANCED route generated and validated
  [RouteGeneratorService] COMFORT route generated and validated
  ```
- Route selection screen shows 2-3 routes
- Each route shows:
  - "X€ (estimation)"
  - "Xh (estimation)"
  - "X lieux (calculé automatiquement)"

### Test 2: Tight Budget Constraint

**Steps:**
1. Set budget: 20€ (very tight)
2. Set time: 4 hours
3. Select activities
4. Generate

**Expected:**
- Fewer places in routes (budget constraint limits)
- Routes may be filtered out if they exceed budget
- Backend logs show: "Derived max places: X (budget constraint: Y)"

### Test 3: Tight Time Constraint

**Steps:**
1. Set budget: 100€
2. Set time: 2 hours (very tight)
3. Select activities
4. Generate

**Expected:**
- Fewer places in routes (time constraint limits)
- Routes may be filtered out if they exceed time
- Backend logs show: "Derived max places: X (time constraint: Y)"

### Test 4: No Routes Found (All Exceed Constraints)

**Steps:**
1. Set budget: 5€ (extremely tight)
2. Set time: 1 hour (extremely tight)
3. Select activities
4. Generate

**Expected:**
- Route selection screen shows: "Aucun itinéraire trouvé"
- Message: "Aucun itinéraire ne respecte vos contraintes..."
- Button: "Modifier les contraintes"

---

## What to Check in Backend Console

When you click "Generate", you should see:

```
=== ROUTE GENERATION REQUEST RECEIVED ===
Location: 48.8566, 2.3522
Activities: [RESTAURANT]
Budget: 50.0
Duration: 240
[RouteGeneratorService] Starting route generation...
[RouteGeneratorService] Fetching places for 1 activities...
[RouteGeneratorService] Searching places for category: RESTAURANT
[RouteGeneratorService] Found X places for category RESTAURANT
[RouteGeneratorService] Total places found: X
[RouteGeneratorService] Getting weather data...
[RouteGeneratorService] Weather: 18.0°C, Clear
[RouteGeneratorService] Places after weather filter: X
[RouteGeneratorService] Generating route variants based on constraints...
[RouteGeneratorService] Derived max places: X (time constraint: Y, budget constraint: Z)
[RouteGeneratorService] ECONOMIC route generated and validated
[RouteGeneratorService] BALANCED route generated and validated
[RouteGeneratorService] COMFORT route generated and validated
Route generation completed. Generated 3 routes.
```

**Key indicators:**
- ✅ "Derived max places" - Shows constraint-driven calculation
- ✅ "route generated and validated" - Route passed constraint checks
- ✅ "route discarded" - Route exceeded constraints (expected in some cases)

---

## What to Check in Android Logcat

Filter by: `LoadingScreen` or `RoutesRepository`

**Expected logs:**
```
D/LoadingScreen: === STARTING ROUTE GENERATION ===
D/LoadingScreen: Testing backend connectivity...
D/LoadingScreen: Backend test successful: {status=ok, ...}
D/LoadingScreen: Built RouteRequest:
D/LoadingScreen:   Activities: [RESTAURANT]
D/LoadingScreen:   Location: 48.8566, 2.3522
D/LoadingScreen:   Budget: 50.0
D/LoadingScreen:   Duration: 240
D/RoutesRepository: === SENDING ROUTE GENERATION REQUEST ===
D/RoutesRepository: Calling API: POST /api/routes/generate
D/RoutesRepository: API call successful. Received 3 routes.
```

---

## Verification Points

### ✅ Constraint-Driven Features

1. **Number of places is derived:**
   - Check backend logs: "Derived max places: X"
   - Check UI: "X lieux (calculé automatiquement)"
   - Should NOT be hardcoded (5, 7, 8)

2. **Transport costs included:**
   - Total budget should be higher than sum of place costs
   - Check backend logs for transport cost calculation

3. **Constraint validation:**
   - Routes exceeding constraints are discarded
   - Backend logs show: "route discarded (exceeds constraints)"

4. **UI shows estimates:**
   - Budget: "X€ (estimation)"
   - Duration: "Xh (estimation)"
   - Number of places: "(calculé automatiquement)"

5. **Inputs labeled as constraints:**
   - "Contrainte: Activity Types"
   - "Contrainte: Maximum Budget"
   - "Contrainte: Available Time"
   - "Contrainte: Effort Level"

---

## Common Issues

### Issue: "No routes found" even with reasonable constraints

**Possible causes:**
- No places found for selected activities
- All places filtered out by weather
- Constraints too tight (try increasing budget/time)

**Check:**
- Backend logs: "Total places found: X"
- If X = 0, check Overpass API connectivity

### Issue: Routes always have same number of places

**Possible causes:**
- Constraint calculation not working
- Check backend logs: "Derived max places" should vary with constraints

### Issue: Routes exceed budget/time

**Possible causes:**
- Transport costs not calculated correctly
- Visit time estimation too low
- Check backend logs for warnings

---

## Expected Behavior Summary

✅ **Works correctly if:**
1. Backend receives request (check console)
2. Places are found (check logs: "Total places found: X")
3. Routes are generated (check logs: "route generated and validated")
4. UI shows routes with estimates clearly marked
5. Number of places varies based on constraints

❌ **Not working if:**
1. Backend console shows nothing (request not reaching backend)
2. "Total places found: 0" (no places available)
3. All routes discarded (constraints too tight)
4. Number of places always the same (constraint calculation not working)

---

## Quick Test Command

Test backend directly:
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

**Expected:** JSON array with 2-3 route objects, each with:
- `totalBudget` (includes transport)
- `totalDuration` (includes travel time)
- `steps` array (number of places - DERIVED, not fixed)




