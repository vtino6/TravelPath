# Constraint-Driven Design Implementation

## Overview

The TravelPath application now implements a **constraint-driven design** where:
- Users specify **constraints** (budget, time, effort level, activities, weather)
- The system **derives** the number of places, visit order, and travel time
- Routes are **validated** against constraints and discarded if they exceed limits
- All costs and durations are clearly marked as **estimates**

---

## Backend Implementation

### 1. Constraint-Driven Place Selection

**Location:** `RouteGeneratorService.selectPlacesByType()`

**How it works:**
- **Number of places is DERIVED** from constraints, not user-specified
- Calculates max places based on:
  - **Time constraint**: `usableTime / (visitTime + travelTime)`
  - **Budget constraint**: `usableBudget / averagePlaceCost`
- Takes the **minimum** of both constraints
- Applies route type multiplier (ECONOMIC: 0.8x, BALANCED: 1.0x, COMFORT: 1.2x)

**Assumptions:**
- Average visit time: 60 minutes per place
- Average travel time: 15 minutes between places
- 80% of available time is usable (20% buffer)
- 90% of budget for places, 10% for transport

### 2. Transport Cost Estimation

**Location:** `RouteGeneratorService.estimateTransportCost()`

**Cost Logic:**
- **Walking (< 2km)**: Free
- **Public transport (2-5km)**: 2.50€ per trip
- **Taxi/Uber (> 5km)**: 10€ per trip
- **Food/refreshments**: 5€ per place (estimated)

**Total transport cost** = sum of all transport segments + food estimates

### 3. Constraint Validation

**Location:** `RouteGeneratorService.isRouteValid()`

**Validation Rules:**
- **Budget constraint**: Route must not exceed `maxBudget * 1.1` (10% tolerance)
- **Time constraint**: Route must not exceed `duration * 1.2` (20% tolerance)
- **Minimum places**: Route must have at least 2 places

**Routes that exceed constraints are DISCARDED** and not returned to the user.

### 4. Route Variants

**Location:** `RouteGeneratorService.generateRoutes()`

**Generates 2-3 variants:**
1. **ECONOMIC** - Budget-optimized (fewer places, cheaper)
2. **BALANCED** - Time and budget balanced
3. **COMFORT** - Experience-optimized (more places, premium)

Each variant is validated against constraints before being returned.

---

## UI Implementation

### Preferences Screen

**All inputs are clearly labeled as "Contrainte:" (Constraint):**

1. **Contrainte: Activity Types** (multi-select)
   - Users select which activity types they want
   - No limit on number of activities

2. **Contrainte: Maximum Budget** (slider)
   - Shows: "X€ (estimation)"
   - Helper text: "Le nombre de lieux sera calculé automatiquement"
   - Makes it clear that number of places is derived

3. **Contrainte: Available Time** (presets: 4h, 8h, 12h)
   - Time constraint for route generation

4. **Contrainte: Effort Level** (low/medium/high)
   - Affects route optimization

5. **Weather Sensitivity** (toggles)
   - Cold, heat, humidity sensitivity

**No input for number of places** - this is derived automatically.

### Results Screen (Route Selection)

**Each route card displays:**

1. **Total Estimated Cost** 💰
   - Shows: "X€ (estimation)"
   - Includes: activities + transport + food

2. **Total Duration** ⏱️
   - Shows: "Xh (estimation)"
   - Includes: visit time + travel time

3. **Number of Places** 📍
   - Shows: "X lieux (calculé automatiquement)"
   - **Derived** from constraints, not user-specified

4. **Effort Level** 💪
   - Low / Medium / High

5. **Route Type**
   - "Économique" (Economical)
   - "Équilibré" (Balanced)
   - "Confort" (Comfort)

**All costs and durations are clearly marked as estimates.**

---

## Key Design Decisions

### 1. Number of Places Derivation

**Formula:**
```
maxPlacesByTime = (availableTime * 0.8) / (60 + 15)
maxPlacesByBudget = (maxBudget * 0.9) / averagePlaceCost
derivedMaxPlaces = min(maxPlacesByTime, maxPlacesByBudget) * routeTypeMultiplier
```

**Why:**
- Ensures routes fit within user's constraints
- No arbitrary limits
- Adapts to user's budget and time availability

### 2. Transport Cost Estimation

**Assumptions:**
- Walking is free (encourages local exploration)
- Public transport for medium distances (2-5km)
- Taxi for long distances (> 5km)
- Food/refreshments: 5€ per place (standard estimate)

**Why estimation:**
- Real-time transport pricing APIs are expensive/complex
- Estimates are sufficient for route planning
- Users understand these are estimates

### 3. Constraint Validation

**Tolerance levels:**
- Budget: 10% tolerance (for estimation variance)
- Time: 20% tolerance (for breaks/buffer)

**Why:**
- Allows for estimation inaccuracies
- Prevents routes that are clearly over budget/time
- Still provides flexibility

### 4. Route Variants

**Three variants provide:**
- **ECONOMIC**: Maximum value (fewer places, cheaper)
- **BALANCED**: Best compromise
- **COMFORT**: Maximum experience (more places, premium)

**Each variant:**
- Derived from same constraints
- Validated independently
- May be discarded if it exceeds constraints

---

## Estimation Assumptions (Documented in Code)

### Cost Estimation:
- Fast food: 10€
- Cafe: 15€
- Restaurant: 25€ (regular), 50€ (fine dining)
- Museum: 12€ (average)
- Transport: 2.50€ (public), 10€ (taxi)
- Food/refreshments: 5€ per place

### Time Estimation:
- Visit time: 60 minutes per place (base)
- Wait time: Variable (from place data)
- Travel time: 15 minutes average (walking)
- Buffer: 20% of total time reserved

### Distance Estimation:
- Walking: < 2km (free)
- Public transport: 2-5km (2.50€)
- Taxi: > 5km (10€)

---

## Code Comments

All constraint-driven logic is documented with comments explaining:
- What constraint is being applied
- How the derivation works
- What assumptions are made
- Why certain values are used

Example:
```java
// CONSTRAINT-DRIVEN: Derive number of places from available time and budget
// Assumptions:
// - Average visit time: 60 minutes per place
// - Average travel time between places: 15 minutes (walking)
// - Reserve 20% of time for buffer/breaks
```

---

## Testing the Constraint-Driven Design

### Test Case 1: Tight Budget
- Budget: 30€
- Time: 4 hours
- Expected: Fewer places, cheaper options

### Test Case 2: Tight Time
- Budget: 100€
- Time: 2 hours
- Expected: Fewer places, closer together

### Test Case 3: Generous Constraints
- Budget: 200€
- Time: 8 hours
- Expected: More places, variety of options

### Test Case 4: Constraint Violation
- Budget: 20€
- Time: 1 hour
- Expected: May return fewer routes (some variants discarded)

---

## Summary

✅ **Number of places**: Derived from constraints (not user input)
✅ **Visit order**: Optimized using nearest-neighbor algorithm
✅ **Travel time**: Calculated using OpenRouteService (walking)
✅ **Cost estimation**: Activities + transport + food
✅ **Constraint validation**: Routes exceeding constraints are discarded
✅ **Route variants**: 2-3 variants (economical/balanced/comfort)
✅ **UI labels**: All inputs clearly labeled as constraints
✅ **Estimate indicators**: Costs and durations marked as estimates
✅ **Derived metrics**: Number of places shown as "calculated automatically"

The system now fully implements constraint-driven design with clear documentation of all assumptions and estimation logic.




