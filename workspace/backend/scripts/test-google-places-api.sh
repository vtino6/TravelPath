#!/bin/bash

# Script to test Google Places API configuration
# Usage: ./test-google-places-api.sh YOUR_API_KEY

if [ -z "$1" ]; then
    echo "Usage: $0 YOUR_GOOGLE_PLACES_API_KEY"
    echo ""
    echo "Or set environment variable:"
    echo "  export GOOGLE_PLACES_API_KEY=your-key"
    echo "  $0"
    exit 1
fi

API_KEY="${1:-$GOOGLE_PLACES_API_KEY}"

if [ -z "$API_KEY" ]; then
    echo "Error: No API key provided"
    exit 1
fi

echo "Testing Google Places API..."
echo "API Key: ${API_KEY:0:10}..." # Show only first 10 chars for security

# Test 1: Check if Places API (New) is enabled
echo ""
echo "Test 1: Checking if Places API (New) is enabled..."
RESPONSE=$(curl -s -w "\n%{http_code}" \
    -X POST \
    -H "Content-Type: application/json" \
    -H "X-Goog-Api-Key: $API_KEY" \
    -H "X-Goog-FieldMask: places.id,places.displayName" \
    -d '{
        "textQuery": "Eiffel Tower",
        "locationBias": {
            "circle": {
                "center": {
                    "latitude": 48.8584,
                    "longitude": 2.2945
                },
                "radius": 1000.0
            }
        }
    }' \
    "https://places.googleapis.com/v1/places:searchText")

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

if [ "$HTTP_CODE" = "200" ]; then
    echo "✅ Places API (New) is enabled and working!"
    echo ""
    echo "Response preview:"
    echo "$BODY" | head -n 20
    echo ""
    
    # Try to extract a photo reference
    PHOTO_NAME=$(echo "$BODY" | grep -o '"name":"places/[^"]*photos/[^"]*"' | head -n1 | cut -d'"' -f4)
    
    if [ -n "$PHOTO_NAME" ]; then
        echo "✅ Found photo reference: $PHOTO_NAME"
        echo ""
        echo "Test 2: Testing photo URL generation..."
        PHOTO_URL="https://places.googleapis.com/v1/$PHOTO_NAME/media?key=$API_KEY&maxHeightPx=800&maxWidthPx=800"
        echo "Photo URL: $PHOTO_URL"
        
        PHOTO_RESPONSE=$(curl -s -w "\n%{http_code}" -I "$PHOTO_URL")
        PHOTO_HTTP_CODE=$(echo "$PHOTO_RESPONSE" | tail -n1)
        
        if [ "$PHOTO_HTTP_CODE" = "200" ] || [ "$PHOTO_HTTP_CODE" = "302" ]; then
            echo "✅ Photo URL is accessible!"
        else
            echo "⚠️  Photo URL returned HTTP $PHOTO_HTTP_CODE"
        fi
    else
        echo "⚠️  No photo found in response"
    fi
elif [ "$HTTP_CODE" = "403" ]; then
    echo "❌ Error: API key is invalid or Places API (New) is not enabled"
    echo ""
    echo "Please check:"
    echo "  1. The API key is correct"
    echo "  2. Places API (New) is enabled in Google Cloud Console"
    echo "  3. The API key has access to Places API (New)"
elif [ "$HTTP_CODE" = "400" ]; then
    echo "❌ Error: Bad request - check the API key format"
    echo "Response: $BODY"
else
    echo "❌ Error: HTTP $HTTP_CODE"
    echo "Response: $BODY"
fi

echo ""
echo "Configuration instructions:"
echo "  1. Add to application.properties:"
echo "     google.places.api.key=$API_KEY"
echo ""
echo "  2. Or set environment variable:"
echo "     export GOOGLE_PLACES_API_KEY=$API_KEY"
echo ""
echo "  3. Restart the backend application"




