# Backend Connection Fix

## Issue
Error: "Impossible de se connecter au serveur. Vérifiez que le backend est démarré."

## Root Cause
The backend is running, but the `RoutesController` had `@RequestMapping("/api/routes")` which, combined with `server.servlet.context-path=/api` in `application.properties`, creates a double `/api` prefix.

## Fix Applied
Changed `RoutesController` from:
```java
@RequestMapping("/api/routes")  // Wrong - creates /api/api/routes
```

To:
```java
@RequestMapping("/routes")  // Correct - creates /api/routes (context-path adds /api)
```

## Action Required: Restart Backend

**The backend MUST be restarted for changes to take effect:**

1. **Stop the current backend:**
   - Press `Ctrl+C` in the terminal where backend is running
   - Or kill the process: `kill 24670` (use the PID from `lsof -i :8080`)

2. **Restart the backend:**
   ```bash
   cd workspace/backend
   mvn spring-boot:run
   ```

3. **Verify it's working:**
   ```bash
   curl http://localhost:8080/api/routes/test
   ```
   
   **Expected response:**
   ```json
   {"status":"ok","message":"Backend is running","timestamp":"..."}
   ```

4. **Test route generation:**
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

## After Restart

Once the backend is restarted:
1. The test endpoint should work: `/api/routes/test`
2. Route generation should work: `/api/routes/generate`
3. Android app should be able to connect

## Current Status

✅ Backend is running (port 8080 in use)
✅ Code fix applied (removed double `/api` prefix)
⏳ **Backend needs restart** for changes to take effect

## Quick Verification

After restarting, check:
- `curl http://localhost:8080/api/routes/test` → Should return JSON
- Android app → Should connect successfully
- Backend console → Should show "TEST ENDPOINT CALLED" when app connects




