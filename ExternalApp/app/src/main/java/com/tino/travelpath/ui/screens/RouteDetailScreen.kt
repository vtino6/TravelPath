package com.tino.travelpath.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.activity.compose.BackHandler
import com.tino.travelpath.data.repository.RoutesRepository
import com.tino.travelpath.ui.components.MapMarker
import com.tino.travelpath.ui.components.OpenStreetMapView
import org.osmdroid.util.GeoPoint
import com.tino.travelpath.ui.viewmodels.RouteSelectionViewModel
import com.tino.travelpath.ui.viewmodels.RouteSelectionViewModelFactory
import kotlinx.coroutines.launch
import android.content.Context
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.tino.travelpath.ui.utils.ImageUtils

@Composable
fun RouteDetailScreen(
    navController: NavController,
    parcoursId: String
) {
    // Handle system back button
    BackHandler {
        navController.popBackStack()
    }
    
    val context = LocalContext.current
    val activity = context as? androidx.activity.ComponentActivity
    val localViewModelStoreOwner = androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner.current
    val viewModelStoreOwner = activity ?: localViewModelStoreOwner
    
    if (viewModelStoreOwner == null) {
        android.util.Log.e("RouteDetailScreen", "ERROR: No ViewModelStoreOwner available!")
        return
    }
    
    val viewModel: RouteSelectionViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        viewModelStoreOwner = viewModelStoreOwner,
        key = "route_selection",
        factory = RouteSelectionViewModelFactory(RoutesRepository())
    )
    
    val routes by viewModel.routes.collectAsState()
    var route by remember { mutableStateOf<com.tino.travelpath.data.api.dto.RouteResponse?>(null) }
    val coroutineScope = rememberCoroutineScope()
    
    LaunchedEffect(parcoursId, routes) {
        val foundRoute = routes.find { it.id == parcoursId }
        
        if (foundRoute != null) {
            android.util.Log.d("RouteDetailScreen", "Found route in ViewModel: ${foundRoute.name}")
            route = foundRoute
        } else {
            android.util.Log.d("RouteDetailScreen", "Route not in ViewModel, loading from backend...")
            coroutineScope.launch {
                try {
                    val loadedRoute = RoutesRepository().getRouteById(parcoursId)
                    route = loadedRoute
                    android.util.Log.d("RouteDetailScreen", "Loaded route from backend: ${loadedRoute?.name}")
                } catch (e: Exception) {
                    android.util.Log.e("RouteDetailScreen", "Error loading route from backend", e)
                }
            }
        }
    }
    
    // Update route when routes change (e.g., after toggling like)
    LaunchedEffect(routes) {
        val foundRoute = routes.find { it.id == parcoursId }
        if (foundRoute != null) {
            route = foundRoute
        }
    }
    
    if (route == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }
    
    val startingPointName by viewModel.startingPointName.collectAsState()
    val startingPoint by viewModel.startingPoint.collectAsState()
    
    // Calculate total distance - sum all distances between steps
    // Always calculate from coordinates to ensure accuracy, even if backend provides distanceFromPrevious
    val totalDistance = remember(route, startingPoint) {
        if (route!!.steps.isEmpty()) {
            0.0
        } else {
            var total = 0.0
            
            // Calculate distance from starting point to first step
            val currentStartingPoint = startingPoint
            if (currentStartingPoint != null) {
                val firstStep = route!!.steps[0]
                val firstPlace = firstStep.place
                total += calculateHaversineDistance(
                    currentStartingPoint.first, currentStartingPoint.second,
                    firstPlace.latitude, firstPlace.longitude
                )
            }
            
            // Add distances between subsequent steps
            for (i in 1 until route!!.steps.size) {
                val prevPlace = route!!.steps[i - 1].place
                val currentPlace = route!!.steps[i].place
                total += calculateHaversineDistance(
                    prevPlace.latitude, prevPlace.longitude,
                    currentPlace.latitude, currentPlace.longitude
                )
            }
            
            total
        }
    }
    
    // Build step items with starting point first
    val stepItems = mutableListOf<StepItem>()
    
    // Add starting point as first item (no distance shown)
    stepItems.add(
        StepItem(
            title = "Point de départ: ${startingPointName ?: "Localisation sélectionnée"}",
            info = "",
            imageUrl = "",
            category = "",
            isStartingPoint = true,
            travelTime = null,
            travelDistance = null
        )
    )
    
    // Add route steps with distance info (only distance, no time)
    route!!.steps.forEachIndexed { index, step ->
        val distance = step.distanceFromPrevious ?: 0.0
        
        // Only show distance if it's greater than 0
        // Distance is shown for all steps (distance from previous step or starting point)
        val travelInfo = if (distance > 0) {
            "${String.format("%.1f", distance)} km depuis ${if (index == 0) "le point de départ" else "l'étape précédente"}"
        } else {
            ""
        }
        
        stepItems.add(
            StepItem(
                title = "${step.order}. ${step.place.name}",
                info = travelInfo,
                imageUrl = ImageUtils.getImageUrlForCategory(step.place.category, step.place.id),
                category = step.place.category,
                isStartingPoint = false,
                travelTime = null,
                travelDistance = if (distance > 0) distance else null
            )
        )
    }
    
    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { navController.popBackStack() }) {
                    Text("← ${route!!.name}")
                }
                IconButton(onClick = { /* Menu */ }) {
                    Text("⋮", fontSize = 20.sp)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
        ) {
            // Map view using OpenStreetMap (FREE)
            val startingPoint by viewModel.startingPoint.collectAsState()
            
            // Build markers: starting point first (if available), then steps
            val markers = mutableListOf<MapMarker>()
            
            // Add starting point as first marker (distinct)
            startingPoint?.let { (lat, lng) ->
                markers.add(
                    MapMarker(
                        latitude = lat,
                        longitude = lng,
                        title = "Point de départ",
                        isStartingPoint = true
                    )
                )
            }
            
            // Add step markers
            route!!.steps.forEach { step ->
                markers.add(
                    MapMarker(
                        latitude = step.place.latitude,
                        longitude = step.place.longitude,
                        title = step.place.name,
                        isStartingPoint = false
                    )
                )
            }
            
            // Build polyline: starting point first (if available), then steps
            val routePolylinePoints = mutableListOf<GeoPoint>()
            startingPoint?.let { (lat, lng) ->
                routePolylinePoints.add(GeoPoint(lat, lng))
            }
            route!!.steps.forEach { step ->
                routePolylinePoints.add(
                    GeoPoint(step.place.latitude, step.place.longitude)
                )
            }
            
            // Center map on starting point if available, otherwise first step
            val centerLat = startingPoint?.first 
                ?: route!!.steps.firstOrNull()?.place?.latitude 
                ?: (route!!.steps.lastOrNull()?.place?.latitude ?: 0.0)
            val centerLng = startingPoint?.second 
                ?: route!!.steps.firstOrNull()?.place?.longitude 
                ?: (route!!.steps.lastOrNull()?.place?.longitude ?: 0.0)
            
            OpenStreetMapView(
                centerLatitude = centerLat,
                centerLongitude = centerLng,
                markers = markers,
                routePolyline = routePolylinePoints.takeIf { it.isNotEmpty() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Metrics
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(Icons.Default.AttachMoney, "%.0f€".format(route!!.totalBudget), modifier = Modifier.weight(1f))
                MetricCard(Icons.Default.Train, getTransportationModeLabel(route!!.transportationMode ?: "MIXED"), modifier = Modifier.weight(1f))
            }
            
            // Step list - always visible below map
            Text(
                text = "Étapes (${route!!.steps.size + 1})",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 15.dp)
            )
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(stepItems) { step ->
                    StepItemCard(step)
                }
                
                // Total distance at the bottom
                item {
                    Divider(
                        modifier = Modifier.padding(vertical = 10.dp),
                        color = Color(0xFFE0E0E0)
                    )
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF667eea))
                    ) {
                        Text(
                            text = "Distance totale: ${String.format("%.1f", totalDistance)} km",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(15.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(15.dp))
            
            Button(
                onClick = { 
                    coroutineScope.launch {
                        try {
                            android.util.Log.d("RouteDetailScreen", "Starting navigation: ${route!!.id}")
                            navController.navigate("active_navigation?routeId=${route!!.id}")
                        } catch (e: Exception) {
                            android.util.Log.e("RouteDetailScreen", "Error starting navigation", e)
                            navController.navigate("active_navigation?routeId=${route!!.id}")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF667eea)
                )
            ) {
                Text("COMMENCER NAVIGATION", modifier = Modifier.padding(vertical = 12.dp))
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 15.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IconButton(
                    onClick = { 
                        coroutineScope.launch {
                            viewModel.toggleLike(route!!, null)
                            // Update local route state to reflect the change immediately
                            route = route!!.copy(isFavorite = !(route!!.isFavorite ?: false))
                        }
                    },
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = if (route!!.isFavorite == true) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (route!!.isFavorite == true) "Retirer des favoris" else "Ajouter aux favoris",
                        modifier = Modifier.size(28.dp),
                        tint = if (route!!.isFavorite == true) Color(0xFFE91E63) else Color.Gray
                    )
                }
                OutlinedButton(
                    onClick = { 
                        route?.let { shareRoute(context, it) }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Partager")
                }
            }
        }
    }
}

