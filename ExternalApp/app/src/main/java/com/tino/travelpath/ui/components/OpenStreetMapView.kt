package com.tino.travelpath.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

/**
 * OpenStreetMap view using osmdroid (FREE, no API key needed)
 */
@Composable
fun OpenStreetMapView(
    centerLatitude: Double,
    centerLongitude: Double,
    markers: List<MapMarker> = emptyList(),
    routePolyline: List<GeoPoint>? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Initialize osmdroid configuration
    DisposableEffect(Unit) {
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = "TravelPath/1.0"
        
        onDispose { }
    }
    
    AndroidView(
        factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK) // Free OpenStreetMap tiles
                setMultiTouchControls(true)
                
                // Set initial center
                controller.setCenter(GeoPoint(centerLatitude, centerLongitude))
                controller.setZoom(15.0)
                
                // Add markers
                markers.forEach { marker ->
                    val mapMarker = Marker(this)
                    mapMarker.position = GeoPoint(marker.latitude, marker.longitude)
                    mapMarker.title = marker.title
                    mapMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    
                    // Style starting point marker with 📍 pin emoji
                    if (marker.isStartingPoint) {
                        val pinBitmap = android.graphics.Bitmap.createBitmap(64, 80, android.graphics.Bitmap.Config.ARGB_8888)
                        val canvas = android.graphics.Canvas(pinBitmap)
                        
                        // Draw pin shape (red/pink pin with shadow)
                        val pinPaint = android.graphics.Paint().apply {
                            color = android.graphics.Color.parseColor("#FF5252") // Red pin color
                            style = android.graphics.Paint.Style.FILL
                            isAntiAlias = true
                        }
                        
                        // Draw pin shadow
                        val shadowPaint = android.graphics.Paint().apply {
                            color = android.graphics.Color.parseColor("#80000000") // Semi-transparent black
                            style = android.graphics.Paint.Style.FILL
                            isAntiAlias = true
                        }
                        canvas.drawCircle(32f, 32f, 20f, shadowPaint)
                        
                        // Draw pin circle
                        canvas.drawCircle(32f, 30f, 18f, pinPaint)
                        
                        // Draw pin point (triangle)
                        val path = android.graphics.Path()
                        path.moveTo(32f, 48f)
                        path.lineTo(20f, 70f)
                        path.lineTo(44f, 70f)
                        path.close()
                        canvas.drawPath(path, pinPaint)
                        
                        // Draw 📍 emoji on top
                        val textPaint = android.graphics.Paint().apply {
                            textSize = 28f
                            textAlign = android.graphics.Paint.Align.CENTER
                            isAntiAlias = true
                        }
                        canvas.drawText("📍", 32f, 38f, textPaint)
                        
                        mapMarker.icon = android.graphics.drawable.BitmapDrawable(ctx.resources, pinBitmap)
                        mapMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    }
                    
                    overlays.add(mapMarker)
                }
                
                // Add route polyline if provided
                routePolyline?.let { points ->
                    val polyline = Polyline()
                    polyline.setPoints(points)
                    polyline.color = android.graphics.Color.parseColor("#667eea")
                    polyline.width = 10f
                    overlays.add(polyline)
                }
            }
        },
        modifier = modifier.fillMaxSize()
    )
}

data class MapMarker(
    val latitude: Double,
    val longitude: Double,
    val title: String,
    val isStartingPoint: Boolean = false
)





