# Backend Connectivity Debugging Guide

## Issue: Nothing Happening on Backend

If the backend isn't receiving requests, follow these steps:

---

## Step 1: Verify Backend is Running

### Check if backend is running:
```bash
# In your backend directory
cd workspace/backend
mvn spring-boot:run
```

**You should see:**
```
Started TravelPathApplication in X.XXX seconds
```

### Test backend directly:
```bash
# Test endpoint (should return JSON)
curl http://localhost:8080/api/routes/test

# Expected response:
{"status":"ok","message":"Backend is running","timestamp":"2024-..."}
```

**If this doesn't work:**
- Backend isn't running
- Wrong port (check `application.properties`)
- Database connection issue

---

## Step 2: Verify Network Connectivity

### For Android Emulator:
- URL should be: `http://10.0.2.2:8080/api/`
- `10.0.2.2` is the special IP that maps to `localhost` on your computer

### Test from emulator:
```bash
# In Android Studio Terminal or ADB shell
adb shell
curl http://10.0.2.2:8080/api/routes/test
```

**If this doesn't work:**
- Emulator can't reach your computer
- Firewall blocking port 8080
- Backend not running

### For Physical Device:
- URL should be: `http://YOUR_COMPUTER_IP:8080/api/`
- Find your IP: `ifconfig` (Mac/Linux) or `ipconfig` (Windows)
- Example: `http://192.168.1.100:8080/api/`

**Important:** Both device and computer must be on the same WiFi network!

---

## Step 3: Check Android Logcat

### What to look for:

1. **Backend Test:**
```
D/LoadingScreen: Testing backend connectivity...
D/LoadingScreen: Backend test successful: {status=ok, message=Backend is running, ...}
```

**If you see:**
```
E/LoadingScreen: Backend test FAILED
```
→ Backend is not reachable

2. **Route Generation Request:**
```
D/RoutesRepository: === SENDING ROUTE GENERATION REQUEST ===
D/RoutesRepository: Calling API: POST /api/routes/generate
```

**If you DON'T see this:**
→ Request isn't being sent (check if activities are selected)

3. **Network Logs:**
```
D/OkHttp: --> POST http://10.0.2.2:8080/api/routes/generate
D/OkHttp: <-- 200 OK http://10.0.2.2:8080/api/routes/generate (XXXms)
```

**If you see:**
```
D/OkHttp: --> POST http://10.0.2.2:8080/api/routes/generate
D/OkHttp: <-- FAILED: java.net.ConnectException: Failed to connect to 10.0.2.2/10.0.2.2:8080
```
→ Connection refused (backend not running or wrong URL)

---

## Step 4: Check Backend Console

### What you should see:

**When test endpoint is called:**
```
=== TEST ENDPOINT CALLED ===
Backend is running and reachable!
```

**When route generation is called:**
```
=== ROUTE GENERATION REQUEST RECEIVED ===
Location: 48.8566, 2.3522
Activities: [RESTAURANT]
...
```

**If you see NOTHING:**
→ Request isn't reaching the backend (network/CORS issue)

---

## Common Issues and Solutions

### Issue 1: "Failed to connect to 10.0.2.2:8080"

**Causes:**
- Backend not running
- Wrong port
- Firewall blocking

**Solutions:**
1. Verify backend is running: `curl http://localhost:8080/api/routes/test`
2. Check port in `application.properties`: `server.port=8080`
3. Check firewall: Allow port 8080

### Issue 2: "Connection refused"

**Causes:**
- Backend crashed
- Database connection failed
- Port already in use

**Solutions:**
1. Check backend logs for errors
2. Verify database is running
3. Check if another process is using port 8080: `lsof -i :8080`

### Issue 3: "CORS error" or "Network error"

**Causes:**
- CORS not configured correctly
- Wrong URL in RetrofitClient

**Solutions:**
1. Check `WebConfig.java` - CORS should allow all origins
2. Verify URL in `RetrofitClient.kt`: `http://10.0.2.2:8080/api/`
3. Check backend logs for CORS errors

### Issue 4: Request sent but backend doesn't receive it

**Causes:**
- Wrong endpoint path
- Request body format issue
- Backend not listening on correct path

**Solutions:**
1. Check endpoint: Should be `/api/routes/generate` (not `/routes/generate`)
2. Verify `server.servlet.context-path=/api` in `application.properties`
3. Check request body format matches `RouteRequest` DTO

---

## Quick Test Checklist

Run through this checklist:

- [ ] Backend is running (`mvn spring-boot:run`)
- [ ] Backend test endpoint works: `curl http://localhost:8080/api/routes/test`
- [ ] Database is running (PostgreSQL)
- [ ] Android app can reach backend (test endpoint in app)
- [ ] At least one activity is selected
- [ ] Logcat shows request being sent
- [ ] Backend console shows request received

---

## Manual Test

### Test 1: Backend Health
```bash
curl http://localhost:8080/api/routes/test
```
**Expected:** `{"status":"ok","message":"Backend is running",...}`

### Test 2: Route Generation (Direct)
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
**Expected:** JSON array with 3 route objects

### Test 3: From Emulator
```bash
adb shell
curl http://10.0.2.2:8080/api/routes/test
```
**Expected:** Same as Test 1

---

## What I Added

1. **Test Endpoint** (`/api/routes/test`)
   - Simple GET endpoint to verify backend is reachable
   - Called automatically when loading screen opens

2. **Better Logging**
   - Android: Logs all network requests
   - Backend: Logs when requests are received

3. **Connection Test**
   - App tests backend connectivity before sending route request
   - Shows clear error if backend is unreachable

---

## Next Steps

1. **Start backend** (if not running)
2. **Test backend directly**: `curl http://localhost:8080/api/routes/test`
3. **Run Android app** and click "Generate"
4. **Check Logcat** - Look for "Backend test successful" or error
5. **Check backend console** - Look for "TEST ENDPOINT CALLED" or "ROUTE GENERATION REQUEST RECEIVED"

Share what you see in:
- Android Logcat (filter by "LoadingScreen" or "RoutesRepository")
- Backend console output

This will tell us exactly where the connection is failing!