@Composable
fun TabButton(text: String, selected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) Color(0xFF667eea) else Color(0xFFF5F5F5)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = if (selected) Color.White else Color.Black
            )
        }
    }
}

@Composable
fun MetricCard(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = Color(0xFF667eea)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

data class StepItem(
    val title: String, 
    val info: String, 
    val imageUrl: String, 
    val category: String,
    val isStartingPoint: Boolean = false,
    val travelTime: Int? = null,
    val travelDistance: Double? = null
)

@Composable
fun StepItemCard(step: StepItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (step.isStartingPoint) Color(0xFFE8F5E9) else Color(0xFFF5F5F5)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp),
            horizontalArrangement = Arrangement.spacedBy(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Show pin emoji for starting point, or step number for others
            if (step.isStartingPoint) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFF4CAF50), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Point de départ",
                        modifier = Modifier.size(24.dp),
                        tint = Color.White
                    )
                }
            } else {
                // Show step number in a circle
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFF667eea), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = step.title.substringBefore(".").takeIf { it.matches(Regex("\\d+")) } ?: "",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = step.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                // Display category if not starting point and category is not empty
                if (!step.isStartingPoint && step.category.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = getCategoryLabel(step.category),
                        fontSize = 12.sp,
                        color = Color(0xFF9E9E9E),
                        fontWeight = FontWeight.Normal
                    )
                }
                if (step.info.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = step.info,
                        fontSize = 14.sp,
                        color = Color(0xFF667eea),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * Calculate distance between two coordinates using Haversine formula
 */
private fun calculateHaversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val earthRadius = 6371.0 // km
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2)
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    return earthRadius * c
}

/**
 * Get human-readable label for category
 */
private fun getCategoryLabel(category: String): String {
    return when (category.uppercase()) {
        "RESTAURANT" -> "Restaurant"
        "CULTURE" -> "Culture"
        "LEISURE" -> "Loisir"
        "DISCOVERY" -> "Découverte"
        else -> category
    }
}

/**
 * Get human-readable label for transportation mode
 */
private fun getTransportationModeLabel(mode: String): String {
    return when (mode) {
        "WALKING" -> "Marche"
        "BICYCLE" -> "Vélo"
        "PUBLIC_TRANSPORT" -> "Transport"
        "CAR" -> "Voiture"
        "MIXED" -> "Mixte"
        else -> mode
    }
}

/**
 * Share route information via Android share intent
 */
fun shareRoute(context: Context, route: com.tino.travelpath.data.api.dto.RouteResponse) {
    val routeTypeName = when (route.routeType) {
        "ECONOMIC" -> "Économique"
        "BALANCED" -> "Équilibré"
        "COMFORT" -> "Confort"
        else -> route.name
    }
    
    val transportationLabel = getTransportationModeLabel(route.transportationMode ?: "MIXED")
    
    val budgetFormatted = "%.0f€".format(route.totalBudget)
    val shareText = buildString {
        appendLine("Parcours: $routeTypeName")
        appendLine("Budget: $budgetFormatted")
        appendLine("Transport: $transportationLabel")
        appendLine("${route.steps.size} étapes")
        appendLine()
        appendLine("Étapes:")
        route.steps.forEachIndexed { index, step ->
            appendLine("${index + 1}. ${step.place.name}")
            if (step.place.address != null) {
                appendLine("   ${step.place.address}")
            }
        }
        appendLine()
        appendLine("Généré avec TravelPath")
    }
    
    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, shareText)
        type = "text/plain"
    }
    
    context.startActivity(Intent.createChooser(shareIntent, "Partager le parcours"))
}
